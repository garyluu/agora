package org.broadinstitute.dsde.agora.server.webservice.validation

import org.broadinstitute.dsde.agora.server.model.{AgoraEntityType, AgoraProjectionDefaults, AgoraEntity}

object AgoraValidation {
  // TODO for DSDEEPB-457: validate that namespace meets DNS requirements shown here: https://cloud.google.com/storage/docs/bucket-naming#requirements
  def validateMetadata(entity: AgoraEntity): AgoraValidation = {
    var validation = AgoraValidation()
    if (!entity.namespace.exists(_.trim.nonEmpty)) {
      validation = validation.addError("Namespace is required")
    }
    if (!entity.name.exists(_.trim.nonEmpty)) {
      validation = validation.addError("Name is required")
    }
    if (!entity.synopsis.forall(_.length() <= 80)) {
      validation = validation.addError("Synopsis must be 80 characters or less")
    }
    validation
  }

  def validateEntityType(agoraEntityType: Option[AgoraEntityType.EntityType],
                         path: String): AgoraValidation = {
    agoraEntityType match {
      case Some(entityType) =>
        if (!AgoraEntityType.byPath(path).contains(entityType))
          AgoraValidation().addError(s"You can't perform operation for entity type $entityType at path /$path.")
        else AgoraValidation()
      case None => AgoraValidation()
    }
  }

  def validateParameters(includeFields: Seq[String],
                         excludeFields: Seq[String]): AgoraValidation = {
    val badExcludeFields = excludeFields.intersect(AgoraProjectionDefaults.RequiredProjectionFields)

    var validation = AgoraValidation(badExcludeFields.map(x => s"You can't exclude $x."))
    if (includeFields.nonEmpty && excludeFields.nonEmpty) {
      validation = validation.addError("You can't specify both exclude and include fields in the same request.")
    }
    validation
  }
}

case class AgoraValidation(messages: Seq[String] = Seq.empty[String]) {

  def addError(message: String):AgoraValidation = {
    copy(messages = messages :+ message)
  }

  def valid = messages.size == 0
}
