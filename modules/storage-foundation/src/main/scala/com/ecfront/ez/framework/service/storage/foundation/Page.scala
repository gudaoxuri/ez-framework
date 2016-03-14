package com.ecfront.ez.framework.service.storage.foundation

class Page[E] {
  //start with 1
  var pageNumber: Long = _
  var pageSize: Int = _
  var pageTotal: Long = _
  var recordTotal: Long = _
  var objects: List[E] = _
}
