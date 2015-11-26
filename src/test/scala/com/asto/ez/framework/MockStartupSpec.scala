package com.asto.ez.framework

abstract class MockStartupSpec extends BasicSpec {

  val startup = new MockStartup()

  override protected def before2(): Any = {
    super.before2()
    startup.start()
  }

  override protected def after2(): Any = {
    super.after2()
    startup.stop()
  }
}

