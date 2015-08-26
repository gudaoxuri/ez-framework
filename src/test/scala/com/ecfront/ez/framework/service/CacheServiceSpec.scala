package com.ecfront.ez.framework.service

import com.ecfront.common.SReq
import com.ecfront.ez.framework.BasicSpec
import com.ecfront.ez.framework.service.protocols.CacheService

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class CacheServiceSpec extends BasicSpec {

  test("Cache测试") {

    val request = Some(SReq("0000", "jzy"))

    Await.result(TestCacheService._deleteAll(request), Duration.Inf)
    //-------------------save--------------------------------------------
    val model = TestModel()
    model.name = "张三"
    model.bool = true
    model.age = 14
    model.id = "id001"
    Await.result(TestCacheService._save(model, request), Duration.Inf)
    assert(Await.result(TestCacheService._save(model, request), Duration.Inf).message == "Id exist :id001")
    var resultSingle = Await.result(TestCacheService._getById("id001", request), Duration.Inf).body
    assert(resultSingle.name == "张三")
    assert(resultSingle.bool)
    assert(resultSingle.create_user == "jzy")
    assert(resultSingle.update_user == "jzy")
    assert(resultSingle.create_time != 0)
    assert(resultSingle.update_time != 0)
    //-------------------update--------------------------------------------
    model.name = "haha"
    model.bool = false
    Await.result(TestCacheService._update("id001", model, request), Duration.Inf)
    resultSingle = Await.result(TestCacheService._getById("id001", request), Duration.Inf).body
    assert(resultSingle.name == "haha")
    assert(resultSingle.create_time != 0)
    assert(!resultSingle.bool)
    //-------------------getByCondition--------------------------------------------
    /* resultSingle = Await.result(TestCacheService._getByCondition("id=? AND name=? ", Some(List("id001", "haha")), request), Duration.Inf).body
     assert(resultSingle.name == "haha")*/
    //-------------------findAll--------------------------------------------
    var resultList = Await.result(TestCacheService._findAll(request), Duration.Inf).body
    assert(resultList.size == 1)
    assert(resultList.head.name == "haha")
    //-------------------pageAll--------------------------------------------
    model.id = null
    Await.result(TestCacheService._save(model, request), Duration.Inf)
    model.id = null
    Await.result(TestCacheService._save(model, request), Duration.Inf)
    model.id = null
    Await.result(TestCacheService._save(model, request), Duration.Inf)
    model.id = null
    model.name = "last"
    Await.result(TestCacheService._save(model, request), Duration.Inf)
    /*  var resultPage = Await.result(TestCacheService._pageAll(2, 2, request), Duration.Inf).body
     assert(resultPage.getPageNumber == 2)
     assert(resultPage.getPageSize == 2)
     assert(resultPage.getPageTotal == 3)
     assert(resultPage.getRecordTotal == 5)*/
    //-------------------pageByCondition--------------------------------------------
    /*resultPage = Await.result(TestCacheService._pageByCondition("name = ? ORDER BY create_time desc", Some(List("haha")), 1, 3, request), Duration.Inf).body
    assert(resultPage.getPageNumber == 1)
    assert(resultPage.getPageSize == 3)
    assert(resultPage.getPageTotal == 2)
    assert(resultPage.getRecordTotal == 4)*/
    //-------------------deleteById--------------------------------------------
    resultList = Await.result(TestCacheService._findAll(request), Duration.Inf).body
    assert(resultList.size == 5)
    Await.result(TestCacheService._deleteById(resultList.last.id, request), Duration.Inf)
    resultList = Await.result(TestCacheService._findAll(request), Duration.Inf).body
    assert(resultList.size == 4)
    //-------------------deleteAll--------------------------------------------
    Await.result(TestCacheService._deleteAll(request), Duration.Inf)
    resultList = Await.result(TestCacheService._findAll(request), Duration.Inf).body
    assert(resultList.isEmpty)
  }

}

object TestCacheService extends CacheService[TestModel,SReq] with FutureService[TestModel,SReq]




