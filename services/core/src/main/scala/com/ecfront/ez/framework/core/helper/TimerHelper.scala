package com.ecfront.ez.framework.core.helper

import java.lang.Long

import com.ecfront.ez.framework.core.EZ
import io.vertx.core.Handler

object TimerHelper {

  def periodic(delay: Long, fun: => Unit): Unit = {
    EZ.vertx.setPeriodic(delay, new Handler[Long] {
      override def handle(event: Long): Unit = {
        fun
      }
    })
  }

  def timer(delay: Long, fun: => Unit): Unit = {
    EZ.vertx.setTimer(delay, new Handler[Long] {
      override def handle(event: Long): Unit = {
        fun
      }
    })
  }

}
