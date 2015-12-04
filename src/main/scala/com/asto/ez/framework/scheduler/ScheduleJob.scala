package com.asto.ez.framework.scheduler


trait ScheduleJob {

  def execute(scheduler:EZ_Scheduler):Unit

}
