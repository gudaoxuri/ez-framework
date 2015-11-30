package com.asto.ez.framework.helper

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

object TimeHelper {

  val msf = new SimpleDateFormat("yyyyMMddHHmmssSSS")
  val sf = new SimpleDateFormat("yyyyMMddHHmmss")
  val mf = new SimpleDateFormat("yyyyMMddHHmm")
  val hf = new SimpleDateFormat("yyyyMMddHH")
  val df = new SimpleDateFormat("yyyyMMdd")

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

}
