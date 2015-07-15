
package org.broadinstitute.dsde.agora.server.model

import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}
import cromwell.binding.WdlNamespace
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.business.MethodImportResolver
import org.joda.time.DateTime
import scalaz._
import scalaz.Scalaz._

import scala.annotation.meta.field

object AgoraEntityType extends Enumeration {
  def byPath(path: String): Seq[EntityType] = path match {
    case AgoraConfig.methodsRoute => Seq(Task, Workflow)
    case AgoraConfig.configurationsRoute => Seq(Configuration)
  }

  type EntityType = Value
  val Task = Value("Task")
  val Workflow = Value("Workflow")
  val Configuration = Value("Configuration")
}

object AgoraEntity {

  // ValidationNel is a Non-empty List (Nel) data structure. The left type
  // is the failure type. The right type is the success type.
  //
  // When completing a ValidationNel, both types must be provided.
  //
  // Examples to complete a ValidationNel[String, Int]:
  // Obj[SuccessType].successNel[FailureType] = 1.successNel[String]
  // Obj[FailureType].failureNel[SuccessType] = "fail".failureNel[Int]

  def validate(entity: AgoraEntity): ValidationNel[String, Boolean] = {

    def validateNamespace(namespace: String): ValidationNel[String, String] = {
      if (namespace.trim.nonEmpty) namespace.successNel[String]
      else "Namespace cannot be empty".failureNel[String]
    }

    def validateName(name: String): ValidationNel[String, String] = {
      if (name.trim.nonEmpty) name.successNel[String]
      else "Name cannot be empty".failureNel[String]
    }

    def validateSnapshotId(_id: Int): ValidationNel[String, Int] = {
      if (_id > 0) _id.successNel[String]
      else "SnapshotId must be greater than 0".failureNel[Int]
    }

    def validateSynopsis(synopsis: String): ValidationNel[String, String] = {
      if (synopsis.length <= 80) synopsis.successNel[String]
      else "Synopsis must be less than 80 chars".failureNel[String]
    }

    def validateDocumentation(doc: String): ValidationNel[String, String] = {
      if (doc.getBytes.size <= 10000) doc.successNel[String]
      else "Documentation must be less than 10kb".failureNel[String]
    }

    val namespace = entity.namespace match {
      case Some(n) => validateNamespace(n)
      case None => None.successNel[String]
    }

    val name = entity.name match {
      case Some(n) => validateName(n)
      case None => None.successNel[String]
    }

    val _id = entity.snapshotId match {
      case Some(id) => validateSnapshotId(id)
      case None => None.successNel[String]
    }

    val synopsis = entity.synopsis match {
      case Some(s) => validateSynopsis(s)
      case None => None.successNel[String]
    }

    val doc = entity.documentation match {
      case Some(docs) => validateDocumentation(docs)
      case None => None.successNel[String]
    }

    def doNothing = true

    // The |@| operator is a combinator that combines the validations into a single object
    // This allows all of the errors to be returned at once!
    (namespace |@| name |@| _id |@| synopsis |@| doc) {(namespace, name, _id, synopsis, doc) => doNothing }
  }

}

@ApiModel(value = "Agora Method")
case class AgoraEntity(@(ApiModelProperty@field)(required = false, value = "The namespace to which the method belongs")
                       namespace: Option[String] = None,

                       @(ApiModelProperty@field)(required = false, value = "The method name ")
                       name: Option[String] = None,

                       @(ApiModelProperty@field)(required = false, value = "The method snapshot id", hidden = true)
                       snapshotId: Option[Int] = None,

                       @(ApiModelProperty@field)(required = false, value = "A short description of the method")
                       synopsis: Option[String] = None,

                       @(ApiModelProperty@field)(required = false, value = "Method documentation")
                       documentation: Option[String] = None,

                       @(ApiModelProperty@field)(required = false, value = "User who owns this method in the methods repo", hidden = true)
                       owner: Option[String] = None,

                       @(ApiModelProperty@field)(required = false, value = "The date the method was inserted in the methods repo", hidden = true)
                       createDate: Option[DateTime] = None,

                       @(ApiModelProperty@field)(required = false, value = "The method payload")
                       payload: Option[String] = None,

                       @(ApiModelProperty@field)(required = false, value = "URI for method details", hidden = true)
                       url: Option[String] = None,

                       @(ApiModelProperty@field)(dataType = "string", required = false, value = "The type of the entity (Task, Workflow, Configuration)", allowableValues = "Task,Workflow,Configuration")
                       entityType: Option[AgoraEntityType.EntityType] = None) {

  AgoraEntity.validate(this) match {
    case Success(_) => this
    case Failure(errors) => throw new IllegalArgumentException(s"Entity is not valid: Errors: $errors")
  }

  def agoraUrl: String = {
    AgoraConfig.urlFromType(entityType) + namespace.get + "/" + name.get + "/" + snapshotId.get
  }

  def addUrl(): AgoraEntity = {
    copy(url = Option(agoraUrl))
  }
}



