package com.ecfront.ez.framework.core.cluster

import com.ecfront.ez.framework.core.logger.Logging

trait Cluster extends Logging {

  val rpc: ClusterRPC

  val mq: ClusterMQ

  val dist: ClusterDist

  val cache: ClusterCache

  val manage: ClusterManage

}

