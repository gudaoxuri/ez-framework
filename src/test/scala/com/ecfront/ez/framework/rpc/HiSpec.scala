package com.ecfront.ez.framework.rpc

import org.scalatest.FunSuite

class HiSpec extends FunSuite {

  test("Hi") {
    "Happy,Sad,Laugh,Cry,Hopeful,Disappointed,Successful,Failed"
      .split(',')
      .zipWithIndex
      .filter(_._2 % 2 == 0)
      .map(_._1)
      .foreach(println)

  }
}




