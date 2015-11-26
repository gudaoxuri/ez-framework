package com.asto.ez.framework.storage.mongo

import com.asto.ez.framework.storage.StatusModel

trait MongoStatusModel extends MongoBaseModel with StatusModel {

  /*  def findEnabled(condition: String = " 1=1 ", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[List[this.type]]] = {
      DBExecutor.find(_entityInfo, if (context == null) EZContext.build() else context, appendEnabled(condition), parameters)
    }

    def pageEnabled(condition: String = " 1=1 ", parameters: List[Any] = List(), pageNumber: Long = 1, pageSize: Int = 10, context: EZContext = null): Future[Resp[Page[this.type]]] = {
      DBExecutor.page(_entityInfo, if (context == null) EZContext.build() else context, appendEnabled(condition), parameters, pageNumber, pageSize)
    }

    def countEnabled(condition: String = " 1=1 ", parameters: List[Any] = List(), context: EZContext = null): Future[Resp[Long]] = {
      DBExecutor.count(_entityInfo, if (context == null) EZContext.build() else context, appendEnabled(condition), parameters)
    }

    private def appendEnabled(condition: String): String = {
      condition + " AND enable = true "
    }*/

}








