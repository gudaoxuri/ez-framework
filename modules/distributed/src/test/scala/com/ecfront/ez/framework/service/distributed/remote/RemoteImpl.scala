package com.ecfront.ez.framework.service.distributed.remote

class RemoteImpl1 extends RemoteInter {

  override def test(s: String): String = {
    println(">>" + s)
    s + s
  }

}

object RemoteImpl2 extends RemoteInter {

  override def test(s: String): String = {
    println(">>" + s)
    s + s
  }

}