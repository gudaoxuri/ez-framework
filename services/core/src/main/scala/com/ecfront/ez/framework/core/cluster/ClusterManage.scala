package com.ecfront.ez.framework.core.cluster

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.logger.Logging
import com.fasterxml.jackson.databind.JsonNode

trait ClusterManage extends Logging {

  def init(config: JsonNode): Resp[Void]

  def close(): Unit

}