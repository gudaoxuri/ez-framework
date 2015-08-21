package com.ecfront.ez.framework.rpc.cluster

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

import com.hazelcast.config.Config
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.{AsyncResult, Handler, Vertx, VertxOptions}
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager

/**
 * 集群管理
 */
object ClusterManager extends LazyLogging {

  //集群对象
  var clusterManager: HazelcastClusterManager = _
  var vertx: Vertx = _

  def initCluster(host: String="127.0.0.1"): Unit = {
    if (!existInstance.getAndSet(true)) {
      val counter = new CountDownLatch(1)
      val hazelcastConfig = new Config()
      hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j")
      if (host != "127.0.0.1") {
        hazelcastConfig.getNetworkConfig.getJoin.getMulticastConfig.setEnabled(false)
        hazelcastConfig.getNetworkConfig.getJoin.getTcpIpConfig.setEnabled(true).addMember("127.0.0.1")
        logger.info("Host is 127.0.0.1 , close Multicast.")
      }
      clusterManager = new HazelcastClusterManager(hazelcastConfig)
      Vertx.clusteredVertx(new VertxOptions().setClustered(true)
        .setClusterManager(clusterManager), new Handler[AsyncResult[Vertx]] {
        override def handle(event: AsyncResult[Vertx]): Unit = {
          if (event.succeeded()) {
            logger.info("Vertx cluster start success.")
            vertx = event.result()
            counter.countDown()
          } else {
            logger.error("Vertx cluster start failed!", event.cause())
          }
        }
      })
      counter.await()
    }
  }

  def destoryCluster(): Unit ={
    existInstance.set(false)
  }

  private val existInstance = new AtomicBoolean(false)

}
