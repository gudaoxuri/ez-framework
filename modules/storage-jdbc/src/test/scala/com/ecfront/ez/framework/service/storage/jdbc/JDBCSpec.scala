package com.ecfront.ez.framework.service.storage.jdbc

import java.util.Date

import com.ecfront.common.StandardCode
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.storage.foundation._

import scala.beans.BeanProperty

class JDBCSpec extends MockStartupSpec {

  test("JDBC Test") {

    // JDBCProcessor.find("select count(*) from testA join testB on testA.id = testB.id",List(),classOf[Long]),Duration.Inf)

    JDBCProcessor.update(
      """
        |CREATE TABLE IF NOT EXISTS jdbc_test_entity
        |(
        | id INT NOT NULL AUTO_INCREMENT ,
        | name varchar(100) NOT NULL ,
        | age INT NOT NULL ,
        | time timestamp NOT NULL default CURRENT_TIMESTAMP,
        | time_auto_create timestamp NOT NULL,
        | time_auto_update timestamp NOT NULL,
        | date1 datetime,
        | date2 date,
        | rel1 JSON ,
        | rel2 JSON ,
        | rel3 JSON ,
        | rel4 JSON ,
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
  }

  test("complex type test") {

    var jdbc = JDBC_Test_Entity()
    jdbc.id = "1"
    jdbc.name = "n4"
    jdbc.age = 1
    jdbc.time = new Date()
    jdbc.date1 = new Date()
    jdbc.date2 = new Date()
    jdbc.rel1 = List("1", "2")
    jdbc.rel2 = Map("s" -> "sss")
    val relModel = new Rel_Model()
    relModel.f = "r"
    jdbc.rel3 = List(relModel)
    jdbc.rel4 = relModel

    jdbc = JDBC_Test_Entity.save(jdbc).body
    assert(jdbc.rel1 == List("1", "2"))
    assert(jdbc.rel2 == Map("s" -> "sss"))
    assert(jdbc.rel3.head.f == "r")
    assert(jdbc.rel4.f == "r")
    assert(jdbc.time_auto_create != null)
    assert(jdbc.time_auto_update != null)
    assert(jdbc.time != null)
    assert(jdbc.date1 != null)
    assert(jdbc.date2 != null)
    jdbc.rel1 = List("new")
    jdbc.rel2 = Map("new" -> "new")
    val newRelModel = new Rel_Model()
    newRelModel.f = "new"
    jdbc.rel3 = List(newRelModel)
    jdbc.rel4 = newRelModel
    jdbc.time = null
    jdbc = JDBC_Test_Entity.update(jdbc).body
    assert(jdbc.rel1 == List("new"))
    assert(jdbc.rel2 == Map("new" -> "new"))
    assert(jdbc.rel3.head.f == "new")
    assert(jdbc.rel4.f == "new")
    assert(jdbc.time_auto_create != null)
    assert(jdbc.time_auto_update != null)
    assert(jdbc.time_auto_create != jdbc.time_auto_update)

    jdbc = JDBC_Test_Entity.find("").body.head
    assert(jdbc.rel1 == List("new"))
    assert(jdbc.rel2 == Map("new" -> "new"))
    assert(jdbc.rel3.head.f == "new")
    assert(jdbc.rel4.f == "new")

    jdbc = JDBC_Test_Entity.page("").body.objects.head
    assert(jdbc.rel1 == List("new"))
    assert(jdbc.rel2 == Map("new" -> "new"))
    assert(jdbc.rel3.head.f == "new")
    assert(jdbc.rel4.f == "new")

    jdbc = JDBC_Test_Entity.getById("1").body
    assert(jdbc.rel1 == List("new"))
    assert(jdbc.rel2 == Map("new" -> "new"))
    assert(jdbc.rel3.head.f == "new")
    assert(jdbc.rel4.f == "new")

  }

}

@Entity("")
case class JDBC_Test_Entity() extends SecureModel with StatusModel {

  @Unique
  @Require
  @Label("姓名")
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
  @BeanProperty var rel1: List[String] = _
  @BeanProperty var rel2: Map[String, Any] = _
  @BeanProperty var rel3: List[Rel_Model] = _
  @BeanProperty var rel4: Rel_Model = _

}

class Rel_Model extends Serializable {

  @BeanProperty var f: String = _

}

object JDBC_Test_Entity extends JDBCSecureStorage[JDBC_Test_Entity] with JDBCStatusStorage[JDBC_Test_Entity]