package com.asto.ez.framework.service.scheduler


trait ScheduleJob {

  def execute(scheduler:EZ_Scheduler):Unit

}
