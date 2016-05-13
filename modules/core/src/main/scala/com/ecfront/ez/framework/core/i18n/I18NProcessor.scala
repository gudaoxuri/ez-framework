package com.ecfront.ez.framework.core.i18n

import java.io.File
import java.util.regex.Pattern

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZContext
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.io.Source

/**
  * I18N处理器
  */
object I18NProcessor extends LazyLogging {

  // 正则信息 -> 语言 -> 翻译后的信息
  private val i18nInfo = collection.mutable.Map[Pattern, Map[String, String]]()

  def init(): Unit = {
    logger.info("Init i18n processor")
    load()
    if (i18nInfo.nonEmpty) {
      Resp.customInit = process
    } else {
      logger.info("i18n function disabled")
    }
  }

  /**
    * 加载i18n文件
    */
  def load(): Unit = {
    val i18nPath = new File(EZContext.confPath + "i18n/")
    if (i18nPath.exists()) {
      i18nPath.listFiles().foreach {
        file =>
          val lines = Source.fromFile(file, "UTF-8").getLines().toList
          val head = lines.head
          val languages = head.split('\t').toList.tail.zipWithIndex
          lines.tail.filter(l => !l.startsWith("#") && l.trim.nonEmpty).foreach {
            line =>
              val column = line.split('\t')
              i18nInfo += column(0).r.pattern -> languages.map {
                lang =>
                  lang._1 -> (if (column.length > lang._2 + 1) column(lang._2 + 1) else column(1))
              }.toMap
          }
      }
    }
  }

  /**
    * 设置语言
    *
    * @param _language 语言编码
    */
  def setLanguage(_language: String): Unit = {
    EZContext.language = _language
  }

  private val tabR = "\t"

  def process(resp: Resp[_]): Unit = {
    if (resp.message != null && resp.message.nonEmpty) {
      resp.message = i18n(resp.message.replaceAll(tabR, " "))
    }
  }

  def i18n(str: String): String = {
    var newStr = str
    i18nInfo.find(_._1.matcher(str).matches()).foreach {
      matchedItem =>
        val matcher = matchedItem._1.matcher(str)
        newStr = matcher.replaceAll(matchedItem._2(EZContext.language))
    }
    newStr
  }

  implicit class Impl(val str: String) {
    def x: String = i18n(str)
  }

}
