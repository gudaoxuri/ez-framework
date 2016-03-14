package com.ecfront.ez.framework.service.storage.mongo

import com.ecfront.common.StandardCode
import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.storage.foundation._
import io.vertx.core.json.JsonArray

import scala.beans.BeanProperty

class MongoSpec extends MockStartupSpec {

  test("Mongo Test") {

    Mongo_Test_Entity.deleteByCond("")

    var mongo = Mongo_Test_Entity()
    mongo.parameters = Map("k1" -> "v1", "k2" -> 0, "k3" -> Map("k3-1" -> "v3-1"))
    Mongo_Test_Entity.save(mongo).code == StandardCode.BAD_REQUEST

    mongo.name = "name1"
    var id = Mongo_Test_Entity.save(mongo).body.id
    var getResult = Mongo_Test_Entity.getById(id).body
    assert(getResult.name == "name1")
    getResult.name = "name_new"
    Mongo_Test_Entity.update(getResult).body
    getResult = Mongo_Test_Entity.getById(id).body
    assert(getResult.name == "name_new")
    assert(getResult.parameters == Map("k1" -> "v1", "k2" -> 0, "k3" -> Map("k3-1" -> "v3-1")))
    Mongo_Test_Entity.deleteById(id).body
    getResult = Mongo_Test_Entity.getById(id).body
    assert(getResult == null)

    mongo.name = "name_new_2"
    id = Mongo_Test_Entity.saveOrUpdate(mongo).body.id
    getResult = Mongo_Test_Entity.getById(id).body
    getResult.name = "name_new_3"
    id = Mongo_Test_Entity.saveOrUpdate(getResult).body.id
    getResult = Mongo_Test_Entity.getById(id).body
    assert(getResult.name == "name_new_3")
    Mongo_Test_Entity.deleteById(id).body

    mongo.id = null
    mongo.name = "n1"
    mongo.create_time = 0
    Mongo_Test_Entity.save(mongo).body
    Thread.sleep(1)
    mongo.name = "n2"
    mongo.create_time = 0
    Mongo_Test_Entity.save(mongo).body
    Thread.sleep(1)
    mongo.name = "n3"
    mongo.create_time = 0
    Mongo_Test_Entity.save(mongo).body
    Thread.sleep(1)
    mongo.id = "aaa"
    mongo.name = "n4"
    mongo.create_time = 0
    Mongo_Test_Entity.save(mongo).body

    Thread.sleep(1)
    mongo.id = "bbb"
    mongo.name = "n4"
    mongo.create_time = 0
    assert(Mongo_Test_Entity.save(mongo).message.contains("姓名"))
    assert(Mongo_Test_Entity.getById("bbb").body == null)
    getResult = Mongo_Test_Entity.getById("aaa").body
    getResult.name = "n2"
    assert(Mongo_Test_Entity.update(getResult).message.contains("姓名"))


    Mongo_Test_Entity.updateByCond( s"""{"$$set": {"name":"m4"}}""", s"""{"_id":"aaa"}""").body

    var findResult = Mongo_Test_Entity.find("{}").body
    assert(findResult.size == 4)
    assert(findResult.head.name == "n1")
    findResult = Mongo_Test_Entity.findWithOpt( s"""{"name":{"$$regex":"^n"}}""", Map(SecureModel.CREATE_TIME_FLAG -> SortEnum.DESC)).body
    assert(findResult.size == 3)
    assert(findResult.head.name == "n3")

    var pageResult = Mongo_Test_Entity.page("{}").body
    assert(pageResult.pageNumber == 1)
    assert(pageResult.pageSize == 10)
    assert(pageResult.recordTotal == 4)
    assert(pageResult.objects.size == 4)
    assert(pageResult.objects.head.name == "n1")
    pageResult = Mongo_Test_Entity.pageWithOpt( s"""{"name":{"$$regex":"^n"}}""", 2, 2, Map(SecureModel.CREATE_TIME_FLAG -> SortEnum.DESC)).body
    assert(pageResult.pageNumber == 2)
    assert(pageResult.pageSize == 2)
    assert(pageResult.recordTotal == 3)
    assert(pageResult.objects.size == 1)
    assert(pageResult.objects.head.name == "n1")

    val aggregate = Mongo_Test_Entity.aggregate(new JsonArray(
      s"""
         |[{ "$$match": { "name": {"$$ne" : "m4"} } },
         | {
         |   "$$group": {
         |      "_id": "$$name",
         |      "count": { "$$sum": 1 }
         | }
         |}]
      """.stripMargin
    )).body
    assert(aggregate.size() > 0)

    getResult = Mongo_Test_Entity.getById("aaa").body
    assert(getResult != null)
    Mongo_Test_Entity.deleteByCond( s"""{"name":"m4"}""").body
    getResult = Mongo_Test_Entity.getById("aaa").body
    assert(getResult == null)
    Mongo_Test_Entity.deleteByCond("{}").body
    findResult = Mongo_Test_Entity.find("{}").body
    assert(findResult.isEmpty)


    mongo = Mongo_Test_Entity()
    mongo.name = "name1"
    mongo.parameters = Map("k1" -> "v1", "k2" -> 0, "k3" -> Map("k3-1" -> "v3-1"))
    id = Mongo_Test_Entity.save(mongo).body.id
    mongo = Mongo_Test_Entity.getById(id).body
    assert(!mongo.enable)
    mongo.enable = true
    Mongo_Test_Entity.update(mongo)
    assert(Mongo_Test_Entity.getById(id).body.enable)
    Mongo_Test_Entity.disableById(id)
    assert(!Mongo_Test_Entity.getById(id).body.enable)
    Mongo_Test_Entity.enableById(id)
    assert(Mongo_Test_Entity.getById(id).body.enable)

  }

}

@Entity("")
case class Mongo_Test_Entity() extends SecureModel with StatusModel {

  @Unique
  @Label("姓名")
  @Require
  @BeanProperty var name: String = _
  @BeanProperty var parameters: Map[String, Any] = _

}

object Mongo_Test_Entity extends MongoSecureStorage[Mongo_Test_Entity] with MongoStatusStorage[Mongo_Test_Entity]