/*
 * Copyright 2014 Typesafe Inc. <http://www.typesafe.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.typesafe.scalalogging
package slf4j

import com.ecfront.ez.framework.core.EZ
import org.slf4j.{Logger => Underlying}

import scala.language.experimental.macros

/**
  * Companion for [[Logger]].
  */
object Logger {

  /**
    * Create a [[Logger]] wrapping the given underlying `org.slf4j.Logger`.
    */
  def apply(underlying: Underlying): Logger =
  new Logger(underlying)
}

/**
  * Implementation for a performant logger based on macros and an underlying `org.slf4j.Logger`.
  */
final class Logger private(val underlying: Underlying) extends BaseLogger {

  // Error

  override def error(message: String): Unit = _error(packageContext(message))

  override def error(message: String, cause: Throwable): Unit = _error(packageContext(message), cause)

  override def error(message: String, args: AnyRef*): Unit = _error(packageContext(message), args)

  // Warn

  override def warn(message: String): Unit = _warn(packageContext(message))

  override def warn(message: String, cause: Throwable): Unit = _warn(packageContext(message), cause)

  override def warn(message: String, args: AnyRef*): Unit = _warn(packageContext(message), args)

  // Info

  override def info(message: String): Unit = _info(packageContext(message))

  override def info(message: String, cause: Throwable): Unit = _info(packageContext(message), cause)

  override def info(message: String, args: AnyRef*): Unit = _info(packageContext(message), args)

  // Debug

  override def debug(message: String): Unit = _debug(packageContext(message))

  override def debug(message: String, cause: Throwable): Unit = _debug(packageContext(message), cause)

  override def debug(message: String, args: AnyRef*): Unit = _debug(packageContext(message), args)

  // Trace

  override def trace(message: String): Unit = _trace(packageContext(message))

  override def trace(message: String, cause: Throwable): Unit = _trace(packageContext(message), cause)

  override def trace(message: String, args: AnyRef*): Unit = _trace(packageContext(message), args)


  private def packageContext(message: String): String = {
    val accCode = EZ.context.optAccCode
    val traceInfo =
      if (EZ.context.trace == null || EZ.context.trace.isEmpty) {
        ""
      } else {
        EZ.context.trace.map(i => i._1 + ":" + i._2).mkString("[", ";", "]")
      }
    s"$accCode# $traceInfo $message"
  }

  private def _error(message: String): Unit = macro LoggerMacro.errorMessage

  private def _error(message: String, cause: Throwable): Unit = macro LoggerMacro.errorMessageCause

  private def _error(message: String, args: AnyRef*): Unit = macro LoggerMacro.errorMessageArgs

  private def _warn(message: String): Unit = macro LoggerMacro.warnMessage

  private def _warn(message: String, cause: Throwable): Unit = macro LoggerMacro.warnMessageCause

  private def _warn(message: String, args: AnyRef*): Unit = macro LoggerMacro.warnMessageArgs

  private def _info(message: String): Unit = macro LoggerMacro.infoMessage

  private def _info(message: String, cause: Throwable): Unit = macro LoggerMacro.infoMessageCause

  private def _info(message: String, args: AnyRef*): Unit = macro LoggerMacro.infoMessageArgs

  private def _debug(message: String): Unit = macro LoggerMacro.debugMessage

  private def _debug(message: String, cause: Throwable): Unit = macro LoggerMacro.debugMessageCause

  private def _debug(message: String, args: AnyRef*): Unit = macro LoggerMacro.debugMessageArgs

  private def _trace(message: String): Unit = macro LoggerMacro.traceMessage

  private def _trace(message: String, cause: Throwable): Unit = macro LoggerMacro.traceMessageCause

  private def _trace(message: String, args: AnyRef*): Unit = macro LoggerMacro.traceMessageArgs


}
