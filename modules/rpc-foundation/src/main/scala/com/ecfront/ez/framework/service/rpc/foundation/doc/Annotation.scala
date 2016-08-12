package com.ecfront.ez.framework.service.rpc.foundation.doc

import scala.annotation.StaticAnnotation

case class APIHeader(category: String,desc:String) extends StaticAnnotation

case class APIItem(name: String,desc:String,example:String) extends StaticAnnotation

/**
  * 必需项
  */
@scala.annotation.meta.field
case class APIRequire() extends StaticAnnotation

/**
  * 字段描述
  *
  * @param label 描述
  */
@scala.annotation.meta.field
case class APILabel(label: String) extends StaticAnnotation
