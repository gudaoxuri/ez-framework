package com.ecfront.ez.framework.service.distributed

import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.redisson.{Config, Redisson, RedissonClient}

/**
  * 分布式服务处理类
  */
object DistributedProcessor extends LazyLogging {

  private[distributed] var redis: RedissonClient = _

  def init(address: List[String], db: Integer, auth: String = null, mode: String = "single"): Resp[String] = {
    val config = new Config()
    val matchMode = mode.toUpperCase match {
      case "SINGLE" =>
        val conf = config.useSingleServer().setAddress(address.head).setDatabase(db)
        if (auth != null && auth.nonEmpty) {
          conf.setPassword(auth)
        }
        true
      case "CLUSTER" =>
        // TODO select db
        val cluster = config.useClusterServers()
        if (auth != null && auth.nonEmpty) {
          cluster.setPassword(auth)
        }
        address.foreach(cluster.addNodeAddress(_))
        true
      case _ =>
        false
    }
    if (matchMode) {
      redis = Redisson.create(config)
      Resp.success("Distributed started")
    } else {
      Resp.notImplemented("Only support [ single ] or [ cluster ] mode")
    }
  }

  def close(): Unit = {
    if (redis != null) {
      redis.shutdown()
    }

  }
}
