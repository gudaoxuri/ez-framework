package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.storage.Page
import com.ecfront.common.{JsonHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.json.JsonObject
import io.vertx.core.{AsyncResult, Handler}
import io.vertx.ext.mongo.{FindOptions, MongoClient, UpdateOptions}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

object MongoHelper extends LazyLogging {

  var mongoClient: MongoClient = _

  def save(collection: String, save: JsonObject): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    if (save.getValue(MongoBaseModel.Id_FLAG) != null) {
      save.put("_id", save.getString(MongoBaseModel.Id_FLAG))
    }
    save.remove(MongoBaseModel.Id_FLAG)
    logger.trace(s"Mongo save : $collection -- $save")
    mongoClient.insert(collection, save, new Handler[AsyncResult[String]] {
      override def handle(res: AsyncResult[String]): Unit = {
        if (res.succeeded()) {
          val id = res.result()
          p.success(Resp.success(id))
        } else {
          logger.warn(s"Mongo save error : $collection -- $save", res.cause())
          p.success(Resp.serverError(res.cause().getMessage))
        }
      }
    })
    p.future
  }

  def update(collection: String, id: String, update: JsonObject): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    update.remove(MongoBaseModel.Id_FLAG)
    logger.trace(s"Mongo update : $collection -- $update")
    mongoClient.update(collection, new JsonObject().put("_id", id), new JsonObject().put("$set", update), new Handler[AsyncResult[Void]] {
      override def handle(res: AsyncResult[Void]): Unit = {
        if (res.succeeded()) {
          p.success(Resp.success(id))
        } else {
          logger.warn(s"Mongo update error : $collection -- $id", res.cause())
          p.success(Resp.serverError(res.cause().getMessage))
        }
      }
    })
    p.future
  }

  def saveOrUpdate(collection: String, saveOrUpdate: JsonObject): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    if (saveOrUpdate.getValue(MongoBaseModel.Id_FLAG) != null) {
      saveOrUpdate.put("_id", saveOrUpdate.getString(MongoBaseModel.Id_FLAG))
    }
    saveOrUpdate.remove(MongoBaseModel.Id_FLAG)
    logger.trace(s"Mongo saveOrUpdate : $collection -- $saveOrUpdate")
    mongoClient.save(collection, saveOrUpdate, new Handler[AsyncResult[String]] {
      override def handle(res: AsyncResult[String]): Unit = {
        if (res.succeeded()) {
          val id = if (res.result() == null) saveOrUpdate.getString("_id") else res.result()
          p.success(Resp.success(id))
        } else {
          logger.warn(s"Mongo saveOrUpdate error : $collection -- $saveOrUpdate", res.cause())
          p.success(Resp.serverError(res.cause().getMessage))
        }
      }
    })
    p.future
  }

  def updateByCond(collection: String, query: JsonObject, update: JsonObject): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    logger.trace(s"Mongo updateByCond : $collection -- $query -- $update")
    mongoClient.updateWithOptions(collection, query, update, new UpdateOptions().setMulti(true), new Handler[AsyncResult[Void]] {
      override def handle(res: AsyncResult[Void]): Unit = {
        if (res.succeeded()) {
          val id = res.result()
          p.success(Resp.success(id))
        } else {
          logger.warn(s"Mongo updateByCond error : $collection -- $query", res.cause())
          p.success(Resp.serverError(res.cause().getMessage))
        }
      }
    })
    p.future
  }

  def deleteByCond(collection: String, query: JsonObject): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    logger.trace(s"Mongo deleteByCond : $collection -- $query")
    mongoClient.remove(collection, query, new Handler[AsyncResult[Void]] {
      override def handle(res: AsyncResult[Void]): Unit = {
        if (res.succeeded()) {
          val id = res.result()
          p.success(Resp.success(id))
        } else {
          logger.warn(s"Mongo deleteByCond error : $collection -- $query", res.cause())
          p.success(Resp.serverError(res.cause().getMessage))
        }
      }
    })
    p.future
  }

  def deleteById(collection: String, id: String): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    logger.trace(s"Mongo deleteById : $collection -- $id")
    mongoClient.removeOne(collection, new JsonObject().put("_id", id), new Handler[AsyncResult[Void]] {
      override def handle(res: AsyncResult[Void]): Unit = {
        if (res.succeeded()) {
          val id = res.result()
          p.success(Resp.success(id))
        } else {
          logger.warn(s"Mongo deleteById error : $collection -- $id", res.cause())
          p.success(Resp.serverError(res.cause().getMessage))
        }
      }
    })
    p.future
  }

  def count(collection: String, query: JsonObject): Future[Resp[Long]] = {
    val p = Promise[Resp[Long]]()
    logger.trace(s"Mongo count : $collection -- $query")
    mongoClient.count(collection, query, new Handler[AsyncResult[java.lang.Long]] {
      override def handle(res: AsyncResult[java.lang.Long]): Unit = {
        if (res.succeeded()) {
          val id = res.result()
          p.success(Resp.success(id))
        } else {
          logger.warn(s"Mongo count error : $collection -- $query", res.cause())
          p.success(Resp.serverError(res.cause().getMessage))
        }
      }
    })
    p.future
  }

  def getById[E](collection: String, id: String, resultClass: Class[E] = classOf[JsonObject]): Future[Resp[E]] = {
    val p = Promise[Resp[E]]()
    logger.trace(s"Mongo getById : $collection -- $id")
    mongoClient.findOne(collection, new JsonObject().put("_id", id), null, new Handler[AsyncResult[JsonObject]] {
      override def handle(res: AsyncResult[JsonObject]): Unit = {
        if (res.succeeded()) {
          if (res.result() != null) {
            val result = res.result()
            result.put(MongoBaseModel.Id_FLAG, result.getString("_id"))
            result.remove("_id")
            if (resultClass != classOf[JsonObject]) {
              p.success(Resp.success(JsonHelper.toObject(result.encode(), resultClass)))
            } else {
              p.success(Resp.success(result.asInstanceOf[E]))
            }
          } else {
            p.success(Resp.success(null.asInstanceOf[E]))
          }
        } else {
          logger.warn(s"Mongo getById error : $collection -- $id", res.cause())
          p.success(Resp.serverError(res.cause().getMessage))
        }
      }
    })
    p.future
  }

  def find[E](collection: String, query: JsonObject, sort: JsonObject = null, resultClass: Class[E] = classOf[JsonObject]): Future[Resp[List[E]]] = {
    val p = Promise[Resp[List[E]]]()
    logger.trace(s"Mongo find : $collection -- $query")
    mongoClient.findWithOptions(collection, query, new FindOptions().setSort(sort), new Handler[AsyncResult[java.util.List[JsonObject]]] {
      override def handle(res: AsyncResult[java.util.List[JsonObject]]): Unit = {
        if (res.succeeded()) {
          if (resultClass != classOf[JsonObject]) {
            val result = res.result().map {
              row =>
                row.put(MongoBaseModel.Id_FLAG, row.getString("_id"))
                row.remove("_id")
                JsonHelper.toObject(row.encode(), resultClass)
            }.toList
            p.success(Resp.success(result))
          } else {
            val result = res.result().map {
              row =>
                row.put(MongoBaseModel.Id_FLAG, row.getString("_id"))
                row.remove("_id")
                row
            }.asInstanceOf[List[E]]
            p.success(Resp.success(result))
          }
        } else {
          logger.warn(s"Mongo find error : $collection -- $query", res.cause())
          p.success(Resp.serverError(res.cause().getMessage))
        }
      }
    })
    p.future
  }

  def page[E](collection: String, query: JsonObject, pageNumber: Long = 1, pageSize: Int = 10, sort: JsonObject = null, resultClass: Class[E] = classOf[JsonObject]): Future[Resp[Page[E]]] = {
    val p = Promise[Resp[Page[E]]]()
    logger.trace(s"Mongo page [$pageNumber] [$pageSize] : $collection -- $query")
    count(collection, query).onSuccess {
      case countResp =>
        mongoClient.findWithOptions(collection, query, new FindOptions().setSkip(((pageNumber - 1) * pageSize).toInt).setLimit(pageSize).setSort(sort), new Handler[AsyncResult[java.util.List[JsonObject]]] {
          override def handle(res: AsyncResult[java.util.List[JsonObject]]): Unit = {
            if (res.succeeded()) {
              val page = new Page[E]
              page.pageNumber = pageNumber
              page.pageSize = pageSize
              page.recordTotal = countResp.body
              page.pageTotal = (page.recordTotal + pageSize - 1) / pageSize
              if (resultClass != classOf[JsonObject]) {
                page.objects = res.result().map {
                  row =>
                    row.put(MongoBaseModel.Id_FLAG, row.getString("_id"))
                    row.remove("_id")
                    JsonHelper.toObject(row.encode(), resultClass)
                }.toList
              } else {
                page.objects = res.result().map {
                  row =>
                    row.put(MongoBaseModel.Id_FLAG, row.getString("_id"))
                    row.remove("_id")
                    row
                }.asInstanceOf[List[E]]
              }
              p.success(Resp.success(page))
            } else {
              logger.warn(s"Mongo page error : $collection -- $query", res.cause())
              p.success(Resp.serverError(res.cause().getMessage))
            }
          }
        })
    }
    p.future
  }

}
