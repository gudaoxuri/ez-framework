package com.ecfront.ez.framework.service.jdbc

import scala.annotation.StaticAnnotation

/**
  * 实体注解，要持久化的对象必须应用此注解
  *
  * @param desc 表注释
  */
case class Entity(desc: String = "") extends StaticAnnotation

/**
  * 唯一性
  */
@scala.annotation.meta.field
case class Unique() extends StaticAnnotation

/**
  * 必需项
  */
@scala.annotation.meta.field
case class Require() extends StaticAnnotation

/**
  * 索引
  */
@scala.annotation.meta.field
case class Index() extends StaticAnnotation

/**
  * 字段描述
  *
  * @param label 描述
  */
@scala.annotation.meta.field
case class Label(label: String) extends StaticAnnotation

/**
  * 插入时使用当前时间函数
  *
  */
@scala.annotation.meta.field
case class NowBySave() extends StaticAnnotation

/**
  * 更新时使用当前时间函数
  *
  */
@scala.annotation.meta.field
case class NowByUpdate() extends StaticAnnotation

@scala.annotation.meta.field
case class UUID() extends StaticAnnotation

/**
  * 主键策略，默认是uuid，可选 seq
  *
  * @param strategy 主键策略
  */
@scala.annotation.meta.field
case class Id(strategy: String = "uuid") extends StaticAnnotation

object Id {
  val STRATEGY_SEQ = "seq"
  val STRATEGY_UUID = "uuid"
}
