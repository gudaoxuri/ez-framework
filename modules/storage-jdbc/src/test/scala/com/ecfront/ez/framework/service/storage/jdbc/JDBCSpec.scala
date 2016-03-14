package com.ecfront.ez.framework.service.storage.jdbc

import com.ecfront.common.StandardCode
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.storage.foundation._

import scala.beans.BeanProperty

class JDBCSpec extends MockStartupSpec {

  test("JDBC Test") {

    // JDBCProcessor.find("select count(*) from testA join testB on testA.id = testB.id",List(),classOf[Long]),Duration.Inf)

    val createFuture = JDBCProcessor.update(
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
    createFuture

    JDBC_Test_Entity.deleteByCond("1=1", List()).body

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

}

@Entity("")
case class JDBC_Test_Entity() extends SecureModel with StatusModel {

  @Unique
  @Require
  @Label("姓名")
  @BeanProperty var name: String = _
  @BeanProperty var age: Int = _

}

object JDBC_Test_Entity extends JDBCSecureStorage[JDBC_Test_Entity] with JDBCStatusStorage[JDBC_Test_Entity]