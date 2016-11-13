package com.ecfront.ez.framework.core.helper

import java.lang.Long
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

object TimerHelper {

  private val ex = new ScheduledThreadPoolExecutor(1)

  def periodic(initialDelay: Long,
               period: Long, fun: => Unit): Unit = {
    val task = new Runnable {
      def run() = fun
    }
    ex.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.SECONDS)
  }

  def periodic(period: Long, fun: => Unit): Unit = {
    periodic(0L, period, fun)
  }

  def timer(delay: Long, fun: => Unit): Unit = {
    val task = new Runnable {
      def run() = fun
    }
    ex.schedule(task, delay, TimeUnit.SECONDS)
  }

}
