package com.ecfront.ez.framework.service.masterslave

import java.util.concurrent.CountDownLatch

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.test.MockStartupSpec

class MasterSlaveSpec extends MockStartupSpec {

  test("Master Slave test") {

    val counter = new CountDownLatch(1)

    Assigner.Worker.register(List(MockWorkProcessor))

    Assigner.Master.register({
      finishDTO =>
        assert(finishDTO.instanceId != "")
        assert(finishDTO.isSuccess)
        assert(finishDTO.hasChange)
        assert(finishDTO.message == "")
        assert(finishDTO.instanceParameters == Map("inst1" -> "3", "newField" -> "haha..."))
        assert(finishDTO.isSuccess)
        println("finish -> " + finishDTO)
       // counter.countDown()
    }, {
      startDTO =>
        println("start -> " + startDTO)
      //  counter.countDown()
    })

    val threads=for(i <- 0 to 1000) yield
    new Thread(new Runnable {
      override def run(): Unit = {
        Assigner.Master.prepareTask(TaskPrepareDTO(
          instanceId = i+"",
          worker = "testModule",
          category = "m",
          taskInfo = Map("source" -> "jdbc://xxxx"),
          taskVar = Map("var1" -> i),
          instanceParameters = Map("inst1" -> "3")
        ))
      }
    })
    threads.foreach(_.start())

    counter.await()
  }

}

object MockWorkProcessor extends TaskBaseProcessor[MockTask] {

  override val category: String = "m"

  override protected def process(
                                  task: MockTask, taskVar: Map[String, Any],
                                  instanceParameters: Map[String, Any]): Resp[(Map[String, Any], Map[String, Any])] = {
    Thread.sleep(5000)
    assert(task.source == "jdbc://xxxx")
    println(">>>>>"+taskVar("var1"))
    Resp.success(taskVar, instanceParameters + ("newField" -> "haha..."))
  }
}

case class MockTask(source: String)


