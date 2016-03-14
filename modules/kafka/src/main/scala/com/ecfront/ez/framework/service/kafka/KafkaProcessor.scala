package com.ecfront.ez.framework.service.kafka

import java.util
import java.util.concurrent.Executors
import java.util.{Properties, UUID}

import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.serialization.StringSerializer

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Future, Promise}

object KafkaProcessor extends LazyLogging {

  private val DEFAULT_AUTO_COMMIT_INTERVAL: Long = 1000

  private val executeService = Executors.newCachedThreadPool()

  private var brokerList: String = _
  private var zkList: String = _

  def init(_brokerList: String, _zkList: String): Unit = {
    brokerList = _brokerList
    zkList = _zkList
  }

  private val producers = ArrayBuffer[Producer]()
  private val consumers = ArrayBuffer[Consumer]()

  def close(): Unit = {
    producers.foreach(_.close())
    consumers.foreach(_.close())
  }

  case class Producer(topic: String, clientId: String) {

    producers += this

    private val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId)
    private val producer: KafkaProducer[String, String] = new KafkaProducer[String, String](props)

    def send(message: String): Unit = {
      val id = UUID.randomUUID().toString
      logger.trace(s"Kafka topic [$topic] send a message : $message")
      producer.send(new ProducerRecord[String, String](topic, id, message))
    }

    def sendFuture(message: String): Future[Resp[(String, String)]] = {
      val p = Promise[Resp[(String, String)]]()
      val id = UUID.randomUUID().toString
      logger.trace(s"Kafka topic [$topic] send a message : $message")
      producer.send(new ProducerRecord[String, String](topic, id, message), new Callback {
        override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
          if (metadata != null) {
            p.success(Resp.success((id, message)))
          } else {
            logger.error(s"Kafka send error : ${exception.getMessage}", exception)
            p.success(Resp.serverError(s"Kafka send error : ${exception.getMessage}"))
          }
        }
      })
      p.future
    }

    def close(): Unit = {
      if (producer != null) producer.close()
    }

  }

  case class Consumer(groupId: String, topic: String, autoCommit: Boolean = false, autoCommitInterval: Long = DEFAULT_AUTO_COMMIT_INTERVAL) {

    consumers += this

    private val props = new Properties()
    props.put("zookeeper.connect", zkList)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    props.put("auto.commit.enable", autoCommit + "")
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, autoCommitInterval + "")

    val consumer = kafka.consumer.Consumer.createJavaConsumerConnector(new kafka.consumer.ConsumerConfig(props))

    def receive(callback: => String => Resp[Void]): Unit = {
      executeService.execute(new Runnable {
        override def run(): Unit = {
          val consumerMap = consumer.createMessageStreams(new util.HashMap[String, Integer] {
            {
              put(topic, new Integer(1))
            }
          })
          consumerMap.get(topic).foreach {
            partition =>
              val it = partition.iterator()
              while (it.hasNext()) {
                val message = new String(it.next().message())
                logger.trace(s"Kafka topic [$topic] at gorup [$groupId] partition [${partition.toString()}] received a message : $message")
                try {
                  val resp = callback(message)
                  if (resp) {
                    if (!autoCommit) {
                      consumer.commitOffsets()
                    }
                  } else {
                    logger.warn(s"Kafka topic [$topic] at gorup [$groupId] partition [${partition.toString()}]" +
                      s" process error  : $message [${resp.code}] ${resp.message}")
                  }
                } catch {
                  case e: Throwable =>
                    logger.warn(s"Kafka topic [$topic] at gorup [$groupId] partition [${partition.toString()}] received a error message : $message", e)
                }
              }
          }
        }
      })
    }

    def close(): Unit = {
      if (consumer != null) consumer.shutdown()
    }

  }

}
