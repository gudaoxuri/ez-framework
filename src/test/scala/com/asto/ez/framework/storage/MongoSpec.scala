package com.asto.ez.framework.storage

import java.util.concurrent.CountDownLatch

import com.asto.ez.framework.storage.mongo.{MongoProcessor, MongoSecureModel, MongoStatusModel, SortEnum}
import com.asto.ez.framework.{BasicSpec, EZGlobal}
import io.vertx.core.json.JsonArray
import io.vertx.ext.mongo.MongoClient

import scala.async.Async.{async, await}
import scala.beans.BeanProperty
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class MongoSpec extends BasicSpec {

  override def before2(): Any = {
    val mongo = EZGlobal.ez_storage.getJsonObject("mongo")
    MongoProcessor.mongoClient = MongoClient.createShared(EZGlobal.vertx, mongo)
  }

  test("Mongo Test") {

    Await.result(Mongo_Test_Entity().deleteByCond("{}"), Duration.Inf).body

    val mongo = Mongo_Test_Entity()
    mongo.name = "name1"
    mongo.parameters = Map("k1" -> "v1", "k2" -> 0, "k3" -> Map("k3-1" -> "v3-1"))
    var id = Await.result(mongo.save(), Duration.Inf).body
    var getResult = Await.result(Mongo_Test_Entity().getById(id), Duration.Inf).body
    assert(getResult.name == "name1")
    getResult.name = "name_new"
    Await.result(getResult.update(), Duration.Inf).body
    getResult = Await.result(Mongo_Test_Entity().getById(id), Duration.Inf).body
    assert(getResult.name == "name_new")
    assert(getResult.parameters == Map("k1" -> "v1", "k2" -> 0, "k3" -> Map("k3-1" -> "v3-1")))
    Await.result(Mongo_Test_Entity().deleteById(id), Duration.Inf).body
    getResult = Await.result(Mongo_Test_Entity().getById(id), Duration.Inf).body
    assert(getResult == null)

    mongo.name = "name_new_2"
    id = Await.result(mongo.saveOrUpdate(), Duration.Inf).body
    getResult = Await.result(Mongo_Test_Entity().getById(id), Duration.Inf).body
    getResult.name = "name_new_3"
    id = Await.result(getResult.saveOrUpdate(), Duration.Inf).body
    getResult = Await.result(Mongo_Test_Entity().getById(id), Duration.Inf).body
    assert(getResult.name == "name_new_3")
    Await.result(Mongo_Test_Entity().deleteById(id), Duration.Inf).body

    mongo.id = null
    mongo.name = "n1"
    mongo.create_time = 0
    Await.result(mongo.save(), Duration.Inf).body
    Thread.sleep(1)
    mongo.name = "n2"
    mongo.create_time = 0
    Await.result(mongo.save(), Duration.Inf).body
    Thread.sleep(1)
    mongo.name = "n3"
    mongo.create_time = 0
    Await.result(mongo.save(), Duration.Inf).body
    Thread.sleep(1)
    mongo.id = "aaa"
    mongo.name = "n4"
    mongo.create_time = 0
    Await.result(mongo.save(), Duration.Inf).body

    Thread.sleep(1)
    mongo.id = "bbb"
    mongo.name = "n4"
    mongo.create_time = 0
    assert(Await.result(mongo.save(), Duration.Inf).message.contains("姓名"))
    assert(Await.result(Mongo_Test_Entity().getById("bbb"), Duration.Inf).body == null)
    getResult = Await.result(Mongo_Test_Entity().getById("aaa"), Duration.Inf).body
    getResult.name = "n2"
    assert(Await.result(getResult.update(), Duration.Inf).message.contains("姓名"))


    Await.result(Mongo_Test_Entity().updateByCond(s"""{"$$set": {"name":"m4"}}""",s"""{"_id":"aaa"}"""), Duration.Inf).body

    var findResult = Await.result(Mongo_Test_Entity().find(), Duration.Inf).body
    assert(findResult.size == 4)
    assert(findResult.head.name == "n1")
    findResult = Await.result(Mongo_Test_Entity().findWithOpt(s"""{"name":{"$$regex":"^n"}}""", Map(SecureModel.CREATE_TIME_FLAG -> SortEnum.DESC)), Duration.Inf).body
    assert(findResult.size == 3)
    assert(findResult.head.name == "n3")

    var pageResult = Await.result(Mongo_Test_Entity().page(), Duration.Inf).body
    assert(pageResult.pageNumber == 1)
    assert(pageResult.pageSize == 10)
    assert(pageResult.recordTotal == 4)
    assert(pageResult.objects.size == 4)
    assert(pageResult.objects.head.name == "n1")
    pageResult = Await.result(Mongo_Test_Entity().pageWithOpt(s"""{"name":{"$$regex":"^n"}}""", 2, 2, Map(SecureModel.CREATE_TIME_FLAG -> SortEnum.DESC)), Duration.Inf).body
    assert(pageResult.pageNumber == 2)
    assert(pageResult.pageSize == 2)
    assert(pageResult.recordTotal == 3)
    assert(pageResult.objects.size == 1)
    assert(pageResult.objects.head.name == "n1")

    val arggregate = Await.result(Mongo_Test_Entity().aggregate(new JsonArray(
      s"""
         |[{ "$$match": { "name": {"$$ne" : "m4"} } },
         | {
         |   "$$group": {
         |      "_id": "$$name",
         |      "count": { "$$sum": 1 }
         | }
         |}]
      """.stripMargin
    )), Duration.Inf).body
    assert(arggregate.size() > 0)

    getResult = Await.result(Mongo_Test_Entity().getById("aaa"), Duration.Inf).body
    assert(getResult != null)
    Await.result(Mongo_Test_Entity().deleteByCond(s"""{"name":"m4"}"""), Duration.Inf).body
    getResult = Await.result(Mongo_Test_Entity().getById("aaa"), Duration.Inf).body
    assert(getResult == null)
    Await.result(Mongo_Test_Entity().deleteByCond("{}"), Duration.Inf).body
    findResult = Await.result(Mongo_Test_Entity().find(), Duration.Inf).body
    assert(findResult.isEmpty)

  }

  test("Mongo Async Test") {

    val cdl = new CountDownLatch(1)
    testMongoAsync().onSuccess {
      case resp =>
        cdl.countDown()
    }
    cdl.await()
  }

  def testMongoAsync() = async {

    await(Mongo_Test_Entity().deleteByCond("{}")).body

    val mongo = Mongo_Test_Entity()
    mongo.name = "name1"
    mongo.parameters = Map("k1" -> "v1", "k2" -> 0, "k3" -> Map("k3-1" -> "v3-1"))
    var id = await(mongo.save()).body
    var getResult = await(Mongo_Test_Entity().getById(id)).body
    assert(getResult.name == "name1")
    getResult.name = "name_new"
    await(getResult.update()).body
    getResult = await(Mongo_Test_Entity().getById(id)).body
    assert(getResult.name == "name_new")
    assert(getResult.parameters == Map("k1" -> "v1", "k2" -> 0, "k3" -> Map("k3-1" -> "v3-1")))
    assert(await(Mongo_Test_Entity().existById(id)).body)
    await(Mongo_Test_Entity().deleteById(id)).body
    assert(!await(Mongo_Test_Entity().existById(id)).body)

    mongo.name = "name_new_2"
    id = await(mongo.saveOrUpdate()).body
    getResult = await(Mongo_Test_Entity().getById(id)).body
    getResult.name = "name_new_3"
    id = await(getResult.saveOrUpdate()).body
    getResult = await(Mongo_Test_Entity().getById(id)).body
    assert(getResult.name == "name_new_3")
    await(Mongo_Test_Entity().deleteById(id)).body

    mongo.id = null
    mongo.name = "n1"
    mongo.create_time = 0
    await(mongo.save()).body
    Thread.sleep(1)
    mongo.name = "n2"
    mongo.create_time = 0
    await(mongo.save()).body
    Thread.sleep(1)
    mongo.name = "n3"
    mongo.create_time = 0
    await(mongo.save()).body
    Thread.sleep(1)
    mongo.id = "aaa"
    mongo.name = "n4"
    mongo.create_time = 0
    await(mongo.save()).body
    await(Mongo_Test_Entity().updateByCond(s"""{"$$set": {"name":"m4"}}""",s"""{"_id":"aaa"}""")).body

    var findResult = await(Mongo_Test_Entity().find()).body
    assert(findResult.size == 4)
    assert(findResult.head.name == "n1")
    findResult = await(Mongo_Test_Entity().findWithOpt(s"""{"name":{"$$regex":"^n"}}""", Map(SecureModel.CREATE_TIME_FLAG -> SortEnum.DESC))).body
    assert(findResult.size == 3)
    assert(findResult.head.name == "n3")

    var pageResult = await(Mongo_Test_Entity().page()).body
    assert(pageResult.pageNumber == 1)
    assert(pageResult.pageSize == 10)
    assert(pageResult.recordTotal == 4)
    assert(pageResult.objects.size == 4)
    assert(pageResult.objects.head.name == "n1")
    pageResult = await(Mongo_Test_Entity().pageWithOpt(s"""{"name":{"$$regex":"^n"}}""", 2, 2, Map(SecureModel.CREATE_TIME_FLAG -> SortEnum.DESC))).body
    assert(pageResult.pageNumber == 2)
    assert(pageResult.pageSize == 2)
    assert(pageResult.recordTotal == 3)
    assert(pageResult.objects.size == 1)
    assert(pageResult.objects.head.name == "n1")

    getResult = await(Mongo_Test_Entity().getById("aaa")).body
    assert(getResult != null)
    await(Mongo_Test_Entity().deleteByCond(s"""{"name":"m4"}""")).body
    getResult = await(Mongo_Test_Entity().getById("aaa")).body
    assert(getResult == null)
    await(Mongo_Test_Entity().deleteByCond("{}")).body
    findResult = await(Mongo_Test_Entity().find()).body
    assert(findResult.isEmpty)

  }

}

@Entity("")
case class Mongo_Test_Entity() extends MongoSecureModel with MongoStatusModel {
  @Unique @Label("姓名")
  @BeanProperty var name: String = _
  @BeanProperty var parameters: Map[String, Any] = _

}