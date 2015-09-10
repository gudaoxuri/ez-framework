package com.ecfront.ez.framework.module.core


object CommonUtils {

  val TOKEN_FLAG = "_token_"

  val ORDER_FIELD_FLAG = "orderField"
  val ORDER_SORT_FLAG = "orderSort"

  val STATUS_FLAG = "enable"

  def packageSql(parameter: Map[String, String]): (String, List[Any]) = {
    val sql =
      if (parameter.contains(STATUS_FLAG))
        //防SQL注入
        s" $STATUS_FLAG = ${parameter(STATUS_FLAG).toBoolean.toString} "
      else ""
    val orderSql = if (parameter.contains(ORDER_FIELD_FLAG)) {
      s"  ORDER BY  ? " + (if (parameter.contains(ORDER_FIELD_FLAG)) s" ${parameter(ORDER_SORT_FLAG)} " else " DESC ")
    } else ""
    (sql + orderSql, List[Any](parameter(ORDER_FIELD_FLAG)))
  }

}
