package com.ecfront.ez.framework.core.i18n

import java.io.File
import java.util.regex.Pattern

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZContext

import scala.io.Source

/**
  * I18N处理器
  */
object I18NProcessor {

  // 正则信息 -> 语言 -> 翻译后的信息
  private val i18nInfo = collection.mutable.Map[Pattern, Map[String, String]]()

  def init(): Unit = {
    load()
    if (i18nInfo.nonEmpty) {
      Resp.customInit = process
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
          val lines = Source.fromFile(file).getLines().toList
          val head = lines.head
          val languages = head.split('\t').toList.tail.zipWithIndex
          lines.tail.filter(!_.startsWith("#")).foreach {
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
      resp.message = resp.message.replaceAll(tabR, " ")
      i18nInfo.find(_._1.matcher(resp.message).matches()).foreach {
        matchedItem =>
          val matcher = matchedItem._1.matcher(resp.message)
          resp.message = matcher.replaceAll(matchedItem._2(EZContext.language))
      }
    }
  }

}
