package com.ecfront.ez.framework.service.storage.jdbc

import java.lang
import java.sql.SQLTransactionRollbackException
import java.util.concurrent.atomic.AtomicLong

import com.ecfront.common.{BeanHelper, JsonHelper, Resp}
import com.ecfront.ez.framework.core.EZContext
import com.ecfront.ez.framework.service.storage.foundation.Page
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.json.{JsonArray, JsonObject}
import io.vertx.core.{AsyncResult, Handler}
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.{ResultSet, SQLConnection, UpdateResult}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success}

/**
  * JDBC 操作
  */
object JDBCProcessor extends LazyLogging {

  var dbClient: JDBCClient = _

  /**
    * update
    *
    * @param sql        sql
    * @param parameters 参数
    * @param retryTimes 重试次数，失败会重试10次
    * @return update结果
    */
  def update(sql: String, parameters: List[Any] = null, retryTimes: Int = 0): Resp[Void] = {
    Await.result(Async.update(sql, parameters, retryTimes), Duration.Inf)
  }

  /**
    * 批处理
    *
    * @param sql           sql
    * @param parameterList 参数列表
    * @return 处理结果
    */
  def batch(sql: String, parameterList: List[List[Any]] = null): Resp[Void] = {
    Await.result(Async.batch(sql, parameterList), Duration.Inf)
  }

  /**
    * 获取一条记录
    *
    * @param sql         sql
    * @param parameters  参数
    * @param resultClass 记录类型
    * @tparam E 记录类型
    * @return 获取到的记录
    */
  def get[E](sql: String, parameters: List[Any], resultClass: Class[E]): Resp[E] = {
    Await.result(Async.get[E](sql, parameters, resultClass), Duration.Inf)
  }

  /**
    * 查找
    *
    * @param sql         sql
    * @param parameters  参数
    * @param resultClass 记录类型
    * @tparam E 记录类型
    * @return 获取到的记录
    */
  def find[E](sql: String, parameters: List[Any], resultClass: Class[E]): Resp[List[E]] = {
    Await.result(Async.find[E](sql, parameters, resultClass), Duration.Inf)
  }

  /**
    * 分页
    *
    * @param sql         sql
    * @param parameters  参数
    * @param pageNumber  当前页，从1开始
    * @param pageSize    每页条数
    * @param resultClass 记录类型
    * @tparam E 记录类型
    * @return 获取到的记录
    */
  def page[E](sql: String, parameters: List[Any], pageNumber: Long, pageSize: Int, resultClass: Class[E]): Resp[Page[E]] = {
    Await.result(Async.page[E](sql, parameters, pageNumber, pageSize, resultClass), Duration.Inf)
  }

  /**
    * 计数
    *
    * @param sql        sql
    * @param parameters 参数
    * @return 计数结果
    */
  def count(sql: String, parameters: List[Any]): Resp[Long] = {
    Await.result(Async.count(sql, parameters), Duration.Inf)
  }

  /**
    * 判断是否存在
    *
    * @param sql        sql
    * @param parameters 参数
    * @return 是否存在
    */
  def exist(sql: String, parameters: List[Any]): Resp[Boolean] = {
    Await.result(Async.exist(sql, parameters), Duration.Inf)
  }

  object Async {

    /**
      * update
      *
      * @param sql        sql
      * @param parameters 参数
      * @param retryTimes 重试次数，失败会重试10次
      * @return update结果
      */
    def update(sql: String, parameters: List[Any] = null, retryTimes: Int = 0): Future[Resp[Void]] = {
      val p = Promise[Resp[Void]]()
      val finalParameters = formatParameters(parameters)
      logger.trace(s"JDBC update : $sql [$finalParameters]")
      db.onComplete {
        case Success(conn) =>
          try {
            if (finalParameters == null) {
              conn.update(sql,
                new Handler[AsyncResult[UpdateResult]] {
                  override def handle(event: AsyncResult[UpdateResult]): Unit = {
                    if (event.succeeded()) {
                      conn.close()
                      p.success(Resp.success(null))
                    } else {
                      conn.close()
                      event.cause() match {
                        case throws if classOf[SQLTransactionRollbackException].isAssignableFrom(throws.getClass) =>
                          if (retryTimes < 10) {
                            EZContext.vertx.setTimer(5000 * (retryTimes + 1), new Handler[lang.Long] {
                              override def handle(event: lang.Long): Unit = {
                                logger.debug(s"JDBC update problem try times [${retryTimes + 1}] : $sql [$finalParameters]")
                                update(sql, parameters, retryTimes + 1)
                              }
                            })
                          } else {
                            logger.warn(s"JDBC update error and try times > 10 : $sql [$finalParameters]", event.cause())
                            p.success(Resp.serverError(event.cause().getMessage))
                          }
                        case _ =>
                          logger.warn(s"JDBC update error : $sql [$finalParameters]", event.cause())
                          p.success(Resp.serverError(event.cause().getMessage))
                      }
                    }
                  }
                }
              )
            } else {
              conn.updateWithParams(sql,
                new JsonArray(finalParameters),
                new Handler[AsyncResult[UpdateResult]] {
                  override def handle(event: AsyncResult[UpdateResult]): Unit = {
                    if (event.succeeded()) {
                      conn.close()
                      p.success(Resp.success(null))
                    } else {
                      conn.close()
                      event.cause() match {
                        case throws if classOf[SQLTransactionRollbackException].isAssignableFrom(throws.getClass) =>
                          if (retryTimes < 10) {
                            EZContext.vertx.setTimer(5000 * (retryTimes + 1), new Handler[lang.Long] {
                              override def handle(event: lang.Long): Unit = {
                                logger.debug(s"JDBC update problem try times [${retryTimes + 1}] : $sql [$finalParameters]")
                                update(sql, parameters, retryTimes + 1)
                              }
                            })
                          } else {
                            logger.warn(s"JDBC update error and try times > 10 : $sql [$finalParameters]", event.cause())
                            p.success(Resp.serverError(event.cause().getMessage))
                          }
                        case _ =>
                          logger.warn(s"JDBC update error : $sql [$finalParameters]", event.cause())
                          p.success(Resp.serverError(event.cause().getMessage))
                      }
                    }
                  }
                }
              )
            }
          } catch {
            case ex: Throwable =>
              conn.close()
              logger.error(s"JDBC execute error : $sql [$finalParameters]", ex)
              p.success(Resp.serverError(ex.getMessage))
          }
        case Failure(ex) =>
          p.success(Resp.serverUnavailable(ex.getMessage))
      }
      p.future
    }

    /**
      * 批处理
      *
      * @param sql           sql
      * @param parameterList 参数列表
      * @return 处理结果
      */
    def batch(sql: String, parameterList: List[List[Any]] = null): Future[Resp[Void]] = {
      val p = Promise[Resp[Void]]()
      logger.trace(s"JDBC batch : $sql [$parameterList]")
      db.onComplete {
        case Success(conn) =>
          try {
            val counter = new AtomicLong(parameterList.length)
            parameterList.foreach {
              parameters =>
                val finalParameters = formatParameters(parameters)
                conn.updateWithParams(sql,
                  new JsonArray(finalParameters),
                  new Handler[AsyncResult[UpdateResult]] {
                    override def handle(event: AsyncResult[UpdateResult]): Unit = {
                      if (!event.succeeded()) {
                        logger.warn(s"JDBC execute error : $sql [$finalParameters]", event.cause())
                      }
                      if (counter.decrementAndGet() == 0) {
                        conn.close()
                        p.success(Resp.success(null))
                      }
                    }
                  }
                )
            }
          } catch {
            case ex: Throwable =>
              conn.close()
              logger.error(s"JDBC execute error : $sql", ex)
              p.success(Resp.serverError(ex.getMessage))
          }
        case Failure(ex) =>
          p.success(Resp.serverUnavailable(ex.getMessage))
      }
      p.future
    }

    private[jdbc] def formatParameters(parameters: List[Any]): List[Any] = {
      if (parameters == null) {
        null
      } else {
        parameters.map {
          case p if
          p.isInstanceOf[String] || p.isInstanceOf[Int] || p.isInstanceOf[Long] || p.isInstanceOf[Boolean] ||
            p.isInstanceOf[Double] || p.isInstanceOf[Float] || p.isInstanceOf[BigDecimal] ||
            p.isInstanceOf[Char] || p.isInstanceOf[Short] || p.isInstanceOf[Byte]
          => p
          case p =>
            JsonHelper.toJsonString(p)
        }
      }
    }

    /**
      * 获取一条记录
      *
      * @param sql         sql
      * @param parameters  参数
      * @param resultClass 记录类型
      * @tparam E 记录类型
      * @return 获取到的记录
      */
    def get[E](sql: String, parameters: List[Any], resultClass: Class[E]): Future[Resp[E]] = {
      val p = Promise[Resp[E]]()
      logger.trace(s"JDBC get : $sql [$parameters]")
      db.onComplete {
        case Success(conn) =>
          try {
            conn.queryWithParams(sql,
              new JsonArray(parameters),
              new Handler[AsyncResult[ResultSet]] {
                override def handle(event: AsyncResult[ResultSet]): Unit = {
                  if (event.succeeded()) {
                    val row = if (event.result().getNumRows == 1) {
                      event.result().getRows.get(0)
                    } else {
                      null
                    }
                    if (row != null) {
                      if (resultClass != classOf[JsonObject]) {
                        val result = Resp.success(convertObject(row, resultClass))
                        conn.close()
                        p.success(result)
                      } else {
                        val result = Resp.success(row.asInstanceOf[E])
                        conn.close()
                        p.success(result)
                      }
                    } else {
                      conn.close()
                      p.success(Resp.success(null.asInstanceOf[E]))
                    }
                  } else {
                    conn.close()
                    logger.warn(s"JDBC execute error : $sql [$parameters]", event.cause())
                    p.success(Resp.serverError(event.cause().getMessage))
                  }
                }
              }
            )
          } catch {
            case ex: Throwable =>
              conn.close()
              logger.error(s"JDBC execute error : $sql [$parameters]", ex)
              p.success(Resp.serverError(ex.getMessage))
          }
        case Failure(ex) => p.success(Resp.serverUnavailable(ex.getMessage))
      }
      p.future
    }

    /**
      * 查找
      *
      * @param sql         sql
      * @param parameters  参数
      * @param resultClass 记录类型
      * @tparam E 记录类型
      * @return 获取到的记录
      */
    def find[E](sql: String, parameters: List[Any], resultClass: Class[E]): Future[Resp[List[E]]] = {
      val p = Promise[Resp[List[E]]]()
      logger.trace(s"JDBC find : $sql [$parameters]")
      db.onComplete {
        case Success(conn) =>
          try {
            conn.queryWithParams(sql,
              new JsonArray(parameters),
              new Handler[AsyncResult[ResultSet]] {
                override def handle(event: AsyncResult[ResultSet]): Unit = {
                  if (event.succeeded()) {
                    val rows = event.result().getRows.toList
                    if (resultClass != classOf[JsonObject]) {
                      val result = Resp.success(rows.map {
                        convertObject(_, resultClass)
                      })
                      conn.close()
                      p.success(result)
                    } else {
                      val result = Resp.success(rows.asInstanceOf[List[E]])
                      conn.close()
                      p.success(result)
                    }
                  } else {
                    conn.close()
                    logger.warn(s"JDBC execute error : $sql [$parameters]", event.cause())
                    p.success(Resp.serverError(event.cause().getMessage))
                  }
                }
              }
            )
          } catch {
            case ex: Throwable =>
              conn.close()
              logger.error(s"JDBC execute error : $sql [$parameters]", ex)
              p.success(Resp.serverError(ex.getMessage))
          }
        case Failure(ex) => p.success(Resp.serverUnavailable(ex.getMessage))
      }
      p.future
    }

    /**
      * 分页
      *
      * @param sql         sql
      * @param parameters  参数
      * @param pageNumber  当前页，从1开始
      * @param pageSize    每页条数
      * @param resultClass 记录类型
      * @tparam E 记录类型
      * @return 获取到的记录
      */
    def page[E](sql: String, parameters: List[Any], pageNumber: Long, pageSize: Int, resultClass: Class[E]): Future[Resp[Page[E]]] = {
      val p = Promise[Resp[Page[E]]]()
      logger.trace(s"JDBC page : $sql [$parameters]")
      db.onComplete {
        case Success(conn) =>
          try {
            countInner(sql, parameters, conn).onSuccess {
              case countResp =>
                if (countResp) {
                  val page = new Page[E]
                  page.pageNumber = pageNumber
                  page.pageSize = pageSize
                  page.recordTotal = countResp.body
                  page.pageTotal = (page.recordTotal + pageSize - 1) / pageSize
                  val limitSql = s"$sql limit ${(pageNumber - 1) * pageSize} ,$pageSize"
                  conn.queryWithParams(limitSql,
                    new JsonArray(parameters),
                    new Handler[AsyncResult[ResultSet]] {
                      override def handle(event: AsyncResult[ResultSet]): Unit = {
                        if (event.succeeded()) {
                          val rows = event.result().getRows.toList
                          if (resultClass != classOf[JsonObject]) {
                            page.objects = rows.map {
                              convertObject(_, resultClass)
                            }
                          } else {
                            page.objects = rows.asInstanceOf[List[E]]
                          }
                          conn.close()
                          p.success(Resp.success(page))
                        } else {
                          conn.close()
                          logger.warn(s"JDBC execute error : $sql [$parameters]", event.cause())
                          p.success(Resp.serverError(event.cause().getMessage))
                        }
                      }
                    }
                  )
                } else {
                  conn.close()
                  p.success(countResp)
                }
            }
          } catch {
            case ex: Throwable =>
              conn.close()
              logger.error(s"JDBC execute error : $sql [$parameters]", ex)
              p.success(Resp.serverError(ex.getMessage))
          }
        case Failure(ex) => p.success(Resp.serverUnavailable(ex.getMessage))
      }
      p.future
    }

    /**
      * 类Json信息，存储为 ： 类名 -> 要Json化的字段名列表
      */
    private val classJsonInfo = collection.mutable.Map[String, List[String]]()

    private val baseTypes = Set("String", "Int", "Long", "Boolean", "Double", "Float", "BigDecimal", "Char", "Short", "Byte")

    private[jdbc] def convertObject[E](row: JsonObject, resultClass: Class[E]): E = {
      if (!classJsonInfo.contains(resultClass.getName)) {
        classJsonInfo += resultClass.getName -> BeanHelper.findFields(resultClass).filterNot {
          field =>
            baseTypes.contains(field._2)
        }.keys.toList
      }
      classJsonInfo(resultClass.getName).foreach {
        field =>
          if (row.containsKey(field) && row.getValue(field) != null) {
            val str=row.getString(field)
            if(str.startsWith("""[""")){
              row.put(field, new JsonArray(str))
            }else {
              row.put(field, new JsonObject(str))
            }
          }
      }
      JsonHelper.toObject(row.encode(), resultClass)
    }

    /**
      * 计数
      *
      * @param sql        sql
      * @param parameters 参数
      * @return 计数结果
      */
    def count(sql: String, parameters: List[Any]): Future[Resp[Long]] = {
      val p = Promise[Resp[Long]]()
      logger.trace(s"JDBC count : $sql [$parameters]")
      db.onComplete {
        case Success(conn) =>
          try {
            conn.queryWithParams(sql,
              new JsonArray(parameters),
              new Handler[AsyncResult[ResultSet]] {
                override def handle(event: AsyncResult[ResultSet]): Unit = {
                  if (event.succeeded()) {
                    val result = Resp.success[Long](event.result().getResults.get(0).getLong(0))
                    conn.close()
                    p.success(result)
                  } else {
                    conn.close()
                    logger.warn(s"JDBC execute error : $sql [$parameters]", event.cause())
                    p.success(Resp.serverError(event.cause().getMessage))
                  }
                }
              }
            )
          } catch {
            case ex: Throwable =>
              conn.close()
              logger.error(s"JDBC execute error : $sql [$parameters]", ex)
              p.success(Resp.serverError(ex.getMessage))
          }
        case Failure(ex) => p.success(Resp.serverUnavailable(ex.getMessage))
      }
      p.future
    }

    // 此方法仅为分页请求提供
    private def countInner(sql: String, parameters: List[Any], conn: SQLConnection): Future[Resp[Long]] = {
      val p = Promise[Resp[Long]]()
      val countSql = s"SELECT COUNT(1) FROM ( $sql ) _${System.currentTimeMillis()}"
      conn.queryWithParams(countSql,
        new JsonArray(parameters),
        new Handler[AsyncResult[ResultSet]] {
          override def handle(event: AsyncResult[ResultSet]): Unit = {
            if (event.succeeded()) {
              val result = Resp.success[Long](event.result().getResults.get(0).getLong(0))
              p.success(result)
            } else {
              logger.warn(s"JDBC execute error : $sql [$parameters]", event.cause())
              p.success(Resp.serverError(event.cause().getMessage))
            }
          }
        }
      )
      p.future
    }

    /**
      * 判断是否存在
      *
      * @param sql        sql
      * @param parameters 参数
      * @return 是否存在
      */
    def exist(sql: String, parameters: List[Any]): Future[Resp[Boolean]] = {
      val p = Promise[Resp[Boolean]]()
      logger.trace(s"JDBC exist : $sql [$parameters]")
      db.onComplete {
        case Success(conn) =>
          try {
            conn.queryWithParams(sql,
              new JsonArray(parameters),
              new Handler[AsyncResult[ResultSet]] {
                override def handle(event: AsyncResult[ResultSet]): Unit = {
                  if (event.succeeded()) {
                    if (event.result().getNumRows > 0) {
                      conn.close()
                      p.success(Resp.success(true))
                    } else {
                      conn.close()
                      p.success(Resp.success(false))
                    }
                  } else {
                    conn.close()
                    logger.warn(s"JDBC execute error : $sql [$parameters]", event.cause())
                    p.success(Resp.serverError(event.cause().getMessage))
                  }
                }
              }
            )
          } catch {
            case ex: Throwable =>
              conn.close()
              logger.error(s"JDBC execute error : $sql [$parameters]", ex)
              p.success(Resp.serverError(ex.getMessage))
          }
        case Failure(ex) => p.success(Resp.serverUnavailable(ex.getMessage))
      }
      p.future
    }

    /**
      * 从连接池中获取连接
      *
      * @return 连接
      */
    private[jdbc] def db: Future[SQLConnection] = {
      val p = Promise[SQLConnection]()
      dbClient.getConnection(new Handler[AsyncResult[SQLConnection]] {
        override def handle(conn: AsyncResult[SQLConnection]): Unit = {
          if (conn.succeeded()) {
            p.success(conn.result())
          } else {
            logger.error("JDBC connecting fail .", conn.cause())
            p.failure(conn.cause())
          }
        }
      })
      p.future
    }

  }

}


