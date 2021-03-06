package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.ActorSystem
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.exceptions.ValidationException
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.configurations.ConfigurationsService
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.scalatest.{DoNotDiscover, _}
import spray.http.StatusCodes._
import spray.httpx.unmarshalling._
import spray.routing.{Directives, ExceptionHandler, MalformedRequestContentRejection, RejectionHandler}
import spray.testkit.ScalatestRouteTest

import scala.concurrent.duration._

@DoNotDiscover
class ApiServiceSpec extends AgoraTestFixture with Directives with Suite with ScalatestRouteTest {

  // ScalatestRouteTest requires that it be mixed in to a thing that is of type Suite. So mix that in as well.
  // But both have run() methods, so we have to pick the right one. In other places, mixing in *Spec accomplishes this.
  override def run(testName: Option[String], args: Args): Status = super[ScalatestRouteTest].run(testName, args)

  implicit val routeTestTimeout = RouteTestTimeout(5.seconds)

  val wrapWithExceptionHandler = handleExceptions(ExceptionHandler {
    case e: IllegalArgumentException => complete(BadRequest, e.getMessage)
    case ve: ValidationException => complete(BadRequest, ve.getMessage)
  })

  val wrapWithRejectionHandler = handleRejections(RejectionHandler {
    case MalformedRequestContentRejection(message, cause) :: _ => complete(BadRequest, message)
  })

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }


  abstract class StatusService(pds: PermissionsDataSource) extends AgoraService(pds) {
    override def path = "/status"
    override def statusRoute = super.statusRoute
  }

  val methodsService = new MethodsService(permsDataSource) with ActorRefFactoryContext
  val configurationsService = new ConfigurationsService(permsDataSource) with ActorRefFactoryContext
  val apiStatusService = new StatusService(permsDataSource) with ActorRefFactoryContext

  def handleError[T](deserialized: Deserialized[T], assertions: (T) => Unit) = {
    if (status.isSuccess) {
      if (deserialized.isRight) assertions(deserialized.right.get) else failTest(deserialized.left.get.toString)
    } else {
      failTest(response.message.toString)
    }
  }

  def uriEncode(uri: String): String = {
    java.net.URLEncoder.encode(uri, "UTF-8")
  }

  def brief(entities: Seq[AgoraEntity]): Seq[AgoraEntity] = {
    entities.map(entity =>
      AgoraEntity(namespace = entity.namespace,
        name = entity.name,
        snapshotId = entity.snapshotId,
        snapshotComment = entity.snapshotComment,
        synopsis = entity.synopsis,
        owner = entity.owner,
        url = entity.url,
        createDate = entity.createDate,
        entityType = entity.entityType,
        id = entity.id
      )
    )
  }

  def brief(agoraEntity: AgoraEntity): AgoraEntity = {
    brief(Seq(agoraEntity)).head
  }

  def excludeProjection(entities: Seq[AgoraEntity]): Seq[AgoraEntity] = {
    entities.map(entity =>
      AgoraEntity(
        namespace = entity.namespace,
        name = entity.name,
        snapshotId = entity.snapshotId,
        owner = entity.owner,
        url = Option(entity.agoraUrl),
        entityType = entity.entityType
      )
    )
  }

  def includeProjection(entities: Seq[AgoraEntity]): Seq[AgoraEntity] = {
    entities.map(entity =>
      AgoraEntity(
        namespace = entity.namespace,
        name = entity.name,
        snapshotId = entity.snapshotId,
        snapshotComment = entity.snapshotComment,
        entityType = entity.entityType,
        url = Option(entity.agoraUrl)
      )
    )
  }
}

