package com.asto.ez.framework.function

import java.util.concurrent.CountDownLatch

import com.asto.ez.framework.helper.DBHelper
import io.vertx.core.json.JsonObject

import scala.concurrent.ExecutionContext.Implicits.global

class JDBCSpec extends DBBasicSpec {

  test("JDBC Test") {
    val cdl = new CountDownLatch(1)
    val createFuture = DBHelper.update(
      """
        |CREATE TABLE IF NOT EXISTS test
        |(
        | id varchar(20) ,
        | occur_time varchar(14) ,
        | c_platform varchar(10) ,
        | c_system varchar(10) ,
        | PRIMARY KEY(id)
        |)ENGINE=innodb DEFAULT CHARSET=utf8
      """.stripMargin
    )
    createFuture.onSuccess {
      case createResp =>
        for {
          insert1Future <- DBHelper.update(
            """INSERT INTO test ( id ,  occur_time , c_platform , c_system ) VALUES( '1','20151018','pc','微软')"""
          )
          insert2Future <- DBHelper.update(
            """INSERT INTO test ( id ,  occur_time , c_platform , c_system ) VALUES( '2','20151018','pc','win10')"""
          )
          insert3Future <- DBHelper.update(
            """INSERT INTO test ( id ,  occur_time , c_platform , c_system ) VALUES( '3','20151018','pc','win10')"""
          )
          insert4Future <- DBHelper.update(
            """INSERT INTO test ( id ,  occur_time , c_platform , c_system ) VALUES( '4','20151019','pc','win10')"""
          )
          update1Future <- DBHelper.update("UPDATE test SET c_platform = 'mobile' WHERE id = ? ", List("1"))
          delete1Future <- DBHelper.update("DELETE FROM test WHERE id = ? ", List("4"))
        } yield {
          DBHelper.get[JsonObject]("SELECT * FROM test WHERE id = ? ", List("1")).onSuccess {
            case get1Resp =>
              assert(get1Resp && get1Resp.body.getString("c_platform") == "mobile")
              DBHelper.get("SELECT * FROM test WHERE id = ? ", List("1"), classOf[TestEntity]).onSuccess {
                case get2Resp =>
                  assert(get2Resp && get2Resp.body.occur_time == "20151018")
                  DBHelper.find("SELECT * FROM test", List(), classOf[TestEntity]).onSuccess {
                    case findAllResp =>
                      assert(findAllResp && findAllResp.body.length == 3)
                      DBHelper.page("SELECT * FROM test ORDER BY id ASC", List(), 1, 2, classOf[TestEntity]).onSuccess {
                        case pageAllResp =>
                          assert(pageAllResp
                            && pageAllResp.body.pageNumber == 1
                            && pageAllResp.body.pageSize == 2
                            && pageAllResp.body.pageTotal == 2
                            && pageAllResp.body.recordTotal == 3
                            && pageAllResp.body.objects.head.id == "1")
                          cdl.countDown()
                      }
                  }
              }
          }
        }
    }
    cdl.await()
  }

}

case class TestEntity(id: String, occur_time: String, c_platform: String, c_system: String)
