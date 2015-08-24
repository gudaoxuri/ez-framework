package com.ecfront.ez.framework.module.core


object CommonUtils {

  def packageOrder(parameter: Map[String, String]): (String, List[Any]) = {
    val orderSql = if (parameter.contains(EZReq.ORDER_FIELD)) {
      s"  ORDER BY  ? " + (if (parameter.contains(EZReq.ORDER_SORT)) s" ${parameter(EZReq.ORDER_SORT)} " else " DESC ")
    } else ""
    (orderSql, List[Any](parameter(EZReq.ORDER_FIELD)))
  }

}
