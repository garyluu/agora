package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Props
import com.gettyimages.spray.swagger.SwaggerHttpService
import com.typesafe.scalalogging.slf4j.LazyLogging
import com.wordnik.swagger.model.ApiInfo
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.webservice.configurations.ConfigurationsService
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import spray.http.StatusCodes._
import spray.routing._
import spray.util.LoggingContext

import scala.reflect.runtime.universe._

object ApiServiceActor {
  def props: Props = Props(classOf[ApiServiceActor])
}

class ApiServiceActor extends HttpServiceActor with LazyLogging {

  override def actorRefFactory = context

  trait ActorRefFactoryContext {
    def actorRefFactory = context
  }

  val methodsService = new MethodsService() with ActorRefFactoryContext
  val configurationsService = new ConfigurationsService() with ActorRefFactoryContext


  def possibleRoutes = methodsService.routes ~ configurationsService.routes ~
    get {
      pathSingleSlash {
        getFromResource("swagger/index.html")
      } ~ getFromResourceDirectory("swagger/") ~ getFromResourceDirectory("META-INF/resources/webjars/swagger-ui/2.1.2/")
    }

  def receive = runRoute(possibleRoutes)

  implicit def routeExceptionHandler(implicit log: LoggingContext) =
    ExceptionHandler {
      case e: IllegalArgumentException => complete(BadRequest, e.getMessage)
      case ex: Throwable => {
        logger.error(ex.getMessage)
        complete(InternalServerError, s"Something went wrong, but the error is unspecified.")
      }
    }

  implicit val routeRejectionHandler =
    RejectionHandler {
      case MalformedRequestContentRejection(message, cause) :: _ => complete(BadRequest, message)
  }

  val swaggerService = new SwaggerHttpService {
    override def apiTypes = Seq(typeOf[MethodsService], typeOf[ConfigurationsService])
    override def apiVersion = AgoraConfig.SwaggerConfig.apiVersion
    override def baseUrl = AgoraConfig.SwaggerConfig.baseUrl
    override def docsPath = AgoraConfig.SwaggerConfig.apiDocs
    override def actorRefFactory = context
    override def swaggerVersion = AgoraConfig.SwaggerConfig.swaggerVersion

    override def apiInfo = Some(
      new ApiInfo(
        AgoraConfig.SwaggerConfig.info,
        AgoraConfig.SwaggerConfig.description,
        AgoraConfig.SwaggerConfig.termsOfServiceUrl,
        AgoraConfig.SwaggerConfig.contact,
        AgoraConfig.SwaggerConfig.license,
        AgoraConfig.SwaggerConfig.licenseUrl)
    )
  }
}
