package com.mathandcs.kino.abacus.scala_runtime.inference

import com.mathandcs.kino.abacus.common.Field

case class Table(var name: String, var schema: List[Field])

case class InferRequest(
                         var inputTables: List[Table],
                         var sqlText: String
                       )
