package com.ecfront.ez.framework.service

import com.ecfront.common.Req

trait SyncService[M <: AnyRef, R <: Req] extends SyncVOService[M, M, R] {

}



