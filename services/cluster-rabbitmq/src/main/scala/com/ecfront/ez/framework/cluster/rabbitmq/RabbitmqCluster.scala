package com.ecfront.ez.framework.cluster.rabbitmq

import com.ecfront.ez.framework.core.cluster._

object RabbitmqCluster extends Cluster {

  override val rpc: ClusterRPC = RabbitmqClusterRPC

  override val mq: ClusterMQ = RabbitmqClusterMQ

  override val dist: ClusterDist = null

  override val cache: ClusterCache = null

  override val manage: ClusterManage = RabbitmqClusterManage
}
