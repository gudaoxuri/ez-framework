package com.ecfront.ez.framework.test

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.scalatest.{BeforeAndAfter, FunSuite}

/**
  * 基础测试类
  */
trait BasicSpec extends FunSuite with BeforeAndAfter with LazyLogging

