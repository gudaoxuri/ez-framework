package com.asto.ez.framework.helper

object FormatHelper {

  def validEmail(email: String): Boolean =
    """(?=[^\s]+)(?=(\w+)@([\w\.]+))""".r.findFirstIn(email).nonEmpty

}
