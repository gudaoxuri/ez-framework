package com.asto.ez.framework.auth.manage

import com.asto.ez.framework.auth.EZ_Role
import com.asto.ez.framework.rpc.{HTTP, RPC}
import com.asto.ez.framework.scaffold.SimpleRPCService
import com.asto.ez.framework.storage.BaseStorage
import com.ecfront.common.Resp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

@RPC("/auth/manage/role/")
@HTTP
object RoleService extends SimpleRPCService[EZ_Role] {

  override protected val storageObj: BaseStorage[EZ_Role] = EZ_Role

  def appendResources(roleCode: String, appendResourceCodes: List[String]): Future[Resp[Void]] = {
    val p = Promise[Resp[Void]]()
    EZ_Role.getByCond(s"""{"code":"$roleCode"}""").onSuccess {
      case roleResp =>
        if (roleResp) {
          roleResp.body.resource_codes ++= appendResourceCodes
          EZ_Role.update(roleResp.body).onSuccess {
            case updateResp =>
              p.success(Resp.success(null))
          }
        } else {
          p.success(Resp.notFound(""))
        }
    }
    p.future
  }

}