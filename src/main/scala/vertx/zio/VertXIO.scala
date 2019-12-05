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

  trait VertX {
    val vertx: VertX.Service[Any]
  }

  object VertX {

    trait Service[R] {
      val deploymentId: UIO[String]
      val config: UIO[Config]

      def createHttpServer: RIO[R, HttpServer]

      def undeployFuture(deploymentId: String): RIO[R, Unit]

      def asJava: RIO[R, JVertx]
    }

  }

  trait VertXLive extends VertX {
    final val vertx: VertX.Service[Any] = new VertX.Service[Any] {
      val deploymentId: UIO[String] = UIO(self.deploymentID)
      val config: UIO[Config] = UIO(self.config)

      def createHttpServer: Task[HttpServer] = Task.effect(self.vertx.createHttpServer())

      def undeployFuture(deploymentId: String): Task[Unit] = Task.effect(self.vertx.undeployFuture(deploymentId))

      def asJava: Task[JVertx] = Task.effect(self.vertx.asJava.asInstanceOf[JVertx])
    }
  }

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

  final def undeployVerticle: RIO[VertX, Unit] = {
    for {
      vertx <- ZIO.access[VertX](_.vertx)
      deploymentId <- vertx.deploymentId
      undeploy <- vertx.undeployFuture(deploymentId)
    } yield undeploy
  }
}
