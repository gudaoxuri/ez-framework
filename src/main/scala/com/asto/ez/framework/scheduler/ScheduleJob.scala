package com.asto.ez.framework.scheduler

import com.ecfront.common.AsyncResp


trait ScheduleJob {

  def execute(scheduler:EZ_Scheduler,p: AsyncResp[Void]):Unit

}
