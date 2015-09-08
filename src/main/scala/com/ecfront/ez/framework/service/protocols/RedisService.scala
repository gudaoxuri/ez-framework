package com.ecfront.ez.framework.service.protocols

import java.util.concurrent.atomic.AtomicBoolean

import com.ecfront.easybi.base.utils.PropertyHelper
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.redisson.{Config, Redisson}


object RedisService extends LazyLogging {

  private val isExist: AtomicBoolean = new AtomicBoolean(false)
  private[ecfront] var redis: Redisson = _

  def init(): Unit = {
    this.synchronized {
      if (!isExist.getAndSet(true)) {
        logger.info("Init Redis.")
        var path = this.getClass.getResource("/").getPath
        if (System.getProperties.getProperty("os.name").toUpperCase.indexOf("WINDOWS") != -1) {
          path = path.substring(1)
        }
        PropertyHelper.setPropertiesPath(path)

        val mode = PropertyHelper.get("ez_redis_mode", "single")
        val urls = PropertyHelper.get("ez_redis_urls").split(";")

        val config = new Config()
        mode.toUpperCase match {
          case "SINGLE" =>
            config.useSingleServer().setAddress(urls.head)
          case "MASTERSLAVE" =>
            val msConfig = config.useMasterSlaveConnection()
              .setMasterAddress(urls.head)
            urls.tail.foreach {
              url =>
                msConfig.addSlaveAddress(url)
            }
          case "CLUSTER" =>
            val clusterConfig = config.useClusterServers().setScanInterval(2000)
            urls.foreach {
              url =>
                clusterConfig.addNodeAddress(url)
            }
        }
        redis = Redisson.create(config)
        logger.info(s"Redis is started.")
      }
    }
  }

  def close(): Unit = {
    if (redis != null) {
      redis.shutdown()
    }

  }

}
