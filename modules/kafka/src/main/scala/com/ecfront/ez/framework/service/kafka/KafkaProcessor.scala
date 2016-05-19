package com.ecfront.ez.framework.service.kafka

import java.util
import java.util.concurrent.{ConcurrentHashMap, Executors}
import java.util.{Properties, UUID}

import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer, OffsetAndMetadata, OffsetCommitCallback}
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.collection.JavaConversions._
import scala.concurrent.{Future, Promise}

/**
  * Kafka操作
  */
object KafkaProcessor extends LazyLogging {

  private val DEFAULT_AUTO_COMMIT_INTERVAL: Long = 1000

  private val executeService = Executors.newCachedThreadPool()

  private var brokerList: String = _

  /**
    * 初始化
    *
    * @param _brokerList kafka broker 列表，逗号分隔
    */
  def init(_brokerList: String): Unit = {
    brokerList = _brokerList
  }

  // 生产者集合
  private val producers = collection.mutable.Map[String, Producer]()
  // 消费者集合
  private val consumers = collection.mutable.Map[String, Consumer]()

  /**
    * 关闭所有实例
    */
  def close(): Unit = {
    producers.values.foreach(_.close())
    consumers.values.foreach(_.close())
  }

  /**
    * 生产者类
    *
    * @param topic    主题
    * @param clientId 客户端ID
    */
  case class Producer(topic: String, clientId: String) {

    producers += topic -> this

    private val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId)
    private val producer: KafkaProducer[String, String] = new KafkaProducer[String, String](props)

    /**
      * 发送消息
      *
      * @param message   消息
      * @param messageId 消息ID，默认自动生成UUID
      */
    def send(message: String, messageId: String = UUID.randomUUID().toString): Unit = {
      logger.trace(s"Kafka topic [$topic] send a message[$messageId] : $message")
      producer.send(new ProducerRecord[String, String](topic, messageId, message), new Callback {
        override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
          if (exception != null) {
            logger.error(s"Kafka topic [$topic] send a error message[$messageId] : $message", exception)
          }
        }
      })
    }

    /**
      * 发送消息
      *
      * @param message   消息
      * @param messageId 消息ID，默认自动生成UUID
      * @return 是否成功 ,返回对应的消息ID（自动生成）和message
      */
    def sendFuture(message: String, messageId: String = UUID.randomUUID().toString): Future[Resp[(String, String)]] = {
      val p = Promise[Resp[(String, String)]]()
      logger.trace(s"Kafka topic [$topic] send a message[$messageId] : $message")
      producer.send(new ProducerRecord[String, String](topic, messageId, message), new Callback {
        override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
          if (metadata != null) {
            p.success(Resp.success((messageId, message)))
          } else {
            logger.error(s"Kafka send error message[$messageId] : ${exception.getMessage}", exception)
            p.success(Resp.serverError(s"Kafka send error message[$messageId] : ${exception.getMessage}"))
          }
        }
      })
      p.future
    }

    private lazy val localQueues = collection.mutable.Map[String, ConcurrentHashMap[String, String]]()

    /**
      * 发送需要回复的消息
      *
      * @param message  消息
      * @param ackTopic 回复主题
      * @param timeout  等待超时时间（ms）
      * @return 回复的消息
      */
    def ack(message: String, ackTopic: String, timeout: Long = 30 * 1000): Resp[String] = {
      if (!consumers.contains(ackTopic)) {
        consumers += ackTopic -> Consumer(ackTopic, clientId)
        localQueues += ackTopic -> new ConcurrentHashMap[String, String]()

        consumers(ackTopic).receive({
          (message, messageId) =>
            localQueues(ackTopic).put(messageId, if (message == null) "" else message)
            Resp.success(null)
        })
      }
      val messageId = UUID.randomUUID().toString
      send(message, messageId)
      val expireTime = System.currentTimeMillis() + timeout
      val ackMessageQueue = localQueues(ackTopic)
      while (!ackMessageQueue.containsKey(messageId) && expireTime > System.currentTimeMillis()) {
        Thread.sleep(100)
      }
      if (ackMessageQueue.containsKey(messageId)) {
        val ackMessage = ackMessageQueue.get(messageId)
        ackMessageQueue.remove(messageId)
        Resp.success(ackMessage)
      } else {
        Resp("-1", "Timeout")
      }
    }

    /**
      * 关闭当前生产者
      */
    def close(): Unit = {
      if (producer != null) producer.close()
    }

  }

  /**
    * 消费者类
    *
    * @param topic              主题
    * @param groupId            组ID，用于发布-订阅模式
    * @param autoCommit         是否自动提交
    * @param autoCommitInterval 自动提供间隔
    */
  case class Consumer(topic: String, groupId: String, autoCommit: Boolean = false, autoCommitInterval: Long = DEFAULT_AUTO_COMMIT_INTERVAL) {

    consumers += topic -> this

    private val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoCommit + "")
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, autoCommitInterval + "")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)

    val consumer = new KafkaConsumer[String, String](props)

    consumer.subscribe(List(topic))


    /**
      * 接收消息
      *
      * @param fun      收到消息后的回调方法
      * @param ackTopic 回复主题，如果存在会回复消息
      */
    def receive(fun: (String, String) => Resp[String], ackTopic: String = null): Unit = {
      if (ackTopic != null && !producers.contains(ackTopic)) {
        producers += ackTopic -> Producer(ackTopic, groupId)
      }
      executeService.execute(new Runnable {
        override def run(): Unit = {
          while (true) {
            val records = consumer.poll(100)
            records.foreach {
              record =>
                val messageId: String = record.key()
                val message: String = record.value()
                logger.trace(s"Kafka topic [$topic] at group [$groupId] received a message[$messageId] : $message")
                try {
                  val resp = fun(message, messageId)
                  if (resp) {
                    if (!autoCommit) {
                      consumer.commitAsync(new OffsetCommitCallback {
                        override def onComplete(offsets: util.Map[TopicPartition, OffsetAndMetadata], exception: Exception): Unit = {
                          if (ackTopic != null) {
                            producers(ackTopic).send(resp.body, messageId)
                          }
                        }
                      })
                    }
                  } else {
                    logger.warn(s"Kafka topic [$topic] at group [$groupId]" +
                      s" process error a message[$messageId] : $message [${resp.code}] ${resp.message}")
                  }
                } catch {
                  case e: Throwable =>
                    logger.warn(s"Kafka topic [$topic] at group [$groupId] received a error message[$messageId] : $message", e)
                }
            }
          }
        }
      })
    }

    /**
      * 关闭当前消费者
      */
    def close(): Unit = {
      if (consumer != null) consumer.close()
    }

  }

}
