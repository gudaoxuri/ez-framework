package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.storage.Page
import com.ecfront.common.{JsonHelper, Resp}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.json.JsonObject
import io.vertx.core.{AsyncResult, Handler}
import io.vertx.ext.mongo.{FindOptions, MongoClient, UpdateOptions}

import scala.collection.JavaConversions._
import scala.concurrent.{Future, Promise}

object MongoHelper extends LazyLogging {

  var mongoClient: MongoClient = _

  def save(collection: String, document: JsonObject): Future[Resp[String]] = {
    val p = Promise[Resp[String]]()
    mongoClient.insert(collection, document, new Handler[AsyncResult[String]] {
      override def handle(res: AsyncResult[String]): Unit = {
        if (res.succeeded()) {
          val id = res.result()
          p.success(Resp.success(id))
        } else {
          logger.warn(s"Mongo save error : $collection -- $document", res.cause())
          p.success(Resp.serverError(res.cause().getMessage))
        }
      }
    })
    p.future
  }

  def updateById(collection: String, id: String, update: JsonObject): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    mongoClient.update(collection, new JsonObject().put("_id", id), new JsonObject().put("$set", update), new Handler[AsyncResult[Void]] {
      override def handle(res: AsyncResult[Void]): Unit = {
        if (res.succeeded()) {
          val id = res.result()
          p.success(Resp.success(id))
        } else {
          logger.warn(s"Mongo updateById error : $collection -- $id", res.cause())
          p.success(Resp.serverError(res.cause().getMessage))
        }
      }
    })
    p.future
  }

  def update(collection: String, query: JsonObject, update: JsonObject): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    mongoClient.updateWithOptions(collection, query, update, new UpdateOptions().setMulti(true), new Handler[AsyncResult[Void]] {
      override def handle(res: AsyncResult[Void]): Unit = {
        if (res.succeeded()) {
          val id = res.result()
          p.success(Resp.success(id))
        } else {
          logger.warn(s"Mongo update error : $collection -- $query", res.cause())
          p.success(Resp.serverError(res.cause().getMessage))
        }
      }
    })
    p.future
  }

  def delete(collection: String, query: JsonObject): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    mongoClient.remove(collection, query, new Handler[AsyncResult[Void]] {
      override def handle(res: AsyncResult[Void]): Unit = {
        if (res.succeeded()) {
          val id = res.result()
          p.success(Resp.success(id))
        } else {
          logger.warn(s"Mongo delete error : $collection -- $query", res.cause())
          p.success(Resp.serverError(res.cause().getMessage))
        }
      }
    })
    p.future
  }

  def deleteById(collection: String, id: String): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
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
    mongoClient.findOne(collection, new JsonObject().put("_id", id), null, new Handler[AsyncResult[JsonObject]] {
      override def handle(res: AsyncResult[JsonObject]): Unit = {
        if (res.succeeded()) {
          if (res.result() != null) {
            if (resultClass != classOf[JsonObject]) {
              p.success(Resp.success(JsonHelper.toObject(res.result(), resultClass)))
            } else {
              p.success(Resp.success(res.result().asInstanceOf[E]))
            }
          } else {
            p.success(Resp.success(null))
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
    mongoClient.findWithOptions(collection, query, new FindOptions().setSort(sort), new Handler[AsyncResult[java.util.List[JsonObject]]] {
      override def handle(res: AsyncResult[java.util.List[JsonObject]]): Unit = {
        if (res.succeeded()) {
          if (resultClass != classOf[JsonObject]) {
            val result = Resp.success(res.result().map {
              row =>
                JsonHelper.toObject(row.encode(), resultClass)
            })
            p.success(result)
          } else {
            val result = Resp.success(res.result().asInstanceOf[List[E]])
            p.success(result)
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
    count(collection, query).onSuccess {
      case countResp =>
        mongoClient.findWithOptions(collection, query, new FindOptions().setSkip((pageNumber * pageSize).toInt).setLimit(pageSize).setSort(sort), new Handler[AsyncResult[java.util.List[JsonObject]]] {
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
                    JsonHelper.toObject(row.encode(), resultClass)
                }.toList
              } else {
                page.objects = res.result().asInstanceOf[List[E]]
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
