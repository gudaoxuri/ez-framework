package com.ecfront.ez.framework.service.jdbc

import java.io.BufferedReader
import java.sql._
import java.util
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.ecfront.ez.framework.service.jdbc.dialect.DialectFactory
import com.mchange.v2.c3p0.ComboPooledDataSource
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers._

import scala.collection.JavaConversions._

/**
  * JDBC 操作
  */
object JDBCProcessor extends LazyLogging {

  private var defaultProcessor: JDBCProcessor = _

  def ddl(ddl: String): Resp[Void] = {
    defaultProcessor.ddl(ddl)
  }

  def insert(sql: String, parameters: List[Any]): Resp[String] = {
    defaultProcessor.insert(sql, parameters)
  }

  def update(sql: String, parameters: List[Any]): Resp[Void] = {
    defaultProcessor.update(sql, parameters)
  }

  def batch(sql: String, parameterList: List[List[Any]]): Resp[Void] = {
    defaultProcessor.batch(sql, parameterList)
  }

  def get(sql: String, parameters: List[Any]): Resp[Map[String, Any]] = {
    defaultProcessor.get(sql, parameters)
  }

  def get[E](sql: String, parameters: List[Any], resultClass: Class[E]): Resp[E] = {
    defaultProcessor.get(sql, parameters, resultClass)
  }

  def find(sql: String, parameters: List[Any]): Resp[List[Map[String, Any]]] = {
    defaultProcessor.find(sql, parameters)
  }

  def find[E](sql: String, parameters: List[Any], resultClass: Class[E]): Resp[List[E]] = {
    defaultProcessor.find[E](sql, parameters, resultClass)
  }

  def count(sql: String, parameters: List[Any]): Resp[Long] = {
    defaultProcessor.count(sql, parameters)
  }

  def page[E](sql: String, parameters: List[Any], pageNumber: Long, pageSize: Int, resultClass: Class[E]): Resp[Page[E]] = {
    defaultProcessor.page[E](sql, parameters, pageNumber, pageSize, resultClass)
  }

  def page(sql: String, parameters: List[Any], pageNumber: Long, pageSize: Int): Resp[Page[Map[String, Any]]] = {
    defaultProcessor.page(sql, parameters, pageNumber, pageSize)
  }

  def exist(sql: String, parameters: List[Any]): Resp[Boolean] = {
    defaultProcessor.exist(sql, parameters)
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
  private val ds = new ComboPooledDataSource()
  ds.setJdbcUrl(url)
  ds.setDriverClass(dialect.getDriver)
  ds.setUser(userName)
  ds.setPassword(password)

  private val queryRunner = new QueryRunner
  private val localConnection = new ThreadLocal[Connection]
  private val localConnCounter = new ThreadLocal[AtomicLong]

  /**
    * 初始化时建立物理连接的个数
    */
  def setInitialSize(initialSize: Int): this.type = {
    ds.setInitialPoolSize(initialSize)
    this
  }

  /**
    * 最大连接池数量
    */
  def setMaxSize(maxActive: Int): this.type = {
    ds.setMaxPoolSize(maxActive)
    this
  }

  /**
    * 最小连接池数量
    */
  def setMinSize(minIdle: Int): this.type = {
    ds.setMinPoolSize(minIdle)
    this
  }

  /**
    * 获取连接时最大等待时间，单位秒。
    */
  def setMaxIdleTime(maxWait: Int): this.type = {
    ds.setMaxIdleTime(maxWait)
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
  def openTx(): Unit = {
    if (localConnCounter.get() == null) {
      localConnCounter.set(new AtomicLong(0))
      val conn = ds.getConnection
      conn.setAutoCommit(false)
      localConnection.set(conn)
    } else {
      localConnCounter.get().incrementAndGet()
    }
  }

  /**
    * 提交事务
    *
    */
  def commit(): Unit = {
    val counter = localConnCounter.get()
    if (counter == null) {
      logger.error("Transaction not open yet")
    } else {
      if (counter.get() == 0) {
        val connection = localConnection.get()
        if (connection != null && !connection.isClosed) {
          connection.commit()
          connection.close()
          localConnCounter.remove()
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
  def rollback(): Unit = {
    val counter = localConnCounter.get()
    if (counter == null) {
      logger.error("Transaction not open yet")
    } else {
      if (counter.get() == 0) {
        val connection = localConnection.get()
        if (connection != null && !connection.isClosed) {
          connection.rollback()
          connection.close()
          localConnCounter.remove()
          localConnection.remove()
        }
      } else {
        counter.decrementAndGet()
      }
    }
  }

  def tx[E](fun: => Resp[E])(implicit m: Manifest[E]): Resp[E] = {
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

  def ddl(ddl: String): Resp[Void] = {
    val (_conn, _autoClose) = getConnection
    execute[Void]("ddl", {
      queryRunner.update(_conn, ddl)
      Resp.success(null)
    }, ddl, List(), _conn, _autoClose)
  }

  def insert(sql: String, parameters: List[Any]): Resp[String] = {
    val (_conn, _autoClose) = getConnection
    val finalParameterR = formatParameters(parameters)
    if (!finalParameterR) {
      finalParameterR
    } else {
      execute[String]("insert", {
        val result =
          if (finalParameterR.body != null) {
            queryRunner.insert(_conn, sql, new ScalarHandler[Object](), finalParameterR.body.map(_.asInstanceOf[Object]): _*)
          } else {
            queryRunner.insert(_conn, sql, new ScalarHandler[Object]())
          }
        Resp.success(result.toString)
      }, sql, finalParameterR.body, _conn, _autoClose)
    }
  }

  def update(sql: String, parameters: List[Any]): Resp[Void] = {
    val (_conn, _autoClose) = getConnection
    val finalParameterR = formatParameters(parameters)
    if (!finalParameterR) {
      finalParameterR
    } else {
      execute[Void]("update", {
        if (finalParameterR.body != null) {
          queryRunner.update(_conn, sql, finalParameterR.body.map(_.asInstanceOf[Object]): _*)
        } else {
          queryRunner.update(_conn, sql)
        }
        Resp.success(null)
      }, sql, finalParameterR.body, _conn, _autoClose)
    }
  }

  def batch(sql: String, parameterList: List[List[Any]]): Resp[Void] = {
    val (_conn, _autoClose) = getConnection
    val finalParameterR = parameterList.map(formatParameters)
    val errorR = finalParameterR.find(_.code != StandardCode.SUCCESS)
    if (errorR.isDefined) {
      errorR.get
    } else {
      val finalParameters = finalParameterR.map(_.body.map(_.asInstanceOf[Object]).toArray).toArray
      execute[Void]("batch", {
        queryRunner.batch(_conn, sql, finalParameters)
        Resp.success(null)
      }, sql, finalParameterR, _conn, _autoClose)
    }
  }

  def get(sql: String, parameters: List[Any]): Resp[Map[String, Any]] = {
    val (_conn, _autoClose) = getConnection
    execute[Map[String, Any]]("get", {
      val tmpResult =
        if (parameters != null) {
          queryRunner.query[java.util.Map[String, Object]](_conn, sql, new MapHandler(), parameters.map(_.asInstanceOf[Object]): _*)
        } else {
          queryRunner.query[java.util.Map[String, Object]](_conn, sql, new MapHandler())
        }
      if (tmpResult != null) {
        val result: Map[String, Any] = tmpResult.map {
          item =>
            item._1 -> (item._2 match {
              case c: Clob => convertClob(c)
              case _ => item._2
            }).asInstanceOf[Any]
        }.toMap
        Resp.success(result)
      } else {
        Resp.success(null)
      }
    }, sql, parameters, _conn, _autoClose)
  }

  def get[E](sql: String, parameters: List[Any], resultClass: Class[E]): Resp[E] = {
    val (_conn, _autoClose) = getConnection
    execute[E]("get", {
      val result =
        if (parameters != null) {
          queryRunner.query[E](_conn, sql, new BeanHandler(resultClass), parameters.map(_.asInstanceOf[Object]): _*)
        } else {
          queryRunner.query[E](_conn, sql, new BeanHandler(resultClass))
        }
      Resp.success(result)
    }, sql, parameters, _conn, _autoClose)
  }

  def find(sql: String, parameters: List[Any]): Resp[List[Map[String, Any]]] = {
    val (_conn, _autoClose) = getConnection
    doFind(sql, parameters, _conn, _autoClose)
  }

  private def doFind(sql: String, parameters: List[Any], _conn: Connection, _autoClose: Boolean): Resp[List[Map[String, Any]]] = {
    execute[List[Map[String, Any]]]("find", {
      val tmpResult =
        if (parameters != null) {
          queryRunner.query[util.List[util.Map[String, Object]]](_conn, sql, new MapListHandler, parameters.map(_.asInstanceOf[Object]): _*)
        } else {
          queryRunner.query[util.List[util.Map[String, Object]]](_conn, sql, new MapListHandler)
        }
      if (tmpResult != null) {
        val result: List[Map[String, Any]] = tmpResult.map {
          _.map {
            item =>
              item._1 -> (item._2 match {
                case c: Clob => convertClob(c)
                case _ => item._2
              }).asInstanceOf[Any]
          }.toMap
        }.toList
        Resp.success(result)
      } else {
        Resp.success(null)
      }
    }, sql, parameters, _conn, _autoClose)
  }

  def find[E](sql: String, parameters: List[Any], resultClass: Class[E]): Resp[List[E]] = {
    val (_conn, _autoClose) = getConnection
    doFind(sql, parameters, resultClass, _conn, _autoClose)
  }

  private def doFind[E](sql: String, parameters: List[Any], resultClass: Class[E], _conn: Connection, _autoClose: Boolean): Resp[List[E]] = {
    execute[List[E]]("find", {
      val result =
        if (parameters != null) {
          queryRunner.query[util.List[E]](_conn, sql, new BeanListHandler(resultClass), parameters.map(_.asInstanceOf[Object]): _*)
        } else {
          queryRunner.query[util.List[E]](_conn, sql, new BeanListHandler(resultClass))
        }
      Resp.success(result.toList)
    }, sql, parameters, _conn, _autoClose)
  }

  def count(sql: String, parameters: List[Any]): Resp[Long] = {
    val (_conn, _autoClose) = getConnection
    doCount(sql, parameters, _conn, _autoClose)
  }

  private def doCount(sql: String, parameters: List[Any], _conn: Connection, _autoClose: Boolean): Resp[Long] = {
    val finalSql = dialect.count(sql)
    execute[Long]("count", {
      val result =
        if (parameters != null) {
          queryRunner.query[Long](_conn, finalSql, countHandler, parameters.map(_.asInstanceOf[Object]): _*)
        } else {
          queryRunner.query[Long](_conn, finalSql, countHandler)
        }
      Resp.success(result)
    }, finalSql, parameters, _conn, _autoClose)
  }

  def page[E](sql: String, parameters: List[Any], pageNumber: Long, pageSize: Int, resultClass: Class[E]): Resp[Page[E]] = {
    val (_conn, _autoClose) = getConnection
    val countR = doCount(sql, parameters, _conn, _autoClose = false)
    if (!countR) {
      countR
    } else {
      val finalSql = dialect.paging(sql, pageNumber, pageSize)
      val findR = doFind(finalSql, parameters, resultClass, _conn, _autoClose)
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

  def page(sql: String, parameters: List[Any], pageNumber: Long, pageSize: Int): Resp[Page[Map[String, Any]]] = {
    val (_conn, _autoClose) = getConnection
    val countR = doCount(sql, parameters, _conn, _autoClose = false)
    if (!countR) {
      countR
    } else {
      val finalSql = dialect.paging(sql, pageNumber, pageSize)
      val findR = doFind(finalSql, parameters, _conn, _autoClose)
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

  def exist(sql: String, parameters: List[Any]): Resp[Boolean] = {
    val (_conn, _autoClose) = getConnection
    val countR = doCount(sql, parameters, _conn, _autoClose)
    if (countR) {
      Resp.success(countR.body != 0)
    } else {
      countR
    }
  }

  private def getConnection: (Connection, Boolean) = {
    if (localConnection.get() != null) {
      (localConnection.get(), false)
    } else {
      (ds.getConnection, true)
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