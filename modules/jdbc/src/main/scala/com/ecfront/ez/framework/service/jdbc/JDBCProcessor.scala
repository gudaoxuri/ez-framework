package com.ecfront.ez.framework.service.jdbc

import java.io.BufferedReader
import java.sql._
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

import com.alibaba.druid.pool.DruidDataSource
import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.service.jdbc.dialect.DialectFactory
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers._

import scala.collection.JavaConversions._

/**
  * JDBC 操作
  */
object JDBCProcessor extends LazyLogging {

  private var defaultProcessor: JDBCProcessor = _

  def update(sql: String, parameters: List[Any], conn: Connection = null, autoClose: Boolean = true): Resp[Void] = {
    defaultProcessor.update(sql, parameters, conn, autoClose)
  }

  def batch(sql: String, parameterList: List[List[Any]], conn: Connection = null, autoClose: Boolean = true): Resp[Void] = {
    defaultProcessor.batch(sql, parameterList, conn, autoClose)
  }

  def getMap(sql: String, parameters: List[Any], conn: Connection = null, autoClose: Boolean = true): Resp[Map[String, Any]] = {
    defaultProcessor.getMap(sql, parameters, conn, autoClose)
  }

  def get[E](sql: String, parameters: List[Any], resultClass: Class[E], conn: Connection = null, autoClose: Boolean = true): Resp[E] = {
    defaultProcessor.get(sql, parameters, resultClass, conn, autoClose)
  }

  def findMap(sql: String, parameters: List[Any], conn: Connection = null, autoClose: Boolean = true): Resp[List[Map[String, Any]]] = {
    defaultProcessor.findMap(sql, parameters, conn, autoClose)
  }

  def find[E](sql: String, parameters: List[Any], resultClass: Class[E], conn: Connection = null, autoClose: Boolean = true): Resp[List[E]] = {
    defaultProcessor.find[E](sql, parameters, resultClass, conn, autoClose)
  }

  def count(sql: String, parameters: List[Any], conn: Connection = null, autoClose: Boolean = true): Resp[Long] = {
    defaultProcessor.count(sql, parameters, conn, autoClose)
  }

  def page[E](sql: String, parameters: List[Any], pageNumber: Long, pageSize: Int,
              resultClass: Class[E], conn: Connection = null, autoClose: Boolean = true): Resp[Page[E]] = {
    defaultProcessor.page[E](sql, parameters, pageNumber, pageSize, resultClass, conn, autoClose)
  }

  def pageMap(sql: String, parameters: List[Any], pageNumber: Long, pageSize: Int,
              conn: Connection = null, autoClose: Boolean = true): Resp[Page[Map[String, Any]]] = {
    defaultProcessor.pageMap(sql, parameters, pageNumber, pageSize, conn, autoClose)
  }

  def exist(sql: String, parameters: List[Any], conn: Connection = null, autoClose: Boolean = true): Resp[Boolean] = {
    defaultProcessor.exist(sql, parameters, conn, autoClose)
  }

  /**
    * 开始事务
    *
    * @return 当前事务的连接信息
    */
  def openTx(): Unit = {
    defaultProcessor.openTx()
  }

  /**
    * 提交事务
    *
    */
  def commit(): Unit = {
    defaultProcessor.commit()
  }

  /**
    * 回滚事务
    *
    */
  def rollback(): Unit = {
    defaultProcessor.rollback()
  }

  def tx[E](fun: => Resp[E])(implicit m: Manifest[E]): Resp[E] = {
    defaultProcessor.tx[E](fun)(m)
  }

  private val ds = collection.mutable.Map[String, JDBCProcessor]()

  def initDS(processor: JDBCProcessor): Resp[Void] = {
    defaultProcessor = processor
    Resp.success(null)
  }

  def addDS(code: String, processor: JDBCProcessor): Unit = {
    ds += code -> processor
  }

  def removeDS(code: String): Unit = {
    ds(code).close()
    ds -= code
  }

  def close(): Unit = {
    defaultProcessor.close()
    ds.foreach(_._2.close())
    ds.empty
  }

}


case class JDBCProcessor(url: String, userName: String, password: String) extends LazyLogging {

  logger.info(s"Load JDBC client : $url")

  private val dialect = DialectFactory.parseDialect(url)
  private val ds = new DruidDataSource()
  ds.setUrl(url)
  ds.setDriverClassName(dialect.getDriver)
  ds.setUsername(userName)
  ds.setPassword(password)

  private val queryRunner = new QueryRunner
  private val localConnection = new ThreadLocal[Connection]
  private val localConnCounter = new ThreadLocal[AtomicLong]

  def setInitialSize(initialSize: Int): this.type = {
    ds.setInitialSize(initialSize)
    this
  }

  def setMaxActive(maxActive: Int): this.type = {
    ds.setMaxActive(maxActive)
    this
  }

  def setMinIdle(minIdle: Int): this.type = {
    ds.setMinIdle(minIdle)
    this
  }

  def setMaxWait(maxWait: Int): this.type = {
    ds.setMaxWait(maxWait)
    this
  }

  private[jdbc] def close(): Unit = {
    ds.close()
  }

  /**
    * 开始事务
    *
    * @return 当前事务的连接信息
    */
  private def openTx(): Unit = {
    if (localConnCounter.get() == null) {
      localConnCounter.set(new AtomicLong(0))
      localConnection.set(ds.getConnection)
    }
    localConnCounter.get().incrementAndGet()
  }

  /**
    * 提交事务
    *
    */
  private def commit(): Unit = {
    val counter = localConnCounter.get()
    if (counter == null) {
      logger.error("Transaction not open yet")
    } else {
      if (counter.get() == 0) {
        val connection = localConnection.get()
        if (connection != null && !connection.isClosed) {
          connection.commit()
          localConnection.remove()
        }
      } else {
        counter.decrementAndGet()
      }
    }
  }

  /**
    * 回滚事务
    *
    */
  private def rollback(): Unit = {
    val connection = localConnection.get()
    if (connection != null && !connection.isClosed) {
      connection.rollback()
      localConnection.remove()
      localConnCounter.get().set(0)
    }
  }

  private def tx[E](fun: => Resp[E])(implicit m: Manifest[E]): Resp[E] = {
    openTx()
    try {
      val result = fun
      if (result) {
        commit()
      } else {
        logger.warn(s"Execute error in transaction:[${result.code}] ${result.message}")
        rollback()
      }
      result
    } catch {
      case e: Throwable =>
        logger.warn("Execute error in transaction", e)
        rollback()
        throw e
    }
  }

  private def update(sql: String, parameters: List[Any], conn: Connection = null, autoClose: Boolean = true): Resp[Void] = {
    val (_conn, _autoClose) = getConnection(conn, autoClose)
    val finalParameterR = formatParameters(parameters)
    if (!finalParameterR) {
      finalParameterR
    } else {
      execute[Void]("update", {
        if (finalParameterR.body != null) {
          queryRunner.update(_conn, sql, finalParameterR.body)
        } else {
          queryRunner.update(_conn, sql)
        }
        Resp.success(null)
      }, sql, finalParameterR.body, _conn, _autoClose)
    }
  }

  private def batch(sql: String, parameterList: List[List[Any]], conn: Connection = null, autoClose: Boolean = true): Resp[Void] = {
    val (_conn, _autoClose) = getConnection(conn, autoClose)
    val finalParameterR = parameterList.map(formatParameters)
    val errorR = finalParameterR.find(_.code != StandardCode.SUCCESS)
    if (errorR.isDefined) {
      errorR.get
    } else {
      val finalParameters = finalParameterR.map(_.body)
      logger.trace(s"JDBC batch : $sql [$finalParameters]")
      try {
        finalParameters.foreach {
          parameters =>
            if (parameters != null) {
              queryRunner.update(_conn, sql, parameters)
            } else {
              queryRunner.update(_conn, sql)
            }
        }
        if (_autoClose && !_conn.isClosed) {
          _conn.close()
        }
        Resp.success(null)
      } catch {
        case e: Throwable =>
          _conn.rollback()
          if (!_conn.isClosed) {
            _conn.close()
          }
          logger.error(s"JDBC batch error : $sql [$finalParameters]", e)
          Resp.serverError(s"JDBC batch error : $sql [$finalParameters] ${e.getMessage}")
      }
    }
  }

  private def getMap(sql: String, parameters: List[Any], conn: Connection = null, autoClose: Boolean = true): Resp[Map[String, Any]] = {
    val (_conn, _autoClose) = getConnection(conn, autoClose)
    execute[Map[String, Any]]("get", {
      val tmpResult =
        if (parameters != null) {
          queryRunner.query[java.util.Map[String, Object]](_conn, sql, new MapHandler(), parameters)
        } else {
          queryRunner.query[java.util.Map[String, Object]](_conn, sql, new MapHandler())
        }
      if (tmpResult != null) {
        val result = tmpResult.map {
          item =>
            item._1 -> (item._2 match {
              case c: Clob => convertClob(c)
              case _ => item._2
            })
        }
        Resp.success(result)
      } else {
        Resp.success(null)
      }
    }, sql, parameters, _conn, _autoClose)
  }

  private def get[E](sql: String, parameters: List[Any], resultClass: Class[E], conn: Connection = null, autoClose: Boolean = true): Resp[E] = {
    val (_conn, _autoClose) = getConnection(conn, autoClose)
    execute[E]("get", {
      val result =
        if (parameters != null) {
          queryRunner.query[E](_conn, sql, new BeanHandler(resultClass), parameters)
        } else {
          queryRunner.query[E](_conn, sql, new BeanHandler(resultClass))
        }
      Resp.success(result)
    }, sql, parameters, _conn, _autoClose)
  }

  private def findMap(sql: String, parameters: List[Any], conn: Connection = null, autoClose: Boolean = true): Resp[List[Map[String, Any]]] = {
    val (_conn, _autoClose) = getConnection(conn, autoClose)
    execute[List[Map[String, Any]]]("find", {
      val tmpResult =
        if (parameters != null) {
          queryRunner.query[java.util.List[java.util.Map[String, Object]]](_conn, sql, new MapListHandler, parameters)
        } else {
          queryRunner.query[java.util.List[java.util.Map[String, Object]]](_conn, sql, new MapListHandler)
        }
      if (tmpResult != null) {
        val result = tmpResult.map {
          _.map {
            item =>
              item._1 -> (item._2 match {
                case c: Clob => convertClob(c)
                case _ => item._2
              })
          }
        }
        Resp.success(result)
      } else {
        Resp.success(null)
      }
    }, sql, parameters, _conn, _autoClose)
  }

  private def find[E](sql: String, parameters: List[Any], resultClass: Class[E], conn: Connection = null, autoClose: Boolean = true): Resp[List[E]] = {
    val (_conn, _autoClose) = getConnection(conn, autoClose)
    execute[E]("find", {
      val result =
        if (parameters != null) {
          queryRunner.query[java.util.List[E]](_conn, sql, new BeanListHandler(resultClass), parameters)
        } else {
          queryRunner.query[java.util.List[E]](_conn, sql, new BeanListHandler(resultClass))
        }
      Resp.success(result)
    }, sql, parameters, _conn, _autoClose)
  }

  private def count(sql: String, parameters: List[Any], conn: Connection = null, autoClose: Boolean = true): Resp[Long] = {
    val (_conn, _autoClose) = getConnection(conn, autoClose)
    val finalSql = dialect.count(sql)
    execute[Long]("count", {
      val result =
        if (parameters != null) {
          queryRunner.query[Long](_conn, finalSql, countHandler, parameters)
        } else {
          queryRunner.query[Long](_conn, finalSql, countHandler)
        }
      Resp.success(result)
    }, finalSql, parameters, _conn, _autoClose)
  }

  private def page[E](sql: String, parameters: List[Any], pageNumber: Long, pageSize: Int,
                      resultClass: Class[E], conn: Connection = null, autoClose: Boolean = true): Resp[Page[E]] = {
    val (_conn, _autoClose) = getConnection(conn, autoClose)
    val countR = count(sql, parameters, _conn, autoClose = false)
    if (!countR) {
      countR
    } else {
      val finalSql = dialect.paging(sql, pageNumber, pageSize)
      val findR = find(finalSql, parameters, resultClass, _conn, _autoClose)
      if (!findR) {
        findR
      } else {
        val page = new Page[E]
        page.pageNumber = pageNumber
        page.pageSize = pageSize
        page.recordTotal = countR.body
        page.pageTotal = (page.recordTotal + pageSize - 1) / pageSize
        page.objects = findR.body
        Resp.success(page)
      }
    }
  }

  private def pageMap(sql: String, parameters: List[Any], pageNumber: Long, pageSize: Int,
                      conn: Connection = null, autoClose: Boolean = true): Resp[Page[Map[String, Any]]] = {
    val (_conn, _autoClose) = getConnection(conn, autoClose)
    val countR = count(sql, parameters, _conn, autoClose = false)
    if (!countR) {
      countR
    } else {
      val finalSql = dialect.paging(sql, pageNumber, pageSize)
      val findR = findMap(finalSql, parameters, _conn, _autoClose)
      if (!findR) {
        findR
      } else {
        val page = new Page[Map[String, Any]]
        page.pageNumber = pageNumber
        page.pageSize = pageSize
        page.recordTotal = countR.body
        page.pageTotal = (page.recordTotal + pageSize - 1) / pageSize
        page.objects = findR.body
        Resp.success(page)
      }
    }
  }

  private def exist(sql: String, parameters: List[Any], conn: Connection = null, autoClose: Boolean = true): Resp[Boolean] = {
    val (_conn, _autoClose) = getConnection(conn, autoClose)
    val countR = count(sql, parameters, _conn, _autoClose)
    if (countR) {
      Resp.success(countR.body)
    } else {
      countR
    }
  }

  private def getConnection(conn: Connection, autoClose: Boolean): (Connection, Boolean) = {
    if (conn == null) {
      if (localConnection.get() != null) {
        (localConnection.get(), false)
      } else {
        (ds.getConnection, true)
      }
    } else {
      (conn, autoClose)
    }
  }

  private def execute[E](funName: String, fun: => Resp[E], sql: String, parameters: List[Any], _conn: Connection, _autoClose: Boolean): Resp[E] = {
    logger.trace(s"JDBC $funName : $sql [$parameters]")
    try {
      val resultR = fun
      if (resultR) {
        if (_autoClose && !_conn.isClosed) {
          _conn.close()
        }
      } else {
        _conn.rollback()
        if (!_conn.isClosed) {
          _conn.close()
        }
      }
      resultR
    } catch {
      case e: Throwable =>
        _conn.rollback()
        if (!_conn.isClosed) {
          _conn.close()
        }
        logger.error(s"JDBC $funName error : $sql [$parameters]", e)
        Resp.serverError(s"JDBC $funName error : $sql [$parameters] ${e.getMessage}")
    }
  }

  private def formatParameters(parameters: List[Any]): Resp[List[Any]] = {
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

  private def convertClob(clob: Clob): String = {
    val value: StringBuilder = new StringBuilder
    var line: String = null
    if (clob != null) {
      val reader = clob.getCharacterStream
      val br = new BufferedReader(reader)
      while ((line = br.readLine) != null) value.append(line).append("\r\n")
    }
    if (value.length >= 2) {
      value.substring(0, value.length - 2)
    } else {
      ""
    }
  }

  private val countHandler = new ScalarHandler[Long]() {
    override def handle(rs: ResultSet): Long = {
      val obj: Any = super.handle(rs)
      obj match {
        case decimal: BigDecimal => decimal.longValue
        case _: Long => obj.asInstanceOf[Long]
        case _ => obj.asInstanceOf[Number].longValue
      }
    }
  }

}