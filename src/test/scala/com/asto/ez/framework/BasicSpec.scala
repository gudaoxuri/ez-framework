package com.asto.ez.framework

import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.scalatest.{BeforeAndAfter, FunSuite}

import scala.io.Source

abstract class BasicSpec extends FunSuite with BeforeAndAfter with LazyLogging {

  protected def before2(): Any = {

  }

  protected def after2(): Any = {

  }

  before {
    EZGlobal.vertx = Vertx.vertx()
    EZGlobal.config = new JsonObject(Source.fromFile(this.getClass.getResource("/").getPath + "config.json").mkString)
    before2()
  }

  after {
    after2()
  }

}

