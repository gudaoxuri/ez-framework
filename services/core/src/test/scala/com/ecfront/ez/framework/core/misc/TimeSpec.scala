package com.ecfront.ez.framework.core.misc

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import com.ecfront.ez.framework.core.BasicSpec

class TimeSpec extends BasicSpec {

  test("ZeroTimeOffset Test") {

    val dfd = new SimpleDateFormat("yyyyMMdd")

    def getZeroTimeOffset = {
      val currentTime = new Date()
      val currentDay = dfd.parse(dfd.format(currentTime))
      val calendar = Calendar.getInstance()
      calendar.setTime(currentDay)
      calendar.add(Calendar.DATE, 1)
      calendar.getTime.getTime - currentTime.getTime
    }

    println(getZeroTimeOffset)
    Thread.sleep(10000)
    println(getZeroTimeOffset)
    val calendar = Calendar.getInstance()
    calendar.setTimeInMillis(new Date().getTime + getZeroTimeOffset)
    println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(calendar.getTime))

  }

}


