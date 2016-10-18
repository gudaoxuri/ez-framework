package com.ecfront.ez.framework.core

import java.util.Date

import com.ecfront.common.JsonHelper
import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.core.rpc.{OptInfo, RPCProcessor}

import scala.beans.BeanProperty
import scala.language.implicitConversions

class EZContext {
  @BeanProperty var id: String = _
  @BeanProperty var sourceIP: String = _
  @BeanProperty var startTime: Long = _
  @BeanProperty var sourceRPCPath: String = _
  @BeanProperty var token: String = _
  @BeanProperty var optAccCode: String = _
  @BeanProperty var optOrgCode: String = _
  @BeanProperty var trace:collection.mutable.Map[String,String] = _

  lazy val optInfo: Option[OptInfo] = {
    if (token != null && token.nonEmpty) {
      val result = EZ.cache.get(RPCProcessor.TOKEN_INFO_FLAG + token)
      if (result != null && result.nonEmpty) {
        Some(JsonHelper.toObject[OptInfo](result))
      } else {
        None
      }
    } else {
      None
    }
  }
}

object EZContext {

  val _context = new ThreadLocal[EZContext]

  private[core] def getContext: EZContext = {
    var cxt = _context.get()
    if (cxt == null) {
      cxt = new EZContext
      cxt.id = EZ.createUUID
      cxt.startTime = TimeHelper.msf.format(new Date).toLong
      cxt.sourceIP = EZ.Info.projectIp
      cxt.sourceRPCPath = ""
      cxt.token = ""
      cxt.optAccCode = ""
      cxt.optOrgCode = ""
      setContext(cxt)
    }
    cxt
  }

  private[core] def setContext(context: EZContext): Unit = {
    if (context.token == null) {
      context.token = ""
    }
    if (context.optAccCode == null) {
      context.optAccCode = ""
    }
    if (context.optOrgCode == null) {
      context.optOrgCode = ""
    }
    _context.set(context)
  }

}
