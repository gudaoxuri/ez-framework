package com.ecfront.ez.framework.service.storage.jdbc

import com.ecfront.ez.framework.core.test.MockStartupSpec

class TxSpec extends MockStartupSpec {

  test("Tx Test") {

    JDBCProcessor.update(
      """
        |CREATE TABLE IF NOT EXISTS jdbc_test_entity
        |(
        | id INT NOT NULL AUTO_INCREMENT ,
        | name varchar(100) NOT NULL ,
        | age INT NOT NULL ,
        | rel1 JSON ,
        | rel2 JSON ,
        | rel3 JSON ,
        | rel4 JSON ,
        | create_user varchar(100)  COMMENT '创建用户' ,
        | create_org varchar(100)  COMMENT '创建组织' ,
        | create_time BIGINT COMMENT  '创建时间(yyyyMMddHHmmssSSS)' ,
        | update_user varchar(100) COMMENT '更新用户' ,
        | update_org varchar(100) COMMENT '更新组织' ,
        | update_time BIGINT COMMENT '更新时间(yyyyMMddHHmmssSSS)' ,
        | enable BOOLEAN COMMENT '是否启用' ,
        | PRIMARY KEY(id)
        |)ENGINE=innodb DEFAULT CHARSET=utf8
      """.stripMargin
    )

    var conn = JDBCProcessor.openTx()
    JDBCProcessor.update(s"""INSERT INTO jdbc_test_entity (name,age) VALUES (? ,?)""", List("张三", 23), conn)
    JDBCProcessor.update(s"""INSERT INTO jdbc_test_entity (name,age) VALUES (? ,?)""", List("李四", 111), conn)
    JDBCProcessor.rollback(conn)
    assert(!JDBCProcessor.exist("SELECT * FROM  jdbc_test_entity WHERE name =?", List("张三")).body)

    conn = JDBCProcessor.openTx()
    JDBCProcessor.update(s"""INSERT INTO jdbc_test_entity (name,age) VALUES (? ,?)""", List("张三", 23), conn)
    JDBCProcessor.update(s"""INSERT INTO jdbc_test_entity (name,age) VALUES (? ,?)""", List("李四", 111), conn)
    // 需要至少两个连接
    assert(!JDBCProcessor.exist("SELECT * FROM  jdbc_test_entity WHERE name =?", List("张三")).body)
    JDBCProcessor.commit(conn)
    assert(JDBCProcessor.exist("SELECT * FROM  jdbc_test_entity WHERE name =?", List("张三")).body)

  }

}





