package com.ecfront.ez.framework.service.jdbc

import java.util.Date

import com.ecfront.common.{Resp, StandardCode}
import com.ecfront.ez.framework.core.helper.TimeHelper
import com.ecfront.ez.framework.test.MockStartupSpec

import scala.beans.BeanProperty

class JDBCSpec extends MockStartupSpec {

  test("JDBC Test") {
    baseTest()
    entityTest()
    intTest()
    txTest()
    testSP()
  }

  def baseTest(): Unit = {
    /* JDBCProcessor.ddl(
       """
         |CREATE TABLE IF NOT EXISTS test_entity
         |(
         | id INT NOT NULL AUTO_INCREMENT ,
         | name varchar(100) NOT NULL ,
         | age INT NOT NULL ,
         | time timestamp NOT NULL default CURRENT_TIMESTAMP,
         | time_auto_create timestamp NOT NULL default now(),
         | time_auto_update timestamp NOT NULL default now(),
         | create_user varchar(100) NOT NULL COMMENT '创建用户' ,
         | create_org varchar(100) NOT NULL COMMENT '创建组织' ,
         | create_time BIGINT NOT NULL COMMENT '创建时间(yyyyMMddHHmmssSSS)' ,
         | update_user varchar(100) NOT NULL COMMENT '更新用户' ,
         | update_org varchar(100) NOT NULL COMMENT '更新组织' ,
         | update_time BIGINT NOT NULL COMMENT '更新时间(yyyyMMddHHmmssSSS)' ,
         | enable BOOLEAN NOT NULL COMMENT '是否启用' ,
         | PRIMARY KEY(id)
         |)ENGINE=innodb DEFAULT CHARSET=utf8
       """.stripMargin)*/

    JDBC_Test_Entity.deleteByCond("", List()).body

    JDBCProcessor.ddl(
      s"""TRUNCATE test_entity""".stripMargin)

    val id = JDBCProcessor.insert(
      s"""
         |insert into test_entity
         |  (name,age,time_auto_create,time_auto_update,create_user,create_org,create_time,update_user,update_org,update_time,enable)
         |  values
         |  (?,?,now(),now(),?,?,?,?,?,?,?)
       """.stripMargin
      , List("张三", 11, "admin", "", TimeHelper.msf.format(new Date()), "admin", "", TimeHelper.msf.format(new Date()), true)).body
    JDBCProcessor.update(
      s"""
         |update test_entity set name = ? where id = ?
       """.stripMargin, List("李四", id))
    JDBCProcessor.batch(
      s"""
         |insert into test_entity
         |  (name,age,time_auto_create,time_auto_update,create_user,create_org,create_time,update_user,update_org,update_time,enable)
         |  values
         |  (?,?,now(),now(),?,?,?,?,?,?,?)
       """.stripMargin
      , List(
        List("测试1", 11, "admin", "", TimeHelper.msf.format(new Date()), "admin", "", TimeHelper.msf.format(new Date()), true),
        List("测试2", 11, "admin", "", TimeHelper.msf.format(new Date()), "admin", "", TimeHelper.msf.format(new Date()), true),
        List("测试3", 11, "admin", "", TimeHelper.msf.format(new Date()), "admin", "", TimeHelper.msf.format(new Date()), false)
      )).body
    assert(JDBCProcessor.exist(s"""select * from test_entity where enable = ? order by time_auto_create desc""", List(1)).body)
    assert(JDBCProcessor.count(s"""select * from test_entity where enable = ? order by time_auto_create desc""", List(true)).body == 3)

    val getMap = JDBCProcessor.get(s"""select * from test_entity where id = ?""", List(id)).body
    assert(getMap("name") == "李四" && getMap("enable") == true)
    val findMap = JDBCProcessor.find(s"""select * from test_entity where enable = ?""", List(true)).body
    assert(findMap.length == 3 && findMap.head("name") == "李四")
    val pageMap = JDBCProcessor.page(s"""select * from test_entity where enable = ?""", List(true), 2, 1).body
    assert(pageMap.recordTotal == 3 && pageMap.pageNumber == 2 && pageMap.pageSize == 1 && pageMap.pageTotal == 3
      && pageMap.objects.length == 1 && pageMap.objects.head("name") == "测试1")

    val get = JDBCProcessor.get(s"""select * from test_entity where id = ?""", List(id), classOf[JDBC_Test_Entity]).body
    assert(get.name == "李四" && get.enable)
    val find = JDBCProcessor.find(s"""select * from test_entity where enable = ?""", List(true), classOf[JDBC_Test_Entity]).body
    assert(find.length == 3 && find.head.name == "李四")
    val page = JDBCProcessor.page(s"""select * from test_entity where enable = ?""", List(true), 2, 1, classOf[JDBC_Test_Entity]).body
    assert(page.recordTotal == 3 && page.pageNumber == 2 && page.pageSize == 1 && page.pageTotal == 3
      && page.objects.length == 1 && page.objects.head.name == "测试1")
  }

  def entityTest(): Unit = {
    JDBC_Test_Entity.deleteByCond("", List()).body
    val jdbc = JDBC_Test_Entity()
    jdbc.age = 10
    assert(JDBC_Test_Entity.save(jdbc).code == StandardCode.BAD_REQUEST)
    jdbc.name = "name1"
    var getResult = JDBC_Test_Entity.save(jdbc).body
    assert(getResult.name == "name1")
    getResult.name = "name_new"
    getResult = JDBC_Test_Entity.update(getResult).body
    assert(getResult.name == "name_new")
    assert(getResult.age == 10)
    JDBC_Test_Entity.deleteById(getResult.id).body
    getResult = JDBC_Test_Entity.getById(getResult.id).body
    assert(getResult == null)
    jdbc.name = "name_new_2"
    getResult = JDBC_Test_Entity.saveOrUpdate(jdbc).body
    getResult.name = "name_new_3"
    getResult = JDBC_Test_Entity.saveOrUpdate(getResult).body
    assert(getResult.name == "name_new_3")
    JDBC_Test_Entity.deleteById(getResult.id).body

    jdbc.id = "0"
    jdbc.name = "n1"
    jdbc.age = 10
    JDBC_Test_Entity.save(jdbc).body
    Thread.sleep(1)
    jdbc.name = "n2"
    jdbc.create_time = 0
    JDBC_Test_Entity.save(jdbc).body
    Thread.sleep(1)
    jdbc.name = "n3"
    jdbc.create_time = 0
    JDBC_Test_Entity.save(jdbc).body
    Thread.sleep(1)
    jdbc.id = "200"
    jdbc.name = "n4"
    jdbc.create_time = 0
    JDBC_Test_Entity.save(jdbc).body
    Thread.sleep(1)
    jdbc.id = "300"
    jdbc.name = "n4"
    jdbc.create_time = 0
    assert(JDBC_Test_Entity.save(jdbc).message.contains("姓名"))
    assert(JDBC_Test_Entity.getById("300").body == null)
    getResult = JDBC_Test_Entity.getById("200").body
    getResult.name = "n2"
    assert(JDBC_Test_Entity.update(getResult).message.contains("姓名"))

    JDBC_Test_Entity.updateByCond("name =?", "id =?", List("m4", "200"))

    var findResult = JDBC_Test_Entity.find(" 1=1 ORDER BY create_time ASC").body
    assert(findResult.size == 4)
    assert(4 == JDBC_Test_Entity.count("").body)
    assert(findResult.head.name == "n1")
    findResult = JDBC_Test_Entity.find("name like ? ORDER BY create_time DESC", List("n%")).body
    assert(findResult.size == 3)
    assert(findResult.head.name == "n3")

    var pageResult = JDBC_Test_Entity.page(" 1=1 ORDER BY create_time ASC").body
    assert(pageResult.pageNumber == 1)
    assert(pageResult.pageSize == 10)
    assert(pageResult.recordTotal == 4)
    assert(pageResult.objects.size == 4)
    assert(pageResult.objects.head.name == "n1")
    pageResult = JDBC_Test_Entity.page("name like ? ORDER BY create_time DESC", List("n%"), 2, 2).body
    assert(pageResult.pageNumber == 2)
    assert(pageResult.pageSize == 2)
    assert(pageResult.recordTotal == 3)
    assert(pageResult.objects.size == 1)
    assert(pageResult.objects.head.name == "n1")

    getResult = JDBC_Test_Entity.getById(200).body
    assert(getResult != null)
    JDBC_Test_Entity.deleteByCond("name =?", List("m4")).body
    getResult = JDBC_Test_Entity.getById(200).body
    assert(getResult == null)
    JDBC_Test_Entity.deleteByCond("1=1", List()).body
    findResult = JDBC_Test_Entity.find("1=1").body
    assert(findResult.isEmpty)

    val app = new Loan_app
    app.deposit_payment_amount = 1.1
    Loan_app.save(app)
    assert(Loan_app.find("").body.nonEmpty)
  }

  def intTest(): Unit = {
    /* JDBCProcessor.ddl(
       """
         |CREATE TABLE IF NOT EXISTS jdbc_test_int
         |(
         | id INT NOT NULL AUTO_INCREMENT ,
         | status INT(1) NOT NULL ,
         | enabled INT(1) NOT NULL ,
         | gender INT(1),
         | age INT ,
         | PRIMARY KEY(id)
         |)ENGINE=innodb DEFAULT CHARSET=utf8
       """.stripMargin
     )*/
    JDBCProcessor.ddl(s"TRUNCATE test_entity")

    var obj = JDBC_Test_Int()
    obj.status = 1
    obj.enabled = true
    obj.gender = 1
    obj.age = 11
    obj = JDBC_Test_Int.save(obj).body
    assert(obj.status == 1 && obj.enabled && obj.gender == 1 && obj.age == 11)
    obj.status = 0
    obj.enabled = false
    obj.gender = 0
    obj.age = 11
    obj = JDBC_Test_Int.update(obj).body
    assert(obj.status == 0 && !obj.enabled && obj.gender == 0 && obj.age == 11)
    obj.status = 8
    obj.enabled = true
    obj.gender = -1
    obj.age = 11
    obj = JDBC_Test_Int.update(obj).body
    assert(obj.status == 8 && obj.enabled && obj.gender == -1 && obj.age == 11)
  }

  def txTest(): Unit = {
    JDBCProcessor.ddl(
      """
        |CREATE TABLE IF NOT EXISTS tx_test
        |(
        | id INT NOT NULL AUTO_INCREMENT ,
        | name varchar(100) NOT NULL ,
        | age INT NOT NULL ,
        | PRIMARY KEY(id)
        |)ENGINE=innodb DEFAULT CHARSET=utf8
      """.stripMargin
    )
    JDBCProcessor.ddl(s"TRUNCATE tx_test")

    JDBCProcessor.openTx()
    JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("张三", 23))
    JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("李四", 111))
    JDBCProcessor.rollback()
    assert(!JDBCProcessor.exist("SELECT * FROM  tx_test WHERE name =?", List("张三")).body)

    JDBCProcessor.openTx()
    JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("张三", 23))
    JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("李四", 111))
    JDBCProcessor.commit()
    assert(JDBCProcessor.exist("SELECT * FROM  tx_test WHERE name =?", List("张三")).body)
    JDBCProcessor.ddl(s"TRUNCATE tx_test")

    testTxFail()
    assert(!JDBCProcessor.exist("SELECT * FROM  tx_test WHERE name =?", List("张三")).body)

    JDBCProcessor.tx {
      JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("张三", 23))
      JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("李四", 111))
      test3()
    }
    assert(JDBCProcessor.exist("SELECT * FROM  tx_test WHERE name =?", List("王五")).body)

    // 事务重入
    JDBCProcessor.tx {
      JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("A", 1))
      JDBCProcessor.tx {
        JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("B", 1))
        JDBCProcessor.tx {
          JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("C", 1))
        }
      }
    }
    assert(JDBCProcessor.count("SELECT * FROM  tx_test WHERE name in (?,?,?)", List("A", "B", "C")).body == 3)
    JDBCProcessor.ddl(s"TRUNCATE tx_test")

    JDBCProcessor.tx {
      JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("A", 1))
      JDBCProcessor.tx[Void] {
        JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("B", 1))
        JDBCProcessor.tx[Void] {
          JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("C", 1))
          Resp.badRequest("")
        }
        Resp.badRequest("")
      }
    }
    assert(JDBCProcessor.count("SELECT * FROM  tx_test WHERE name in (?,?,?)", List("A", "B", "C")).body == 0)

    JDBCProcessor.tx {
      JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("A", 1))
      JDBCProcessor.tx[Void] {
        JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("B", 1))
        JDBCProcessor.tx {
          JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("C", 1))
        }
        Resp.badRequest("")
      }
    }
    assert(JDBCProcessor.count("SELECT * FROM  tx_test WHERE name in (?,?,?)", List("A", "B", "C")).body == 0)

  }

  def testTxFail() = JDBCProcessor.tx {
    JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("张三", 23))
    JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("李四", 111))
    test2()
  }

  def test2(): Resp[Void] = {
    JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("王五", 234))
    Resp.forbidden("")
  }

  def test3(): Resp[Void] = {
    JDBCProcessor.update(s"""INSERT INTO tx_test (name,age) VALUES (? ,?)""", List("王五", 234))
  }

  def testSP(): Unit = {
    JDBCProcessor.ddl("""DROP PROCEDURE IF EXISTS sp_test""")
    JDBCProcessor.ddl(
      """
        |CREATE PROCEDURE sp_test(IN num INT,OUT new_num INT)
        |BEGIN
        |	SET new_num := num + 10;
        |END
      """.stripMargin)
    val result = JDBCProcessor.sp("CALL ez.sp_test(?,?)", List((10, 1)), List(("newNum", classOf[Int], 2)))
    assert(result.body("newNum").asInstanceOf[Int] == 20)
  }

}

@Entity("")
case class JDBC_Test_Entity() extends SecureModel with StatusModel {
  @Unique
  @Require
  @Desc("姓名", 40, 0)
  @BeanProperty var name: String = _
  @BeanProperty var age: Int = _
  @NowBySave
  @BeanProperty var time_auto_create: Date = _
  @NowBySave
  @NowByUpdate
  @BeanProperty var time_auto_update: Date = _
  @BeanProperty var time: Date = _
  @BeanProperty var date1: Date = _
  @BeanProperty var date2: Date = _
}

object JDBC_Test_Entity extends SecureStorage[JDBC_Test_Entity] with StatusStorage[JDBC_Test_Entity] {
  override lazy val tableName: String = "test_entity"
}

@Entity("")
case class JDBC_Test_Int() extends BaseModel {
  @BeanProperty var status: Int = _
  @BeanProperty var enabled: Boolean = _
  @BeanProperty var gender: Int = _
  @BeanProperty var age: Int = _
}

object JDBC_Test_Int extends BaseStorage[JDBC_Test_Int]







