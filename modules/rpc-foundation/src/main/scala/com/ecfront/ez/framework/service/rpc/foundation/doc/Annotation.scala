package com.ecfront.ez.framework.service.rpc.foundation.doc

import scala.annotation.StaticAnnotation

case class API(category: String,header:String) extends StaticAnnotation

case class Desc(name: String,header:String) extends StaticAnnotation
