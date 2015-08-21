package com.ecfront.ez.framework.service.common

import com.ecfront.ez.framework.service.protocols.RedisService
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.redisson.core.RMap
import collection.JavaConversions._

case class DMapService[M](key: String) extends LazyLogging {

  private val map: RMap[String, M] = RedisService.redis.getMap(key)

  def put(key: String, value: M) = {
    map.fastPut(key, value)
    this
  }

  def putIfAbsent(key: String, value: M) = {
    map.putIfAbsent(key, value)
    this
  }

  def contains(key: String): Boolean = {
    map.containsKey(key)
  }

  def foreach(fun: (String, M) => Unit) = {
    map.foreach {
      item =>
        fun(item._1, item._2)
    }
    this
  }

  def get(key: String): M = {
    map.get(key)
  }

  def remove(key: String) = {
    map.fastRemove(key)
    this
  }

  def clear() = {
    map.clear()
    this
  }


}
