package com.ecfront.ez.framework.cluster.redis

import com.ecfront.ez.framework.core.cluster._

object RedisCluster extends Cluster {

  override val rpc: ClusterRPC = null

  override val mq: ClusterMQ = null

  override val dist: ClusterDist = RedisClusterDist

  override val cache: ClusterCache = RedisClusterCache

  override val manage: ClusterManage = RedisClusterManage

}


