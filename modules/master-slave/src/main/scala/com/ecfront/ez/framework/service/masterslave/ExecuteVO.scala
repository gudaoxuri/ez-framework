package com.ecfront.ez.framework.service.masterslave

class BaseDTO(instanceId: String)

case class ExecReqDTO(
                      instanceId: String,
                      worker: String,
                      category: String,
                      taskInfo: Map[String, Any],
                      taskVar: Map[String, Any],
                      instanceParameters: Map[String, Any]
                    ) extends BaseDTO(instanceId)

case class ExecFinishRespDTO(
                             instanceId: String,
                             isSuccess: Boolean,
                             hasChange: Boolean,
                             message: String,
                             taskVar: Map[String, Any],
                             instanceParameters: Map[String, Any]
                           ) extends BaseDTO(instanceId)


case class ExecStartRespDTO(
                            instanceId: String
                          ) extends BaseDTO(instanceId)

