package vertx.zio

import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.core.Vertx
import io.vertx.scala.core.http.HttpServer
import io.vertx.scala.ext.web.Router
import nequi.zio.logger._
import vertx.web._
import zio._

import scala.collection.mutable

trait VertXIO {
  trait VertX {
    val vertx: Vertx
    val deploymentId: String
  }
  trait Config { val config: JsonObject }
  trait VertXEnvironment extends VertX with Config

  type Environment = VertXEnvironment with Logger

  private final val serverMap: mutable.Map[String, HttpServer] = mutable.Map[String, HttpServer]()

  final def currentServer: URIO[Environment, Option[HttpServer]] = {
    for {
      deploymentId <- ZIO.access[Environment](_.deploymentId)
      server = serverMap.get(deploymentId)
    } yield server
  }

  final def updateServer(server: HttpServer): URIO[Environment, HttpServer] = {
    for {
      deploymentId <- ZIO.access[Environment](_.deploymentId)
      updatedServer <- ZIO.succeed(serverMap.getOrElseUpdate(deploymentId, server))
    } yield updatedServer
  }

  final def createHttpServer(router: Router): RIO[Environment, HttpServer] = {
    val serverConfig = for {
      config <- ZIO.access[Environment](_.config)
      host = config.getString(HTTP_HOST, DEFAULT_HTTP_HOST)
      port = config.getInteger(HTTP_PORT, DEFAULT_HTTP_PORT)
    } yield ServerConfig(host, port)

    for {
      vertx <- ZIO.access[Environment](_.vertx)
      config <- serverConfig
      ServerConfig(host, port) = config
      server <- RIO.fromFuture(_ => vertx
        .createHttpServer()
        .requestHandler(router)
        .listenFuture(port, host)
      ).onError(
        cause => error(s"Server failed to start on port: $port, b/c ${cause.prettyPrint}")
      )
      _ <- info(s"Server successfully started on port: $port")
      _ <- updateServer(server).ignore
    } yield server
  }

  final def stopServer: RIO[Environment, Unit] = {
    for {
      server <- currentServer.someOrFailException
      close <- ZIO.fromFuture(_ => server.closeFuture())
    } yield close
  }

  final def undeployVerticle: RIO[Environment, Unit] = {
    for {
      vertx <- ZIO.access[Environment](_.vertx)
      deploymentId <- ZIO.access[Environment](_.deploymentId)
      undeploy <- ZIO.fromFuture(_ => vertx.undeployFuture(deploymentId)).catchSome {
        case _: IllegalArgumentException => ZIO.unit
      }
    } yield undeploy
  }

  def runtime: Runtime[Environment]
}
