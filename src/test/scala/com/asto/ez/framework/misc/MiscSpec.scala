package com.asto.ez.framework.misc

import java.util.concurrent.CountDownLatch

import org.scalatest.FunSuite

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.matching.Regex

/**
 * 杂项测试
 */
class MiscSpec extends FunSuite {

  test("Callback Test") {
    val counter = new CountDownLatch(1)

    def plus(i: Int, j: Int, callback: => Int => Unit): Unit = {
      new Thread(new Runnable {
        override def run(): Unit = {
          //process
          callback(i + j)
        }
      }).start()
    }

    plus(1, 3, {
      result =>
        println(result)
        counter.countDown()
    })

    counter.await()
  }

  test("Promise Test") {
    val counter = new CountDownLatch(1)

    def plus(i: Int, j: Int): Future[Int] = {
      val p = Promise[Int]
      new Thread(new Runnable {
        override def run(): Unit = {
          //process
          p.success(1 + 2)
        }
      }).start()
      p.future
    }

    val result: Future[Int] = plus(1, 3)
    result.onSuccess {
      case res =>
        println(res)
        counter.countDown()
    }

    counter.await()
  }

  test("m1 Test") {

    val a = Set(("a", 0), ("b", 20), ("c", 10))
    val b = Set(("a", 2), ("b", 0))
    a.map(f => f._1 -> (if (f._2 == 0) 0 else b.find(_._1 == f._1).getOrElse((f._1, 0))._2 / f._2))

    Set(("b", 2), ("b", 20), ("a", 10), ("b", 1), ("c", 30)).groupBy(_._1).map(f => f._1 -> f._2.map(_._2).reduce(_ / _))

    val rSourceIdMatch = new Regex( """/source/(\S+)/""")

    "/source/dd/" match {
      case rSourceIdMatch(id) =>
        println(id)
    }

    "/source/111/" match {
      case rSourceIdMatch(id) =>
        println(id)
    }
    "/source/dd/" match {
      case rSourceIdMatch(id) =>
        println(id)
    }

    "/source/dd-sfe-343/" match {
      case rSourceIdMatch(id) =>
        println(id)
    }
  }

}

/*test ("TypeTag Test") {

def testTypeTag[T: TypeTag] (a: T) = {
println (typeTag[T].tpe)
println (typeTag[T].tpe.getClass)
val a = JsonHelper.toObject ("""{"a":1}""", typeTag[T].tpe.getClass)
println (a)
}
testTypeTag (TypeTagVO (1) )
}
}

case class TypeTagVO(a: Int)*/
