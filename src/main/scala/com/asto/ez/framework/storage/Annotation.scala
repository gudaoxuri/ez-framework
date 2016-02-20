package com.asto.ez.framework.storage

import com.ecfront.common.Ignore

import scala.annotation.StaticAnnotation

case class Entity(desc: String="") extends StaticAnnotation

@scala.annotation.meta.field
case class Index() extends StaticAnnotation

@scala.annotation.meta.field
case class Unique() extends StaticAnnotation

@scala.annotation.meta.field
case class Require() extends StaticAnnotation

@scala.annotation.meta.field
case class Label(label:String) extends StaticAnnotation

@scala.annotation.meta.field
case class Desc(desc: String) extends StaticAnnotation

@scala.annotation.meta.field
case class Id(strategy:String="uuid") extends StaticAnnotation

object Id{
  val STRATEGY_SEQ="seq"
  val STRATEGY_UUID="uuid"
}

@scala.annotation.meta.field
case class Text() extends StaticAnnotation

@scala.annotation.meta.field
case class Length(length:Int) extends StaticAnnotation

@scala.annotation.meta.field
case class FK() extends StaticAnnotation

@scala.annotation.meta.field
case class ManyToMany(mapping: String, master: Boolean, fetch: Boolean) extends Ignore

@scala.annotation.meta.field
case class OneToMany(mapping: String, relField: String, fetch: Boolean) extends Ignore


