package com.ecfront.ez.framework.core.helper

import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.{Calendar, Date, TimeZone}

/**
  * 时间辅助类
  */
object TimeHelper {

  val msf = new SimpleDateFormat("yyyyMMddHHmmssSSS")
  val sf = new SimpleDateFormat("yyyyMMddHHmmss")
  val mf = new SimpleDateFormat("yyyyMMddHHmm")
  val hf = new SimpleDateFormat("yyyyMMddHH")
  val df = new SimpleDateFormat("yyyyMMdd")
  val Mf = new SimpleDateFormat("yyyyMM")
  val yf = new SimpleDateFormat("yyyy")

  val yyyy_MM_dd_HH_mm_ss_SSS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")

  def dateOffset(offsetValue: Int, offsetUnit: Int, currentTime: Long): Long = {
    val format = currentTime.toString.length match {
      case 8 => df
      case 10 => hf
      case 12 => mf
      case 14 => sf
      case 17 => msf
    }
    val calendar = Calendar.getInstance()
    calendar.setTime(format.parse(currentTime + ""))
    calendar.add(offsetUnit, offsetValue)
    format.format(calendar.getTime).toLong
  }

  def dateOffset(offsetValue: Int, offsetUnit: Int, currentDate: Date): Date = {
    val calendar = Calendar.getInstance()
    calendar.setTime(currentDate)
    calendar.add(offsetUnit, offsetValue)
    calendar.getTime
  }

  def utc2Local(utcTime: String, localTimePatten: String = "yyyy-MM-dd'T'HH:mm:ss"): String = {
    val utcDate = Date.from(ZonedDateTime.parse(utcTime).toInstant)
    val localF = new SimpleDateFormat(localTimePatten)
    localF.setTimeZone(TimeZone.getDefault)
    localF.format(utcDate.getTime)
  }

}
