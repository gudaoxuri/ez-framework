package com.ecfront.ez.framework.module.core


object CommonUtils {

  val TOKEN = "__token__"

  val ORDER_FIELD = "orderField"
  val ORDER_SORT = "orderSort"
  val CONDITION = "condition"

  def packageOrder(parameter: Map[String, String]): (String, List[Any]) = {
    val orderSql = if (parameter.contains(ORDER_FIELD)) {
      s"  ORDER BY  ? " + (if (parameter.contains(ORDER_SORT)) s" ${parameter(ORDER_SORT)} " else " DESC ")
    } else ""
    (orderSql, List[Any](parameter(ORDER_FIELD)))
  }

}
