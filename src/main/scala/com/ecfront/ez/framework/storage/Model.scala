package com.ecfront.ez.framework.storage

import com.ecfront.common.{Ignore, JsonHelper}

import scala.annotation.StaticAnnotation
import scala.beans.BeanProperty
import scala.collection.mutable.ArrayBuffer

object Model {
  val ID_FLAG = "id"
  val REL_FLAG = "rel"
}

case class PageModel[M](
                         @BeanProperty var pageNumber: Long,
                         @BeanProperty var pageSize: Long,
                         @BeanProperty var pageTotal: Long,
                         @BeanProperty var recordTotal: Long,
                         @BeanProperty var results: List[M]
                       )

object PageModel {

  val PAGE_NUMBER_FLAG = "pageNumber"
  val PAGE_SIZE_FLAG = "pageSize"

  def toPage[M](str: String, modelClazz: Class[M]): PageModel[M] = {
    val tmp = JsonHelper.toJson(str)
    val res = tmp.get("results").elements()
    val results = ArrayBuffer[M]()
    while (res.hasNext) {
      results += JsonHelper.toObject(res.next(), modelClazz)
    }
    PageModel(
      tmp.get("pageNumber").asLong(),
      tmp.get("pageSize").asLong(),
      tmp.get("pageTotal").asLong(),
      tmp.get("recordTotal").asLong(),
      results.toList
    )
  }
}

case class Entity(desc: String) extends StaticAnnotation

/**
 * 使用Seq Id，默认uuid
 */
case class SeqId() extends StaticAnnotation

@scala.annotation.meta.field
case class Text() extends StaticAnnotation

@scala.annotation.meta.field
case class Desc(desc: String) extends StaticAnnotation

@scala.annotation.meta.field
case class Index() extends StaticAnnotation

@scala.annotation.meta.field
case class Unique() extends StaticAnnotation

@scala.annotation.meta.field
case class FK() extends StaticAnnotation

@scala.annotation.meta.field
case class ManyToMany(mapping: String, master: Boolean, fetch: Boolean) extends Ignore

@scala.annotation.meta.field
case class OneToMany(mapping: String, relField: String, fetch: Boolean) extends Ignore


abstract class IdModel extends Serializable {
  @BeanProperty
  var id: String = _
}

object IdModel {
  val ID_FLAG = "id"
  val SPLIT_FLAG = "@"
}

abstract class SecureModel extends IdModel {
  @Index
  @BeanProperty var create_user: String = _
  @Index
  @BeanProperty var create_organization: String = _
  @Index
  @BeanProperty var create_time: Long = _
  @Index
  @BeanProperty var update_user: String = _
  @Index
  @BeanProperty var update_time: Long = _
  @Index
  @BeanProperty var update_organization: String = _
}

trait StatusModel {
  @Index
  @BeanProperty var enable: Boolean = _
}

object StatusModel {
  val ENABLE_FLAG = "enable"

}

object SecureModel {
  val SYSTEM_USER_FLAG = "system"
}
