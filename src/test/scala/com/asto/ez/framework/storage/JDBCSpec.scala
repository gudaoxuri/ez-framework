package com.asto.ez.framework.storage

import java.util.concurrent.CountDownLatch

import com.asto.ez.framework.storage.jdbc._

import scala.async.Async.{async, await}
import scala.beans.BeanProperty
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class JDBCSpec extends JDBCBasicSpec {

  test("JDBC Test") {
    val createFuture = DBProcessor.update(
      """
        |CREATE TABLE IF NOT EXISTS jdbc_test_entity
        |(
        | id INT NOT NULL AUTO_INCREMENT ,
        | name varchar(100) NOT NULL ,
        | age INT NOT NULL ,
        | create_user varchar(100) NOT NULL COMMENT '创建用户' ,
        | create_org varchar(100) NOT NULL COMMENT '创建组织' ,
        | create_time BIGINT NOT NULL COMMENT '创建时间(yyyyMMddHHmmssSSS)' ,
        | update_user varchar(100) NOT NULL COMMENT '更新用户' ,
        | update_org varchar(100) NOT NULL COMMENT '更新组织' ,
        | update_time BIGINT NOT NULL COMMENT '更新时间(yyyyMMddHHmmssSSS)' ,
        | enable BOOLEAN NOT NULL COMMENT '是否启用' ,
        | PRIMARY KEY(id)
        |)ENGINE=innodb DEFAULT CHARSET=utf8
      """.stripMargin
    )
    Await.result(createFuture, Duration.Inf)

    Await.result(JDBC_Test_Entity().deleteByCond("1=1", List()), Duration.Inf).body

    val jdbc = JDBC_Test_Entity()
    jdbc.id = 1
    jdbc.name = "name1"
    jdbc.age = 10
    Await.result(jdbc.save(), Duration.Inf).body
    var getResult = Await.result(JDBC_Test_Entity().getById(1), Duration.Inf).body
    assert(getResult.name == "name1")
    getResult.name = "name_new"
    Await.result(getResult.update(), Duration.Inf).body
    getResult = Await.result(JDBC_Test_Entity().getById(1), Duration.Inf).body
    assert(getResult.name == "name_new")
    assert(getResult.age == 10)
    Await.result(JDBC_Test_Entity().deleteById(1), Duration.Inf).body
    getResult = Await.result(JDBC_Test_Entity().getById(1), Duration.Inf).body
    assert(getResult == null)

    jdbc.name = "name_new_2"
    Await.result(jdbc.saveOrUpdate(), Duration.Inf).body
    getResult = Await.result(JDBC_Test_Entity().getById(1), Duration.Inf).body
    getResult.name = "name_new_3"
    Await.result(getResult.saveOrUpdate(), Duration.Inf).body
    getResult = Await.result(JDBC_Test_Entity().getById(1), Duration.Inf).body
    assert(getResult.name == "name_new_3")
    Await.result(JDBC_Test_Entity().deleteById(1), Duration.Inf).body

    jdbc.id = 0
    jdbc.name = "n1"
    jdbc.age = 10
    Await.result(jdbc.save(), Duration.Inf).body
    Thread.sleep(1)
    jdbc.name = "n2"
    jdbc.create_time = 0
    Await.result(jdbc.save(), Duration.Inf).body
    Thread.sleep(1)
    jdbc.name = "n3"
    jdbc.create_time = 0
    Await.result(jdbc.save(), Duration.Inf).body
    Thread.sleep(1)
    jdbc.id = 200
    jdbc.name = "n4"
    jdbc.create_time = 0
    Await.result(jdbc.save(), Duration.Inf).body

    Thread.sleep(1)
    jdbc.id = 300
    jdbc.name = "n4"
    jdbc.create_time = 0
    assert(Await.result(jdbc.save(), Duration.Inf).message.contains("姓名"))
    assert(Await.result(JDBC_Test_Entity().getById(300), Duration.Inf).body==null)
    getResult = Await.result(JDBC_Test_Entity().getById(200), Duration.Inf).body
    getResult.name="n2"
    assert(Await.result(getResult.update(), Duration.Inf).message.contains("姓名"))

    Await.result(JDBC_Test_Entity().updateByCond("name =?", "id =?", List("m4", 200)), Duration.Inf)

    var findResult = Await.result(JDBC_Test_Entity().find(" 1=1 ORDER BY create_time ASC"), Duration.Inf).body
    assert(findResult.size == 4)
    assert(findResult.head.name == "n1")
    findResult = Await.result(JDBC_Test_Entity().find("name like ? ORDER BY create_time DESC", List("n%")), Duration.Inf).body
    assert(findResult.size == 3)
    assert(findResult.head.name == "n3")

    var pageResult = Await.result(JDBC_Test_Entity().page(" 1=1 ORDER BY create_time ASC"), Duration.Inf).body
    assert(pageResult.pageNumber == 1)
    assert(pageResult.pageSize == 10)
    assert(pageResult.recordTotal == 4)
    assert(pageResult.objects.size == 4)
    assert(pageResult.objects.head.name == "n1")
    pageResult = Await.result(JDBC_Test_Entity().page("name like ? ORDER BY create_time DESC", List("n%"), 2, 2), Duration.Inf).body
    assert(pageResult.pageNumber == 2)
    assert(pageResult.pageSize == 2)
    assert(pageResult.recordTotal == 3)
    assert(pageResult.objects.size == 1)
    assert(pageResult.objects.head.name == "n1")

    getResult = Await.result(JDBC_Test_Entity().getById(200), Duration.Inf).body
    assert(getResult != null)
    Await.result(JDBC_Test_Entity().deleteByCond("name =?", List("m4")), Duration.Inf).body
    getResult = Await.result(JDBC_Test_Entity().getById(200), Duration.Inf).body
    assert(getResult == null)
    Await.result(JDBC_Test_Entity().deleteByCond("1=1", List()), Duration.Inf).body
    findResult = Await.result(JDBC_Test_Entity().find("1=1"), Duration.Inf).body
    assert(findResult.isEmpty)

  }

  test("JDBC Async Test") {

    val cdl = new CountDownLatch(1)
    testJDBCAsync().onSuccess {
      case resp =>
        cdl.countDown()
    }
    cdl.await()
  }

  def testJDBCAsync() = async {
    val createFuture = DBProcessor.update(
      """
        |CREATE TABLE IF NOT EXISTS jdbc_test_entity
        |(
        | id INT NOT NULL AUTO_INCREMENT ,
        | name varchar(100) NOT NULL ,
        | age INT NOT NULL ,
        | create_user varchar(100) NOT NULL COMMENT '创建用户' ,
        | create_org varchar(100) NOT NULL COMMENT '创建组织' ,
        | create_time BIGINT NOT NULL COMMENT '创建时间(yyyyMMddHHmmssSSS)' ,
        | update_user varchar(100) NOT NULL COMMENT '更新用户' ,
        | update_org varchar(100) NOT NULL COMMENT '更新组织' ,
        | update_time BIGINT NOT NULL COMMENT '更新时间(yyyyMMddHHmmssSSS)' ,
        | enable BOOLEAN NOT NULL COMMENT '是否启用' ,
        | PRIMARY KEY(id)
        |)ENGINE=innodb DEFAULT CHARSET=utf8
      """.stripMargin
    )
    await(createFuture)

    await(JDBC_Test_Entity().deleteByCond("1=1", List())).body

    val jdbc = JDBC_Test_Entity()
    jdbc.id = 1
    jdbc.name = "name1"
    jdbc.age = 10
    await(jdbc.save()).body
    var getResult = await(JDBC_Test_Entity().getById(1)).body
    assert(getResult.name == "name1")
    getResult.name = "name_new"
    await(getResult.update()).body
    getResult = await(JDBC_Test_Entity().getById(1)).body
    assert(getResult.name == "name_new")
    assert(getResult.age == 10)
    await(JDBC_Test_Entity().deleteById(1)).body
    getResult = await(JDBC_Test_Entity().getById(1)).body
    assert(getResult == null)

    jdbc.name = "name_new_2"
    await(jdbc.saveOrUpdate()).body
    getResult = await(JDBC_Test_Entity().getById(1)).body
    getResult.name = "name_new_3"
    await(getResult.saveOrUpdate()).body
    getResult = await(JDBC_Test_Entity().getById(1)).body
    assert(getResult.name == "name_new_3")
    await(JDBC_Test_Entity().deleteById(1)).body

    jdbc.id = 0
    jdbc.name = "n1"
    jdbc.age = 10
    await(jdbc.save()).body
    Thread.sleep(1)
    jdbc.name = "n2"
    jdbc.create_time = 0
    await(jdbc.save()).body
    Thread.sleep(1)
    jdbc.name = "n3"
    jdbc.create_time = 0
    await(jdbc.save()).body
    Thread.sleep(1)
    jdbc.id = 200
    jdbc.name = "n4"
    jdbc.create_time = 0
    await(jdbc.save()).body

    await(JDBC_Test_Entity().updateByCond("name =?", "id =?", List("m4", 200)))

    var findResult = await(JDBC_Test_Entity().find(" 1=1 ORDER BY create_time ASC")).body
    assert(findResult.size == 4)
    assert(findResult.head.name == "n1")
    findResult = await(JDBC_Test_Entity().find("name like ? ORDER BY create_time DESC", List("n%"))).body
    assert(findResult.size == 3)
    assert(findResult.head.name == "n3")

    var pageResult = await(JDBC_Test_Entity().page(" 1=1 ORDER BY create_time ASC")).body
    assert(pageResult.pageNumber == 1)
    assert(pageResult.pageSize == 10)
    assert(pageResult.recordTotal == 4)
    assert(pageResult.objects.size == 4)
    assert(pageResult.objects.head.name == "n1")
    pageResult = await(JDBC_Test_Entity().page("name like ? ORDER BY create_time DESC", List("n%"), 2, 2)).body
    assert(pageResult.pageNumber == 2)
    assert(pageResult.pageSize == 2)
    assert(pageResult.recordTotal == 3)
    assert(pageResult.objects.size == 1)
    assert(pageResult.objects.head.name == "n1")

    getResult = await(JDBC_Test_Entity().getById(200)).body
    assert(getResult != null)
    await(JDBC_Test_Entity().deleteByCond("name =?", List("m4"))).body
    getResult = await(JDBC_Test_Entity().getById(200)).body
    assert(getResult == null)
    await(JDBC_Test_Entity().deleteByCond("1=1", List())).body
    findResult = await(JDBC_Test_Entity().find("1=1")).body
    assert(findResult.isEmpty)

  }

}

@Entity("")
case class JDBC_Test_Entity() extends JDBCSecureModel with JDBCStatusModel {

  @Id("seq")
  @BeanProperty var id: Long = _
  @Unique @Label("姓名")
  @BeanProperty var name: String = _
  @BeanProperty var age: Int = _

}