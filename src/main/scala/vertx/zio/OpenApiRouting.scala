package vertx.zio

import io.vertx.core.AsyncResult
import io.vertx.ext.web.api.contract.openapi3.{OpenAPI3RouterFactory => OA3RouterFactory}
import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.ext.web.Router
import zio._

trait OpenApiRouting extends VertXIO {
  self: ScalaVerticle =>

  trait OpenApi {
    val specPath: UIO[String]
  }

  def createRouterFactory: RIO[VertX with OpenApi, OA3RouterFactory] = {
    for {
      vertx <- ZIO.access[VertX](_.vertx)
      specPath <- ZIO.accessM[OpenApi](_.specPath)
      jVertx <- vertx.asJava
      routerFactory <- RIO.effectAsync[VertX with OpenApi, OA3RouterFactory](cb =>
        OA3RouterFactory.create(jVertx, specPath, (ar: AsyncResult[OA3RouterFactory]) => {
          if (ar.succeeded()) {
            cb(ZIO.succeed(ar.result()))
          } else {
            cb(ZIO.fail(ar.cause()))
          }
        })
      )
    } yield routerFactory
  }

  def createRouter(routerFactory: OA3RouterFactory): RIO[VertX, Router]
}