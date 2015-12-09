package com.asto.ez.framework.storage.mongo

import com.ecfront.common.Resp
import io.vertx.core.json.JsonObject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

object MongoExecutor {

  def save(entityInfo: MongoEntityInfo, collection: String, save: JsonObject): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    if (entityInfo.uniqueFieldNames.nonEmpty) {
      val existQuery = new JsonObject()
      entityInfo.uniqueFieldNames.filter(save.containsKey).foreach {
        field =>
          existQuery.put(field, save.getValue(field))
      }
      MongoProcessor.exist(collection, existQuery).onSuccess {
        case existResp =>
          if (existResp) {
            if (existResp.body) {
              p.success(Resp.badRequest(entityInfo.uniqueFieldNames.map {
                field =>
                  if (entityInfo.fieldLabel.contains(field)) {
                    entityInfo.fieldLabel(field)
                  } else {
                    field
                  }
              }.mkString("[", ",", "]") + " 不唯一"))
            } else {
              MongoProcessor.save(collection, save).onSuccess {
                case saveResp =>
                  p.success(saveResp)
              }
            }
          } else {
            p.success(existResp)
          }
      }
    } else {
      MongoProcessor.save(collection, save).onSuccess {
        case saveResp =>
          p.success(saveResp)
      }
    }
    p.future
  }

  def update(entityInfo: MongoEntityInfo, collection: String, id: String, update: JsonObject): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    if (entityInfo.uniqueFieldNames.nonEmpty) {
      val existQuery = new JsonObject()
      entityInfo.uniqueFieldNames.filter(update.containsKey).foreach {
        field =>
          existQuery.put(field, update.getValue(field))
      }
      existQuery.put("_id", new JsonObject().put("$ne", id))
      MongoProcessor.exist(collection, existQuery).onSuccess {
        case existResp =>
          if (existResp) {
            if (existResp.body) {
              p.success(Resp.badRequest(entityInfo.uniqueFieldNames.map {
                field =>
                  if (entityInfo.fieldLabel.contains(field)) {
                    entityInfo.fieldLabel(field)
                  } else {
                    field
                  }
              }.mkString("[", ",", "]") + " 不唯一"))
            } else {
              MongoProcessor.update(collection, id, update).onSuccess {
                case updateResp =>
                  p.success(updateResp)
              }
            }
          } else {
            p.success(existResp)
          }
      }
    } else {
      MongoProcessor.update(collection, id, update).onSuccess {
        case updateResp =>
          p.success(updateResp)
      }
    }
    p.future
  }

  def saveOrUpdate(entityInfo: MongoEntityInfo, collection: String, id: String, saveOrUpdate: JsonObject): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    if (saveOrUpdate.getValue(MongoBaseModel.Id_FLAG) == null) {
      save(entityInfo, collection, saveOrUpdate).onSuccess {
        case saveResp =>
          p.success(saveResp)
      }
    } else {
      update(entityInfo, collection, saveOrUpdate.getString(MongoBaseModel.Id_FLAG), saveOrUpdate).onSuccess {
        case updateResp =>
          p.success(updateResp)
      }
    }
    p.future
  }


}
