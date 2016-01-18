package com.asto.ez.framework

import com.asto.ez.framework.auth.{EZ_Account, EZ_Organization, EZ_Resource, EZ_Role}
import com.asto.ez.framework.rpc.Method
import com.asto.ez.framework.storage.BaseModel
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

trait Initiator extends LazyLogging {

 def needinitialization:Boolean

  def initialize():Unit

}
