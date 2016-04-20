package com.ecfront.ez.framework.service.distributed

import com.ecfront.ez.framework.service.redis.RedisProcessor
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.redisson.core.RMap

import scala.collection.JavaConversions._

/**
  * 分布式Map，key为string , value为自定义类型
  *
  * @param key Map名
  * @tparam M Map项的类型
  */
case class DMapService[M](key: String) extends LazyLogging {

  private val map: RMap[String, M] = RedisProcessor.redis.getMap(key)

  def put(key: String, value: M): this.type = {
    map.fastPut(key, value)
    this
  }

  def putIfAbsent(key: String, value: M): this.type = {
    map.putIfAbsent(key, value)
    this
  }

  def contains(key: String): Boolean = {
    map.containsKey(key)
  }

  def foreach(fun: (String, M) => Unit): this.type = {
    map.foreach {
      item =>
        fun(item._1, item._2)
    }
    this
  }

  def get(key: String): M = {
    map.get(key)
  }

  def remove(key: String): this.type = {
    map.fastRemove(key)
    this
  }

  def clear(): this.type = {
    map.clear()
    this
  }

}
