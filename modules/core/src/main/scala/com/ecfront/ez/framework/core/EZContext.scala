package com.ecfront.ez.framework.core

import java.util.Date

import com.ecfront.ez.framework.core.helper.TimeHelper

import scala.beans.BeanProperty
import scala.language.implicitConversions

class EZContext {
  @BeanProperty var id: String = _
  @BeanProperty var sourceIP: String = _
  @BeanProperty var startTime: Long = _
  @BeanProperty var sourceRPCPath: String = _
  @BeanProperty var optAccCode: String = _
  @BeanProperty var optOrgCode: String = _
}

object EZContext {

  val _context = new ThreadLocal[EZContext]

  private[core] def getContext: EZContext = {
    var cxt = _context.get()
    if (cxt == null) {
      cxt.id = EZ.createUUID
      cxt.startTime = TimeHelper.msf.format(new Date).toLong
      cxt.sourceIP = EZ.Info.projectIp
      cxt.sourceRPCPath = ""
      cxt.optAccCode = ""
      cxt.optOrgCode = ""
      cxt.id = EZ.createUUID
      cxt = new EZContext
      setContext(cxt)
    }
    cxt
  }

  private[core] def setContext(context: EZContext): Unit = {
    _context.set(context)
  }

}
