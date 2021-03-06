package com.mathandcs.kino.abacus.scala_runtime.app

import com.mathandcs.kino.abacus.config.AppConfig
import com.mathandcs.kino.abacus.scala_runtime.inference.{InferRequest, InferResponse}
import com.mathandcs.kino.abacus.scala_runtime.io.{DataReader, DataWriter}
import com.mathandcs.kino.abacus.scala_runtime.utils.SparkUtils
import org.apache.spark.Logging
import org.apache.spark.sql.{DataFrame, RowFactory}
import org.json4s.DefaultFormats

/**
  * Created by dash wang on 2016-08-16.
  */
class HiveSQLExecutor extends BaseApp {

  private implicit val formats = DefaultFormats

  private var sqlStatement: String = null

  override def run(appConfig: AppConfig): Unit = {

    sqlStatement = appConfig.extra.get("sqlStatement").toString

    val sqlContext = SparkUtils.switchToHiveContext().sqlContext

    for (i <- appConfig.inputTables.indices) {
      try {
        val data = DataReader.loadToDataFrame(appConfig.inputTables(0), null)
        val tableName = appConfig.extra.get("tableNames").asInstanceOf[java.util.List[String]].get(i)
        data.registerTempTable(tableName)
      } catch {
        case ex: Exception => throw new RuntimeException(s"fail to register ${i}th table", ex)
      }
    }

    log.info("registered tables: " + sqlContext.tableNames().mkString(","))

    log.info(sqlStatement)
    var outputDF: DataFrame = null
    val queries = sqlStatement.split(";")
    for (query <- queries) {
      // new line mark should be trimmed
      val trimmedSql: String = query.trim
      if (trimmedSql.length > 0) {
        log.info(s"Executing trimmed sql statement: [$trimmedSql]")
        outputDF = sqlContext.sql(trimmedSql)
      }
      else {
        log.info(s"Skipping empty sql statement, before trimming sql is: [$query]")
      }
    }

    DataWriter.save(outputDF, appConfig.outputTables(0).url, appConfig.outputTables(0).format)
  }
}

object HiveSQLExecutor extends Logging {

  def inferSchema(inferRequest: InferRequest): InferResponse = {
    // Create empty dataframe with specified schema
    for (table <- inferRequest.inputTables) {
      val rowRDD = SparkUtils.sparkContext.parallelize(List(RowFactory.create(AnyRef)))
      val structType = DataReader.transferScalaSchemaToSparkSqlSchema(table.schema)
      val df = SparkUtils.switchToHiveContext().sqlContext.createDataFrame(rowRDD, structType)
      df.registerTempTable(table.name)
    }

    var inferResponse: InferResponse = null
    val statements = inferRequest.sqlText.split(";")
    try {
      for (statement <- statements) {
        val trimmedSql = statement.trim
        if (trimmedSql.length > 0) {
          val outputDF = SparkUtils.switchToHiveContext().sqlContext.sql(trimmedSql)
          val outputSchema = DataReader.transferSparkSqlSchemaToScalaSchema(outputDF.schema)
          val errors = List.empty[Error]
          inferResponse = new InferResponse(outputSchema, errors)
        } else {
          log.info("Skipping empty sql statement, before trimming sql is: [{}]", statement)
        }
      }
    } catch {
      case ex: Exception => throw new RuntimeException(s"fail to execute sql ${inferRequest.sqlText}")
    } finally {
      // remove temp tables
      for (table <- inferRequest.inputTables) {
        SparkUtils.switchToHiveContext().sqlContext.dropTempTable(table.name)
      }
    }

    return inferResponse
  }
}