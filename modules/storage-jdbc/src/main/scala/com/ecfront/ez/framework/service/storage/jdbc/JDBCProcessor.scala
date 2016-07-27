package com.ecfront.ez.framework.service.storage.jdbc

import java.util
import java.util.Date

import com.ecfront.common.{BeanHelper, JsonHelper, Resp, StandardCode}
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
    * @param conn       已存在的connection，为空时会新建
    * @return update结果
    */
  def update(sql: String, parameters: List[Any] = null, conn: SQLConnection = null): Resp[Void] = {
    Await.result(Async.update(sql, parameters, getConnection(conn)), Duration.Inf)
  }

  /**
    * 批处理
    *
    * @param sql           sql
    * @param parameterList 参数列表
    * @param conn          已存在的connection，为空时会新建
    * @return 处理结果
    */
  def batch(sql: String, parameterList: List[List[Any]] = null, conn: SQLConnection = null): Resp[Void] = {
    Await.result(Async.batch(sql, parameterList, getConnection(conn)), Duration.Inf)
  }

  /**
    * 获取一条记录
    *
    * @param sql         sql
    * @param parameters  参数
    * @param resultClass 记录类型
    * @param conn        已存在的connection，为空时会新建
    * @tparam E 记录类型
    * @return 获取到的记录
    */
  def get[E](sql: String, parameters: List[Any], resultClass: Class[E], conn: SQLConnection = null): Resp[E] = {
    Await.result(Async.get[E](sql, parameters, resultClass, getConnection(conn)), Duration.Inf)
  }

  /**
    * 查找
    *
    * @param sql         sql
    * @param parameters  参数
    * @param resultClass 记录类型
    * @param conn        已存在的connection，为空时会新建
    * @tparam E 记录类型
    * @return 获取到的记录
    */
  def find[E](sql: String, parameters: List[Any], resultClass: Class[E], conn: SQLConnection = null): Resp[List[E]] = {
    Await.result(Async.find[E](sql, parameters, resultClass, getConnection(conn)), Duration.Inf)
  }

  /**
    * 分页
    *
    * @param sql         sql
    * @param parameters  参数
    * @param pageNumber  当前页，从1开始
    * @param pageSize    每页条数
    * @param resultClass 记录类型
    * @param conn        已存在的connection，为空时会新建
    * @tparam E 记录类型
    * @return 获取到的记录
    */
  def page[E](sql: String, parameters: List[Any], pageNumber: Long, pageSize: Int, resultClass: Class[E], conn: SQLConnection = null): Resp[Page[E]] = {
    Await.result(Async.page[E](sql, parameters, pageNumber, pageSize, resultClass, getConnection(conn)), Duration.Inf)
  }

  /**
    * 计数
    *
    * @param sql        sql
    * @param parameters 参数
    * @param conn       已存在的connection，为空时会新建
    * @return 计数结果
    */
  def count(sql: String, parameters: List[Any], conn: SQLConnection = null): Resp[Long] = {
    Await.result(Async.count(sql, parameters, getConnection(conn)), Duration.Inf)
  }

  /**
    * 判断是否存在
    *
    * @param sql        sql
    * @param parameters 参数
    * @param conn       已存在的connection，为空时会新建
    * @return 是否存在
    */
  def exist(sql: String, parameters: List[Any], conn: SQLConnection = null): Resp[Boolean] = {
    Await.result(Async.exist(sql, parameters, getConnection(conn)), Duration.Inf)
  }

  /**
    * 开始事务
    *
    * @return 当前事务的连接信息
    */
  def openTx(): SQLConnection = {
    Await.result(Async.openTx(), Duration.Inf)
  }

  /**
    * 开始事务
    *
    * @return 当前事务的连接信息
    */
  def openTxByThreadLocal(): Unit = {
    while (threadLocal.get() != null) {
      logger.trace("ThreadLocal transaction already exists,wait...")
      Thread.sleep(100)
    }
    threadLocal.set(Await.result(Async.openTx(), Duration.Inf))
  }

  /**
    * 回滚事务
    *
    * @param conn 当前事务的连接信息
    */
  def rollback(conn: SQLConnection = null): Unit = {
    val connection = getConnection(conn)
    if (connection != null) {
      Await.result(Async.rollback(connection), Duration.Inf)
      connection.close()
      threadLocal.remove()
    }
  }

  /**
    * 提交事务
    *
    * @param conn 当前事务的连接信息
    */
  def commit(conn: SQLConnection = null): Unit = {
    val connection = getConnection(conn)
    if (connection != null) {
      Await.result(Async.commit(connection), Duration.Inf)
      connection.close()
      threadLocal.remove()
    }
  }

  def getConnection(conn: SQLConnection = null): SQLConnection = {
    if (conn != null) {
      conn
    } else {
      threadLocal.get()
    }
  }

  def tx[E](fun: => E)(implicit m: Manifest[E]): E = {
    openTxByThreadLocal()
    try {
      val result = fun
      commit()
      result
    } catch {
      case e: Throwable =>
        logger.error("Execute error in transaction", e)
        rollback()
        throw e
    }
  }

  private val threadLocal = new ThreadLocal[SQLConnection]

  object Async {

    /**
      * update
      *
      * @param sql        sql
      * @param parameters 参数
      * @param conn       已存在的connection，为空时会新建
      * @return update结果
      */
    def update(sql: String, parameters: List[Any] = null, conn: SQLConnection = null): Future[Resp[Void]] = {
      val p = Promise[Resp[Void]]()
      val finalParameterR = formatParameters(parameters)
      if (!finalParameterR) {
        p.success(finalParameterR)
      } else {
        if (conn != null) {
          doUpdate(sql, p, finalParameterR.body, conn, autoClose = false)
        } else {
          db.onComplete {
            case Success(connection) =>
              doUpdate(sql, p, finalParameterR.body, connection, autoClose = true)
            case Failure(ex) =>
              logger.error("service unavailable", ex)
              p.success(Resp.serverUnavailable(ex.getMessage))
          }
        }
      }
      p.future
    }

    private def doUpdate(sql: String, p: Promise[Resp[Void]], finalParameters: List[Any], conn: SQLConnection, autoClose: Boolean): Unit = {
      try {
        logger.trace(s"JDBC update : $sql [$finalParameters]")
        if (finalParameters == null) {
          conn.update(sql,
            new Handler[AsyncResult[UpdateResult]] {
              override def handle(event: AsyncResult[UpdateResult]): Unit = {
                if (autoClose) {
                  conn.close()
                }
                if (event.succeeded()) {
                  p.success(Resp.success(null))
                } else {
                  logger.warn(s"JDBC execute error : $sql [$finalParameters]", event.cause())
                  p.success(Resp.serverError(event.cause().getMessage))
                }
              }
            }
          )
        } else {
          conn.updateWithParams(sql,
            new JsonArray(finalParameters),
            new Handler[AsyncResult[UpdateResult]] {
              override def handle(event: AsyncResult[UpdateResult]): Unit = {
                if (autoClose) {
                  conn.close()
                }
                if (event.succeeded()) {
                  p.success(Resp.success(null))
                } else {
                  logger.warn(s"JDBC execute error : $sql [$finalParameters]", event.cause())
                  p.success(Resp.serverError(event.cause().getMessage))
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
    }

    /**
      * 批处理
      *
      * @param sql           sql
      * @param parameterList 参数列表
      * @param conn          已存在的connection，为空时会新建
      * @return 处理结果
      */
    def batch(sql: String, parameterList: List[List[Any]], conn: SQLConnection = null): Future[Resp[Void]] = {
      val p = Promise[Resp[Void]]()
      if (parameterList.nonEmpty) {
        if (conn != null) {
          doBatch(sql, parameterList, p, conn, autoClose = false)
        } else {
          db.onComplete {
            case Success(connection) =>
              doBatch(sql, parameterList, p, connection, autoClose = true)
            case Failure(ex) =>
              logger.error("service unavailable", ex)
              p.success(Resp.serverUnavailable(ex.getMessage))
          }
        }
      } else {
        p.success(Resp.success(null))
      }
      p.future
    }

    private def doBatch(sql: String, parameterList: List[List[Any]], p: Promise[Resp[Void]], conn: SQLConnection, autoClose: Boolean): Unit = {
      try {
        logger.trace(s"JDBC batch : $sql [$parameterList]")
        val finalParameterR = parameterList.map(formatParameters)
        val errorR = finalParameterR.find(_.code != StandardCode.SUCCESS)
        if (errorR.isDefined) {
          p.success(errorR.get)
        } else {
          conn.batchWithParams(sql, finalParameterR.map(i => new JsonArray(i.body)), new Handler[AsyncResult[util.List[Integer]]] {
            override def handle(event: AsyncResult[util.List[Integer]]): Unit = {
              if (!event.succeeded()) {
                logger.warn(s"JDBC execute error : $sql [$parameterList]", event.cause())
              }
              if (autoClose) {
                conn.close()
              }
              p.success(Resp.success(null))
            }
          })
        }
      } catch {
        case ex: Throwable =>
          conn.close()
          logger.error(s"JDBC execute error : $sql", ex)
          p.success(Resp.serverError(ex.getMessage))
      }
    }

    /**
      * 获取一条记录
      *
      * @param sql         sql
      * @param parameters  参数
      * @param resultClass 记录类型
      * @param conn        已存在的connection，为空时会新建
      * @tparam E 记录类型
      * @return 获取到的记录
      */
    def get[E](sql: String, parameters: List[Any], resultClass: Class[E], conn: SQLConnection = null): Future[Resp[E]] = {
      val p = Promise[Resp[E]]()
      if (conn != null) {
        doGet(sql, parameters, resultClass, p, conn, autoClose = false)
      } else {
        db.onComplete {
          case Success(connection) =>
            doGet(sql, parameters, resultClass, p, connection, autoClose = true)
          case Failure(ex) =>
            logger.error("service unavailable", ex)
            p.success(Resp.serverUnavailable(ex.getMessage))
        }
      }
      p.future
    }

    private def doGet[E](sql: String, parameters: List[Any], resultClass: Class[E], p: Promise[Resp[E]], conn: SQLConnection, autoClose: Boolean): Object = {
      try {
        logger.trace(s"JDBC get : $sql [$parameters]")
        conn.queryWithParams(sql,
          new JsonArray(parameters),
          new Handler[AsyncResult[ResultSet]] {
            override def handle(event: AsyncResult[ResultSet]): Unit = {
              if (event.succeeded()) {
                val row = if (event.result().getNumRows > 0) {
                  event.result().getRows.get(0)
                } else {
                  null
                }
                if (autoClose) {
                  conn.close()
                }
                try {
                  if (row != null) {
                    if (resultClass != classOf[JsonObject]) {
                      val result = Resp.success(convertObject(row, resultClass))
                      p.success(result)
                    } else {
                      val result = Resp.success(row.asInstanceOf[E])
                      p.success(result)
                    }
                  } else {
                    p.success(Resp.success(null.asInstanceOf[E]))
                  }
                } catch {
                  case e: Throwable =>
                    p.success(Resp.serverError("Data convert error :" + e.getMessage))
                    logger.error("Data convert error :" + e.getMessage, e)
                }
              } else {
                if (autoClose) {
                  conn.close()
                }
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
    }

    /**
      * 查找
      *
      * @param sql         sql
      * @param parameters  参数
      * @param resultClass 记录类型
      * @param conn        已存在的connection，为空时会新建
      * @tparam E 记录类型
      * @return 获取到的记录
      */
    def find[E](sql: String, parameters: List[Any], resultClass: Class[E], conn: SQLConnection = null): Future[Resp[List[E]]] = {
      val p = Promise[Resp[List[E]]]()
      if (conn != null) {
        doFind(sql, parameters, resultClass, p, conn, autoClose = false)
      } else {
        db.onComplete {
          case Success(connection) =>
            doFind(sql, parameters, resultClass, p, connection, autoClose = true)
          case Failure(ex) =>
            logger.error("service unavailable", ex)
            p.success(Resp.serverUnavailable(ex.getMessage))
        }
      }
      p.future
    }

    private def doFind[E](sql: String, parameters: List[Any], resultClass: Class[E],
                          p: Promise[Resp[List[E]]], conn: SQLConnection, autoClose: Boolean): Unit = {
      try {
        logger.trace(s"JDBC find : $sql [$parameters]")
        conn.queryWithParams(sql,
          new JsonArray(parameters),
          new Handler[AsyncResult[ResultSet]] {
            override def handle(event: AsyncResult[ResultSet]): Unit = {
              if (event.succeeded()) {
                val rows = event.result().getRows.toList
                if (autoClose) {
                  conn.close()
                }
                try {
                  if (resultClass != classOf[JsonObject]) {
                    val result = Resp.success(rows.map {
                      convertObject(_, resultClass)
                    })
                    p.success(result)
                  } else {
                    val result = Resp.success(rows.asInstanceOf[List[E]])
                    p.success(result)
                  }
                } catch {
                  case e: Throwable =>
                    p.success(Resp.serverError("Data convert error :" + e.getMessage))
                    logger.error("Data convert error :" + e.getMessage, e)
                }
              } else {
                if (autoClose) {
                  conn.close()
                }
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
    }

    /**
      * 分页
      *
      * @param sql         sql
      * @param parameters  参数
      * @param pageNumber  当前页，从1开始
      * @param pageSize    每页条数
      * @param resultClass 记录类型
      * @param conn        已存在的connection，为空时会新建
      * @tparam E 记录类型
      * @return 获取到的记录
      */
    def page[E](sql: String, parameters: List[Any], pageNumber: Long, pageSize: Int,
                resultClass: Class[E], conn: SQLConnection = null): Future[Resp[Page[E]]] = {
      val p = Promise[Resp[Page[E]]]()
      if (conn != null) {
        doPage(sql, parameters, pageNumber, pageSize, resultClass, p, conn, autoClose = false)
      } else {
        db.onComplete {
          case Success(connection) =>
            doPage(sql, parameters, pageNumber, pageSize, resultClass, p, connection, autoClose = true)
          case Failure(ex) =>
            logger.error("service unavailable", ex)
            p.success(Resp.serverUnavailable(ex.getMessage))
        }
      }
      p.future
    }

    private def doPage[E](sql: String, parameters: List[Any], pageNumber: Long, pageSize: Int,
                          resultClass: Class[E], p: Promise[Resp[Page[E]]], conn: SQLConnection, autoClose: Boolean): Unit = {
      try {
        count(sql, parameters, conn).onSuccess {
          case countResp =>
            if (countResp) {
              val page = new Page[E]
              page.pageNumber = pageNumber
              page.pageSize = pageSize
              page.recordTotal = countResp.body
              page.pageTotal = (page.recordTotal + pageSize - 1) / pageSize
              val limitSql = s"$sql limit ${(pageNumber - 1) * pageSize} ,$pageSize"
              logger.trace(s"JDBC page : $limitSql [$parameters]")
              conn.queryWithParams(limitSql,
                new JsonArray(parameters),
                new Handler[AsyncResult[ResultSet]] {
                  override def handle(event: AsyncResult[ResultSet]): Unit = {
                    if (event.succeeded()) {
                      val rows = event.result().getRows.toList
                      if (autoClose) {
                        conn.close()
                      }
                      try {
                        if (resultClass != classOf[JsonObject]) {
                          page.objects = rows.map {
                            convertObject(_, resultClass)
                          }
                        } else {
                          page.objects = rows.asInstanceOf[List[E]]
                        }
                        p.success(Resp.success(page))
                      } catch {
                        case e: Throwable =>
                          p.success(Resp.serverError("Data convert error :" + e.getMessage))
                          logger.error("Data convert error :" + e.getMessage, e)
                      }
                    } else {
                      if (autoClose) {
                        conn.close()
                      }
                      logger.warn(s"JDBC execute error : $sql [$parameters]", event.cause())
                      p.success(Resp.serverError(event.cause().getMessage))
                    }
                  }
                }
              )
            } else {
              if (autoClose) {
                conn.close()
              }
              p.success(countResp)
            }
        }
      } catch {
        case ex: Throwable =>
          conn.close()
          logger.error(s"JDBC execute error : $sql [$parameters]", ex)
          p.success(Resp.serverError(ex.getMessage))
      }
    }

    /**
      * 计数
      *
      * @param sql        sql
      * @param parameters 参数
      * @param conn       已存在的connection，为空时会新建
      * @return 计数结果
      */
    def count(sql: String, parameters: List[Any], conn: SQLConnection = null): Future[Resp[Long]] = {
      val p = Promise[Resp[Long]]()
      if (conn != null) {
        doCount(sql, parameters, p, conn, autoClose = false)
      } else {
        db.onComplete {
          case Success(connection) =>
            doCount(sql, parameters, p, connection, autoClose = true)
          case Failure(ex) =>
            logger.error("service unavailable", ex)
            p.success(Resp.serverUnavailable(ex.getMessage))
        }
      }
      p.future
    }

    private def doCount(sql: String, parameters: List[Any], p: Promise[Resp[Long]], conn: SQLConnection, autoClose: Boolean): Unit = {
      try {
        val countSql = s"SELECT COUNT(1) FROM ( $sql ) _tmp"
        logger.trace(s"JDBC count : $countSql [$parameters]")
        conn.queryWithParams(countSql,
          new JsonArray(parameters),
          new Handler[AsyncResult[ResultSet]] {
            override def handle(event: AsyncResult[ResultSet]): Unit = {
              if (event.succeeded()) {
                if (autoClose) {
                  conn.close()
                }
                try {
                  val result = Resp.success[Long](event.result().getResults.get(0).getLong(0))
                  p.success(result)
                } catch {
                  case e: Throwable =>
                    p.success(Resp.serverError("Data convert error :" + e.getMessage))
                    logger.error("Data convert error :" + e.getMessage, e)
                }
              } else {
                if (autoClose) {
                  conn.close()
                }
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
    }

    /**
      * 判断是否存在
      *
      * @param sql        sql
      * @param parameters 参数
      * @param conn       已存在的connection，为空时会新建
      * @return 是否存在
      */
    def exist(sql: String, parameters: List[Any], conn: SQLConnection = null): Future[Resp[Boolean]] = {
      val p = Promise[Resp[Boolean]]()
      if (conn != null) {
        doExist(sql, parameters, p, conn, autoClose = false)
      } else {
        db.onComplete {
          case Success(connection) =>
            doExist(sql, parameters, p, connection, autoClose = true)
          case Failure(ex) =>
            logger.error("service unavailable", ex)
            p.success(Resp.serverUnavailable(ex.getMessage))
        }
      }
      p.future
    }

    private def doExist(sql: String, parameters: List[Any], p: Promise[Resp[Boolean]], conn: SQLConnection, autoClose: Boolean): Unit = {
      try {
        logger.trace(s"JDBC exist : $sql [$parameters]")
        conn.queryWithParams(sql,
          new JsonArray(parameters),
          new Handler[AsyncResult[ResultSet]] {
            override def handle(event: AsyncResult[ResultSet]): Unit = {
              if (event.succeeded()) {
                if (event.result().getNumRows > 0) {
                  if (autoClose) {
                    conn.close()
                  }
                  p.success(Resp.success(true))
                } else {
                  if (autoClose) {
                    conn.close()
                  }
                  p.success(Resp.success(false))
                }
              } else {
                if (autoClose) {
                  conn.close()
                }
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
    }

    /**
      * 开始事务
      *
      * @return 当前事务的连接信息
      */
    def openTx(): Future[SQLConnection] = {
      val p = Promise[SQLConnection]()
      db.onSuccess {
        case conn =>
          conn.setAutoCommit(false, new Handler[AsyncResult[Void]] {
            override def handle(event: AsyncResult[Void]): Unit = {
              if (event.failed()) {
                conn.close()
                logger.error("JDBC connecting fail .", event.cause())
                p.failure(event.cause())
              } else {
                p.success(conn)
              }
            }
          })
      }
      p.future
    }

    /**
      * 回滚事务
      *
      * @param conn 当前事务的连接信息
      * @return 是否成功
      */
    def rollback(conn: SQLConnection): Future[Void] = {
      val p = Promise[Void]()
      conn.rollback(new Handler[AsyncResult[Void]] {
        override def handle(event: AsyncResult[Void]): Unit = {
          conn.close()
          if (event.failed()) {
            logger.error("JDBC rollback fail .", event.cause())
            p.failure(event.cause())
          } else {
            p.success(null)
          }
        }
      })
      p.future
    }

    /**
      * 提交事务
      *
      * @param conn 当前事务的连接信息
      * @return 是否成功
      */
    def commit(conn: SQLConnection): Future[Void] = {
      val p = Promise[Void]()
      conn.commit(new Handler[AsyncResult[Void]] {
        override def handle(event: AsyncResult[Void]): Unit = {
          conn.close()
          if (event.failed()) {
            logger.error("JDBC commit fail .", event.cause())
            p.failure(event.cause())
          } else {
            p.success(null)
          }
        }
      })
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

    private[jdbc] def formatParameters(parameters: List[Any]): Resp[List[Any]] = {
      if (parameters == null) {
        Resp.success(null)
      } else {
        try {
          val result = parameters.map {
            case p if
            p.isInstanceOf[String] || p.isInstanceOf[Int] || p.isInstanceOf[Long] || p.isInstanceOf[Boolean] ||
              p.isInstanceOf[Double] || p.isInstanceOf[Float] ||
              p.isInstanceOf[Char] || p.isInstanceOf[Short] || p.isInstanceOf[Byte] || p.isInstanceOf[Date]
            => p
            case p =>
              JsonHelper.toJsonString(p)
          }
          Resp.success(result)
        } catch {
          case e: Throwable =>
            logger.error("Data format error : " + e.getMessage, e)
            Resp.serverError("Data format error : " + e.getMessage)
        }
      }
    }

    /**
      * 类Json信息，存储为 ： 类名 -> 要Json化的字段名列表
      */
    private val classJsonInfo = collection.mutable.Map[String, List[String]]()

    private val baseTypes = Set("String", "Int", "Long", "Boolean", "Double", "Float", "BigDecimal", "Char", "Short", "Byte", "java.util.Date")

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
            val str = row.getString(field)
            if (str.startsWith("""[""")) {
              row.put(field, new JsonArray(str))
            } else {
              row.put(field, new JsonObject(str))
            }
          }
      }
      JsonHelper.toObject(row.encode(), resultClass)
    }

  }

}


