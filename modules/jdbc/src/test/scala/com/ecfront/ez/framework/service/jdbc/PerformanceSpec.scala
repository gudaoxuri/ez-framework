package com.ecfront.ez.framework.service.jdbc

import java.util.concurrent.CountDownLatch

import com.ecfront.ez.framework.core.test.MockStartupSpec

class PerformanceSpec extends MockStartupSpec {

  test("性能测试") {

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

    val threads = for (i <- 0 to 10000) yield
      new Thread(new Runnable {
        override def run(): Unit = {
          val jdbc = JDBC_Test_Entity()
          jdbc.age = 10
          jdbc.name = "name"+i
          var getResult = JDBC_Test_Entity.save(jdbc)
          if(!getResult){
            println(getResult)
          }
          assert(getResult)
        }
      })
    threads.foreach(_.start())

    new CountDownLatch(1).await()
  }


}
