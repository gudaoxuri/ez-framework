package com.ecfront.ez.framework.service.auth

import com.ecfront.ez.framework.core.test.MockStartupSpec
import com.ecfront.ez.framework.service.auth.model.{EZ_Account, EZ_Role}
import com.ecfront.ez.framework.service.storage.foundation.BaseModel

class ExtAccountSpec extends MockStartupSpec {

  test("ext inner account test") {

    EZ_Account.deleteByLoginId("t1","")
    EZ_Account.deleteByLoginId("t2","")

    var account = EZ_Account.save(EZ_Account("t1", "t1@sunisle.org", "t1", "123", List(
      BaseModel.SPLIT + EZ_Role.USER_ROLE_FLAG
    ))).body
    assert(account.ext_id == "" && account.ext_info == Map())
    account = EZ_Account("t2", "t2@sunisle.org", "t2", "123", List(
      BaseModel.SPLIT + EZ_Role.USER_ROLE_FLAG
    ))
    account.ext_info = Map("ext1" -> "1")
    account = EZ_Account.save(account).body
    assert(account.ext_id == "" && account.ext_info == Map("ext1" -> "1"))
    account.ext_info = Map("ext1" -> "new")
    account = EZ_Account.update(account).body
    assert(account.ext_id == "" && account.ext_info == Map("ext1" -> "new"))
    account = EZ_Account.getById(account.id).body
    assert(account.ext_id == "" && account.ext_info == Map("ext1" -> "new"))
    account = EZ_Account.find("").body.find(_.name == "t2").get
    assert(account.ext_id == "" && account.ext_info == Map("ext1" -> "new"))
    EZ_Account.deleteById(account.id)

  }

  test("ext new obj account test") {

/*    CREATE TABLE IF NOT EXISTS ext_test_account
      (
        id INT AUTO_INCREMENT ,
        ext1 varchar(200),
        ext2 varchar(200),
        PRIMARY KEY(id)
        )ENGINE=innodb DEFAULT CHARSET=utf8*/

    EZ_Account.init("com.ecfront.ez.framework.service.auth.Ext_Test_Account")

    EZ_Account.deleteByLoginId("t1","")
    EZ_Account.deleteByLoginId("t2","")

    var account = EZ_Account.save(EZ_Account("t1", "t1@sunisle.org", "t1", "123", List(
      BaseModel.SPLIT + EZ_Role.USER_ROLE_FLAG
    ))).body
    account = EZ_Account("t2", "t2@sunisle.org", "t2", "123", List(
      BaseModel.SPLIT + EZ_Role.USER_ROLE_FLAG
    ))
    account.ext_info = Map("ext1" -> "1")
    account = EZ_Account.save(account).body
    assert(account.ext_info("ext1")=="1")
    account.ext_info = Map("ext1" -> "new")
    account = EZ_Account.update(account).body
    assert(account.ext_info("ext1")=="new")
    account = EZ_Account.getById(account.id).body
    assert(account.ext_info("ext1")=="new")
    account = EZ_Account.find("").body.find(_.name == "t2").get
    assert(account.ext_info("ext1")=="new")
    EZ_Account.deleteById(account.id)

  }

}


