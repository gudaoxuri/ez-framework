package com.ecfront.ez.framework.service.protocols

import java.util.UUID

import com.ecfront.common.{Req, Resp}
import com.ecfront.ez.framework.service.BasicService
import com.ecfront.ez.framework.storage.{IdModel, PageModel}
import org.redisson.core.RMap

import scala.collection.JavaConversions._

trait CacheService[M <: AnyRef, R <: Req] extends BasicService[M, R] {

  private val container: RMap[String, M] = RedisService.redis.getMap(_modelClazz.getName)

  override protected def _doFindAll(request: Option[R]): Resp[List[M]] = {
    Resp.success(container.values().toList)
  }

  override protected def _doSave(model: M, request: Option[R]): Resp[String] = {
    val id = model match {
      case m: IdModel =>
        if (m.id == null || m.id.isEmpty) {
          m.id = UUID.randomUUID().toString
        }
        m.id
      case _ => UUID.randomUUID().toString
    }
    container.fastPut(id, model)
    Resp.success(id)
  }

  override protected def _doGetById(id: String, request: Option[R]): Resp[M] = {
    if (container.containsKey(id)) {
      Resp.success(container.get(id))
    } else {
      Resp.notFound(id)
    }
  }

  override protected def _doUpdate(id: String, model: M, request: Option[R]): Resp[String] = {
    container.fastPut(id, model)
    Resp.success(id)
  }

  override protected def _doDeleteById(id: String, request: Option[R]): Resp[String] = {
    container.fastRemove(id)
    Resp.success(id)
  }

  override protected def _doDeleteAll(request: Option[R]): Resp[List[String]] = {
    val ids = container.keySet()
    container.clear()
    Resp.success(ids.toList)
  }

  override protected def _doFindByCondition(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[List[M]] = Resp.notImplemented("findByCondition")

  override protected def _doGetByCondition(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[M] = Resp.notImplemented("getByCondition")

  override protected def _doPageAll(pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]] = Resp.notImplemented("pageAll")

  override protected def _doPageByCondition(condition: String, parameters: Option[List[Any]], pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]] = Resp.notImplemented("pageByCondition")

  override protected def _doDeleteByCondition(condition: String, parameters: Option[List[Any]], request: Option[R]): Resp[List[String]] = Resp.notImplemented("deleteByCondition")

  override protected def _doFindAllDisable(request: Option[R]): Resp[List[M]] = Resp.notImplemented("findAllDisable")

  override protected def _doFindAllEnable(request: Option[R]): Resp[List[M]] = Resp.notImplemented("findAllEnable")

  override protected def _doPageAllDisable(pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]] = Resp.notImplemented("pageAllDisable")

  override protected def _doPageAllEnable(pageNumber: Long, pageSize: Long, request: Option[R]): Resp[PageModel[M]] = Resp.notImplemented("pageAllEnable")
}
