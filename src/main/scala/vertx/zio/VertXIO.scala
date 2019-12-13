package vertx.zio

import io.vertx.core.{Vertx => JVertx}
import io.vertx.lang.scala.ScalaVerticle
import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.core.http.HttpServer
import io.vertx.scala.ext.web.Router
import nequi.zio.logger._
import vertx.web._
import zio._

trait VertXIO {
  self: ScalaVerticle =>

  type Config = JsonObject

  /**
   * VertX trait module.
   */
  trait VertX {
    val vertx: VertX.Service[Any]
  }

  /**
   * VertX module companion, containing everything necessary to create servers, deploy and undeploy verticles.
   * Also provides a deployment id, which identifies a deployed verticle, and is used to undeploy it,
   * and also provides a configuration object, to access properties like host and port.
   */
  object VertX {

    trait Service[R] {
      /**
       * Deployment ID of the deployed vertice, used for undeployment.
       */
      val deploymentId: UIO[String]
      /**
       * Configuration object.
       */
      val config: UIO[Config]

      /**
       * Creates an HTTP server.
       *
       * @return An effect with R requirements, that may fail with Throwable or succeed with an HttpServer.
       */
      def createHttpServer: RIO[R, HttpServer]

      /**
       * Undeploys the verticle given the deployment ID.
       *
       * @param deploymentId Deployment ID of the deployed verticle.
       * @return An effect with R requirements, that may fail with Throwable or succeed with Unit.
       */
      def undeployFuture(deploymentId: String): RIO[R, Unit]

      /**
       * Converts the VertX to it's Java version.
       * Used for integration with core VertX methods which require the Java version.
       *
       * @return An effect with R requirements, that may fail with Throwable or succeed with a Java version of VertX.
       */
      def asJava: RIO[R, JVertx]
    }

  }

  /**
   * VertX live module.
   * It uses verticle properties to fill the module properties, and uses the vertx instance for the methods.
   * This module acts like a proxy for VertX features.
   */
  trait VertXLive extends VertX {
    final val vertx: VertX.Service[Any] = new VertX.Service[Any] {
      val deploymentId: UIO[String] = UIO(self.deploymentID)
      val config: UIO[Config] = UIO(self.config)

      def createHttpServer: Task[HttpServer] = Task.effect(self.vertx.createHttpServer())

      def undeployFuture(deploymentId: String): Task[Unit] = Task.effect(self.vertx.undeployFuture(deploymentId))

      def asJava: Task[JVertx] = Task.effect(self.vertx.asJava.asInstanceOf[JVertx])
    }
  }

  /**
   * Describes the effect of creating an HTTP server, given a request handler.
   * Creates an HTTP server, attaches the router and then listens on the host and port, supplied by the config.
   * Success or failure are logged using the Logger provided by the environment.
   *
   * @param router VertX router to handle requests
   * @return An effect that requires a VertX with Logger, may fail with Throwable or succeed with an HttpServer.
   */
  final def createHttpServer(router: Router): RIO[VertX with Logger, HttpServer] = {
    val serverConfig = (vertx: VertX.Service[Any]) => for {
      config <- vertx.config
      host = config.getString(HTTP_HOST, DEFAULT_HTTP_HOST)
      port = config.getInteger(HTTP_PORT, DEFAULT_HTTP_PORT)
    } yield (host, port)

    for {
      vertx <- ZIO.access[VertX](_.vertx)
      (host, port) <- serverConfig(vertx)
      server <- vertx.createHttpServer
      serverWithRouter <- RIO.effect(server.requestHandler(router))
      startedServer <- RIO.fromFuture(_ => serverWithRouter.listenFuture(port, host))
        .onError(cause => error(s"Server failed to start on port: $port, b/c ${cause.prettyPrint}"))
      _ <- info(s"Server successfully started on port: $port")
    } yield startedServer
  }

  /**
   * Describes the effect of undeploying a verticle.
   * Fetches the vertx instance and the deployment id from the environment, then proceeds to undeploy the verticle.
   *
   * @return An effect that requires a VertX, may fail with Throwable or succeed with Unit.
   */
  final def undeployVerticle: RIO[VertX, Unit] = {
    for {
      vertx <- ZIO.access[VertX](_.vertx)
      deploymentId <- vertx.deploymentId
      undeploy <- vertx.undeployFuture(deploymentId)
    } yield undeploy
  }
}
