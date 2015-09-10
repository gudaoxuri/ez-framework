package com.ecfront.ez.framework.storage

import org.scalatest.FunSuite


class FunSpec extends FunSuite {

  var testPath = this.getClass.getResource("/").getPath
  if (System.getProperties.getProperty("os.name").toUpperCase.indexOf("WINDOWS") != -1) {
    testPath = testPath.substring(1)
  }

  JDBCStorable.init(testPath)

  test("基础功能测试") {
    val res = Resource()
    res.id = "res1"
    res.name = "资源1"
    ResourceService.__save(res)
    assert(ResourceService.__getById("res1").get.getName == "资源1")
    res.name = "资源new"
    ResourceService.__update("res1", res)
    assert(ResourceService.__getById("res1").get.getName == "资源new")
    res.id = "res2"
    res.name = "资源2"
    ResourceService.__save(res)
    res.id = "res3"
    res.name = "资源3"
    ResourceService.__save(res)
    res.id = "res4"
    res.name = "resource4"
    ResourceService.__save(res)
    assert(ResourceService.__findAll().get.size == 4)
    val page = ResourceService.__pageByCondition("name like ? ", Some(List("资源%")), 1, 2).get
    assert(page.pageTotal == 2 && page.pageSize == 2 && page.recordTotal == 3 && page.results.size == 2)
    ResourceService.__deleteById("res1")
    assert(ResourceService.__findAll().get.size == 3)
  }

  test("Rel测试") {
    val app = App()
    app.id = "app"
    app.name = "app test 1"
    AppService.__save(app)

    val res = Resource()
    res.id = "res1"
    res.name = "资源1"
    ResourceService.__save(res)
    res.id = "res2"
    res.name = "资源2"
    ResourceService.__save(res)

    val role1 = Role()
    role1.id = "role1"
    role1.name = "admin"
    RoleService.__save(role1)
    role1.resourceIds = List("res1", "res2")
    RoleService.__update("role1", role1)
    val newRole1 = RoleService.__getById("role1").get
    assert(newRole1.id == "role1")
    assert(newRole1.name == "admin")
    assert(newRole1.resourceIds == List("res1", "res2"))

    val role2 = Role()
    role2.id = "role2"
    role2.name = "user"
    role2.resourceIds = List("res1", "res2")
    RoleService.__save(role2)
    role2.resourceIds = List()
    RoleService.__update("role2", role2)
    val newRole2 = RoleService.__getById("role2").get
    assert(newRole2.id == "role2")
    assert(newRole2.name == "user")
    assert(newRole2.resourceIds == List())

    val account1 = Account()
    account1.id = "user1"
    account1.name = "用户1"
    account1.app_id = "app"
    account1.enable = true
    account1.roleIds = List("role1")
    AccountService.__save(account1)
    val newAccount1 = AccountService.__getById("user1").get
    assert(newAccount1.id == "user1")
    assert(newAccount1.name == "用户1")
    assert(newAccount1.app_id == "app")
    assert(newAccount1.enable)
    assert(newAccount1.roleIds == List("role1"))
    assert(newAccount1.roleInfos("role1").name == "admin")

    val account2 = Account()
    account2.id = "user2"
    account2.name = "用户2"
    account2.app_id = "app"
    account2.enable = true
    account2.roleInfos = Map("role1" -> null, "role2" -> null)
    AccountService.__save(account2)
    var newAccount2 = AccountService.__getById("user2").get
    assert(newAccount2.roleIds == List("role1", "role2"))
    assert(newAccount2.roleInfos("role1").name == "admin")
    assert(newAccount2.roleInfos("role2").name == "user")
    newAccount2.roleIds = List()
    newAccount2.roleInfos = Map[String,Role]()
    AccountService.__update("user2", newAccount2)
    newAccount2 = AccountService.__getById("user2").get
    assert(newAccount2.id == "user2")
    assert(newAccount2.name == "用户2")
    assert(newAccount2.app_id == "app")
    assert(newAccount2.enable)
    assert(newAccount2.roleIds == List())
    assert(newAccount2.roleInfos == Map[String,Role]())

    var newApp = AppService.__getById("app").get
    assert(newApp.id == "app")
    assert(newApp.name == "app test 1")
    assert(newApp.accountIds == List("user1", "user2"))
    assert(newApp.accountInfos("user1").name == "用户1")
    assert(newApp.accountInfos("user2").name == "用户2")
    //disable
    newAccount2 = AccountService.__getById("user2").get
    newAccount2.enable = false
    AccountService.__update(newAccount2.id, newAccount2)
    newApp = AppService.__getById("app").get
    assert(newApp.id == "app")
    assert(newApp.name == "app test 1")
    assert(newApp.accountIds == List("user1"))
    assert(newApp.accountInfos("user1").name == "用户1")

    RoleService.__deleteById("role1")
    AppService.__deleteById("app")
  }

  test("save & update 测试") {
    EntityContainer.autoBuilding("com.ecfront.ez.framework.storage")
    val res = Resource()
    res.id = "res1"
    res.name = "资源1"
    ResourceService.__save(res)
    res.id = "res2"
    res.name = "资源2"
    ResourceService.__save(res)
    //auto id test
    val role = Role()
    role.name = "管理员"
    role.code = "admin"
    role.resourceIds = List("res1", "res2")
    val nRole = RoleService.__getById(RoleService.__save(role, null).get, null).get
    assert(nRole.code == "admin")
    assert(nRole.name == "管理员")
    assert(nRole.resourceIds.size == 2)
    //update without null value test
    nRole.code = "root"
    nRole.name = null
    nRole.resourceIds = null
    val nnRole = RoleService.__getById(RoleService.__update(nRole.id, nRole, null).get, null).get
    assert(nnRole.code == "root")
    assert(nnRole.name == "管理员")
    assert(nnRole.resourceIds.size == 2)
  }

  //此测试目前仅适用Postgres
  test("Seq Id 测试") {
    val log = Log()
    log.name = "test1"
    LogService.__save(log)
    assert(LogService.__getById("1").get.getName == "test1")
    log.name = "test1-1"
    LogService.__update("1", log)
    assert(LogService.__getById("1").get.getName == "test1-1")
    log.id = "4"
    log.name = "test4"
    LogService.__save(log)
    log.id = "5"
    log.name = "test5"
    LogService.__save(log)
    log.id = "6"
    log.name = "测试6"
    LogService.__save(log)
    assert(LogService.__findAll().get.size == 4)
    val page = LogService.__pageByCondition("name like ? ", Some(List("test%")), 1, 2).get
    assert(page.pageTotal == 2 && page.pageSize == 2 && page.recordTotal == 3 && page.results.size == 2)
    LogService.__deleteById("1")
    assert(LogService.__findAll().get.size == 3)
  }

}

object LogService extends JDBCStorable[Log, Void]

object AppService extends JDBCStorable[App, Void]

object RoleService extends JDBCStorable[Role, Void]

object AccountService extends JDBCStorable[Account, Void]

object ResourceService extends JDBCStorable[Resource, Void]


