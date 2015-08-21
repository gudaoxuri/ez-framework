package com.ecfront.ez.framework.module.schedule

import com.typesafe.scalalogging.slf4j.LazyLogging

trait EZTask extends LazyLogging{

  def execute(parameters:Map[String,Any])

}
