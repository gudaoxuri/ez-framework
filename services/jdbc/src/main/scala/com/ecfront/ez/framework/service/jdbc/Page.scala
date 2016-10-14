package com.ecfront.ez.framework.service.jdbc

/**
  * 分页VO
  *
  * @tparam E 实体类型
  */
class Page[E] {
  // 当前页，从1开始
  var pageNumber: Long = _
  // 每页条数
  var pageSize: Int = _
  // 总共页数
  var pageTotal: Long = _
  // 总共记录数
  var recordTotal: Long = _
  // 当前页的实体列表
  var objects: List[E] = _
}
