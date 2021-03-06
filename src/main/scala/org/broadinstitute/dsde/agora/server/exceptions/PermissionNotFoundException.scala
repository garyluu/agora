package org.broadinstitute.dsde.agora.server.exceptions

import com.typesafe.scalalogging.LazyLogging

case class PermissionNotFoundException(message: String = "Could not find permissions.", ex: Throwable = null) extends Exception with LazyLogging {
  override def getMessage: String = message
}
