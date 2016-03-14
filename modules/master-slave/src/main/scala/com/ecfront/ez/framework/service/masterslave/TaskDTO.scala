package com.ecfront.ez.framework.service.masterslave

/**
  * 基础DTO
  *
  * @param instanceId 实例ID
  */
class BaseDTO(instanceId: String)

/**
  * 任务准备DTO
  *
  * 由Master发送给Worker，Worker收到后会将任务放入执行队列
  *
  * @param instanceId         实例ID
  * @param worker             对应的worker名称
  * @param category           对应的处理器名称，一个worker中可以有多个处理器
  * @param taskInfo           任务信息，不同的任务可以有不同的信息
  *                           如jdbc数据抽取信息中就会包含各种jdbc连接信息，hive操作任务会包含hive的连接信息等
  *                           这些信息在整个任务的生命周期中是不变的
  * @param taskVar            任务变量，每次任务执行后都可能会变更的信息
  *                           如最后一次更新时间戳就是个变量，每次执行后都会更新
  * @param instanceParameters 实例参数，每次任务执行时由外部传入的参数
  *                           如数据更新任务，外部触发其执行时可以指定要更新的条件等信息
  */
case class TaskPrepareDTO(
                           instanceId: String,
                           worker: String,
                           category: String,
                           taskInfo: Map[String, Any],
                           taskVar: Map[String, Any],
                           instanceParameters: Map[String, Any]
                         ) extends BaseDTO(instanceId)

/**
  * 任务完成DTO
  *
  * 任务执行完成后Worker发送给Master，告知此任务已完成
  *
  * @param instanceId         实例ID
  * @param isSuccess          是否成功
  * @param hasChange          是否有变更，只在isSuccess=true时有意义
  * @param message            错误消息
  * @param taskVar            任务变量，只在isSuccess=true时有意义
  * @param instanceParameters 实例参数，只在isSuccess=true时有意义
  */
case class TaskFinishDTO(
                          instanceId: String,
                          isSuccess: Boolean,
                          hasChange: Boolean,
                          message: String,
                          taskVar: Map[String, Any],
                          instanceParameters: Map[String, Any]
                        ) extends BaseDTO(instanceId)


/**
  * 任务开始DTO
  *
  * 从执行队列中弹出执行前由Worker发送给Master，告知开始执行此任务
  *
  * @param instanceId 实例ID
  */
case class TaskStartDTO(
                         instanceId: String
                       ) extends BaseDTO(instanceId)

