package com.ecfront.ez.framework.examples.kafka

import com.ecfront.common.Resp
import com.ecfront.ez.framework.examples.ExampleStartup
import com.ecfront.ez.framework.service.kafka.KafkaProcessor


object KafkaExample extends ExampleStartup {

  override protected def start(): Unit = {

    // 定义第一个消费者，属于group1
    val consumer1 = KafkaProcessor.Consumer("topic1", "group1")
    consumer1.receive({
      (message,messageId) =>
        // 收到消息
        println("consumer1 -> " + message)
        // 关闭此消费者，这时第二个消费者可以收到消息
        consumer1.close()
        Resp.success(null)
    })
    // 定义第二个消费者，也属于group1，第一个与第二消费者是竞争关系，同一消息只会被其中一个消费
    KafkaProcessor.Consumer("topic1", "group1").receive({
      (message,messageId) =>
        // 收到消息
        println("consumer2 -> " + message)
        Resp.success(null)
    })
    // 定义第三个消费者，属于group2，这个消费者可以收到所有消息
    KafkaProcessor.Consumer("topic1", "group2").receive({
      (message,messageId) =>
        // 收到消息
        println("consumer3 -> " + message)
        Resp.success(null)
    })

    // 定义一个生产者
    val producer = KafkaProcessor.Producer("topic1", "client1")
    for (i <- 0 to 5) {
      producer.send(s"【$i】haha...")
    }

  }
}
