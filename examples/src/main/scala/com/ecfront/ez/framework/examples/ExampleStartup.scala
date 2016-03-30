package com.ecfront.ez.framework.examples

import com.ecfront.ez.framework.core.EZManager


trait ExampleStartup extends App {

  EZManager.start()

  protected def start(): Unit

  start()

}
