
package org.broadinstitute.dsde.agora.server.model

import org.broadinstitute.dsde.agora.server.exceptions.AgoraException
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions, EntityAccessControl}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}
import spray.json.{JsArray, JsString, _}

import org.broadinstitute.dsde.rawls.model.MethodConfiguration
import AgoraEntity.AttributeStringFormat
import org.broadinstitute.dsde.rawls.model.WorkspaceJsonSupport.MethodStoreMethodFormat
import org.broadinstitute.dsde.rawls.model.{MethodRepoMethod, AttributeString}

import scala.language.implicitConversions

object AgoraApiJsonSupport extends DefaultJsonProtocol {

  implicit def stringToDateTime(str: String): DateTime = parserISO.parseDateTime(str)

  implicit def stringToType(str: String): AgoraEntityType.EntityType = AgoraEntityType.withName(str)

  implicit object ObjectIdJsonFormat extends RootJsonFormat[ObjectId] {
    override def write(obj: ObjectId) = {
      JsObject("$oid" -> JsString(obj.toHexString))
    }

    override def read(json: JsValue): ObjectId = {
      new ObjectId(json.asJsObject.fields("$oid").convertTo[String])
    }
  }
  
  implicit object DateJsonFormat extends RootJsonFormat[DateTime] {
    override def write(obj: DateTime) = {
      JsString(parserISO.print(obj))
    }

    override def read(json: JsValue): DateTime = json match {
      case JsString(s) => parserISO.parseDateTime(s)
      case _ => throw new DeserializationException("only string supported")
    }
  }

  implicit object AgoraEntityTypeFormat extends RootJsonFormat[AgoraEntityType.EntityType] {
    override def write(obj: AgoraEntityType.EntityType): JsValue = JsString(obj.toString)

    override def read(value: JsValue): AgoraEntityType.EntityType = value match {
      case JsString(name) => AgoraEntityType.withName(name)
      case _ => throw new DeserializationException("only string supported")
    }
  }

  implicit object MethodConfigurationFormat extends RootJsonFormat[MethodConfiguration] {
    override def write(obj: MethodConfiguration): JsValue = {
      jsonFormat10(MethodConfiguration).write(obj)
    }

    // Mirror the default values in the MethodConfiguration case class - spray-json does not know how to use them
    // https://stackoverflow.com/questions/15740925/what-is-a-good-way-to-handle-default-values-with-spray-json
    override def read(json: JsValue): MethodConfiguration = {
      // get the fields once so we don't do extra work
      val fields = json.asJsObject.fields

      // check required keys
      val requiredKeys = Set("namespace","name","rootEntityType","prerequisites","inputs","outputs","methodRepoMethod")
      val missingKeys = requiredKeys diff fields.keySet
      if (missingKeys.nonEmpty)
        throw DeserializationException(s"Failed to read field(s) [${missingKeys.mkString(",")}] from method configuration")

      MethodConfiguration(
        namespace = fields("namespace").convertTo[String],
        name = fields("name").convertTo[String],
        rootEntityType = fields("rootEntityType").convertTo[String],
        prerequisites = fields("prerequisites").convertTo[Map[String, AttributeString]],
        inputs = fields("inputs").convertTo[Map[String, AttributeString]],
        outputs = fields("outputs").convertTo[Map[String, AttributeString]],
        methodRepoMethod = fields("methodRepoMethod").convertTo[MethodRepoMethod],

        methodConfigVersion = fields.getOrElse("methodConfigVersion",JsNumber(1)).convertTo[Int],
        deleted = fields.getOrElse("deleted",JsBoolean(false)).convertTo[Boolean],
        deletedDate = fields.get("deletedDate") map (_.convertTo[DateTime])
      )
    }
  }

  implicit object AgoraEntityFormat extends RootJsonFormat[AgoraEntity] {
  
    override def write(entity: AgoraEntity) = {
      var map = Map.empty[String, JsValue]
      if (entity.namespace.nonEmpty) map += ("namespace" -> JsString(entity.namespace.get))
      if (entity.name.nonEmpty) map += ("name" -> JsString(entity.name.get))
      if (entity.snapshotId.nonEmpty) map += ("snapshotId" -> JsNumber(entity.snapshotId.get))
      if (entity.snapshotComment.nonEmpty) map += ("snapshotComment" -> JsString(entity.snapshotComment.get))
      if (entity.synopsis.nonEmpty) map += ("synopsis" -> JsString(entity.synopsis.get))
      if (entity.documentation.nonEmpty) map += ("documentation" -> JsString(entity.documentation.get))
      if (entity.owner.nonEmpty) map += ("owner" -> JsString(entity.owner.get))
      if (entity.managers.nonEmpty) map += ("managers" -> JsArray(entity.managers.map(JsString(_)).toVector))
      if (entity.createDate.nonEmpty) map += ("createDate" -> entity.createDate.get.toJson)
      if (entity.payload.nonEmpty) map += ("payload" -> JsString(entity.payload.get))
      if (entity.payloadObject.nonEmpty) map += ("payloadObject" -> entity.payloadObject.get.toJson)
      if (entity.url.nonEmpty) map += ("url" -> JsString(entity.url.get))
      if (entity.entityType.nonEmpty) map += ("entityType" -> entity.entityType.get.toJson)
      if (entity.id.nonEmpty) map += ("_id" -> entity.id.get.toJson)
      if (entity.methodId.nonEmpty) map += ("methodId" -> entity.methodId.get.toJson)
      if (entity.method.nonEmpty) map += ("method" -> entity.method.get.toJson)
      if (entity.public.nonEmpty) map += ("public" -> entity.public.get.toJson)
      JsObject(map)
    }

    override def read(json: JsValue): AgoraEntity = {
      val jsObject = json.asJsObject
      val namespace = stringOrNone(jsObject, "namespace")
      val name = stringOrNone(jsObject, "name")
      val snapshotId = if (jsObject.getFields("snapshotId").nonEmpty) jsObject.fields("snapshotId").convertTo[Option[Int]] else None
      val snapshotComment = stringOrNone(jsObject, "snapshotComment")
      val synopsis = stringOrNone(jsObject, "synopsis")
      val documentation = stringOrNone(jsObject, "documentation")
      val owner = stringOrNone(jsObject, "owner")
      val createDate = if (jsObject.getFields("createDate").nonEmpty) jsObject.fields("createDate").convertTo[Option[DateTime]] else None
      val payload = stringOrNone(jsObject, "payload")
      val payloadObject = if (jsObject.getFields("payloadObject").nonEmpty) jsObject.fields("payloadObject").convertTo[Option[MethodConfiguration]] else None
      val url = stringOrNone(jsObject, "url")
      val entityType = if (jsObject.getFields("entityType").nonEmpty) jsObject.fields("entityType").convertTo[Option[AgoraEntityType.EntityType]] else None
      val id = if (jsObject.getFields("_id").nonEmpty) jsObject.fields("_id").convertTo[Option[ObjectId]] else None
      val methodId = if (jsObject.getFields("methodId").nonEmpty) jsObject.fields("methodId").convertTo[Option[ObjectId]] else None
      val method = if (jsObject.getFields("method").nonEmpty) jsObject.fields("method").convertTo[Option[AgoraEntity]] else None

      val entity = AgoraEntity(namespace = namespace,
                               name = name,
                               snapshotId = snapshotId,
                               snapshotComment = snapshotComment,
                               synopsis = synopsis,
                               documentation = documentation,
                               owner = owner,
                               createDate = createDate,
                               payload = payload,
                               payloadObject = payloadObject,
                               url = url,
                               entityType = entityType,
                               id = id,
                               methodId = methodId,
                               method = method)
      entity
    }
  }

  implicit val MethodDefinitionProtocol = jsonFormat8(MethodDefinition.apply)

  implicit object AgoraPermissionsFormat extends RootJsonFormat[AgoraPermissions] {
    override def write(obj: AgoraPermissions): JsArray =
      JsArray(obj.toListOfStrings.map(JsString.apply))

    override def read(json: JsValue): AgoraPermissions = json match {
      case array: JsArray =>
        val listOfStrings = array.convertTo[Seq[String]]
        AgoraPermissions(listOfStrings)
      case _ => throw new DeserializationException("unsupported AgoraPermission")
    }
  }

  implicit object AgoraExceptionFormat extends RootJsonFormat[AgoraException] {
    override def write(obj: AgoraException): JsObject =
      JsObject("code" -> JsNumber(obj.statusCode.intValue),
               "message" -> JsString(obj.message)
      )

    override def read(json: JsValue): AgoraException = json match {
      case _ => throw new DeserializationException("Cannot read AgoraExceptions in JSON")
    }
  }

  def methodRef(payload: String): AgoraEntity = {
    val json = payload.parseJson
    val refJson = json.asJsObject.fields("methodRepoMethod").asJsObject
    val namespace = refJson.fields("methodNamespace").convertTo[String]
    val name = refJson.fields("methodName").convertTo[String]
    val snapshotId = refJson.fields("methodVersion").convertTo[Int]
    AgoraEntity(namespace = Option(namespace), name = Option(name), snapshotId = Option(snapshotId))
  }

  private def stringOrNone(json: JsObject, key: String): Option[String] = {
    if (json.getFields(key).nonEmpty) json.fields(key).convertTo[Option[String]] else None
  }
  
  private val parserISO: DateTimeFormatter = {
    ISODateTimeFormat.dateTimeNoMillis()
  }
  
  implicit val AgoraEntityProjectionFormat = jsonFormat2(AgoraEntityProjection.apply)

  implicit val AccessControlFormat = jsonFormat2(AccessControl.apply)

  implicit val AccessControlPairFormat = jsonFormat3(EntityAccessControl)

  implicit object AgoraStatusFormat extends RootJsonFormat[AgoraStatus] {
    override def write(obj: AgoraStatus): JsObject = {
      JsObject("status" -> JsString(if (obj.up) "up" else "down"),
        "message" -> JsArray(obj.messages.map(JsString(_)).toVector)
      )
    }

    override def read(json: JsValue): AgoraStatus = json match {
      case _ => throw new DeserializationException("Cannot read AgoraStatus in JSON")
    }
  }
}
