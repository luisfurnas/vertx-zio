package vertx.zio

import io.vertx.scala.ext.web.Router
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory
import io.vertx.core.{Vertx => JVertx}
import zio._

trait OpenApiRouting extends VertXIO {
  trait OpenApi { val specPath: String }

  def createRouterFactory: RIO[Environment with OpenApi, OpenAPI3RouterFactory] = {
    ZIO.environment[Environment with OpenApi] flatMap { env =>
      val jVertx = env.vertx.asJava.asInstanceOf[JVertx]
      RIO.effectAsync[Environment with OpenApi, OpenAPI3RouterFactory](cb =>
        OpenAPI3RouterFactory.create(jVertx, env.specPath, ar => {
          if (ar.succeeded()) {
            cb(ZIO.succeed(ar.result()))
          } else {
            cb(ZIO.fail(ar.cause()))
          }
        })
      )
    }
  }

  def createRouter(routerFactory: OpenAPI3RouterFactory): RIO[Environment, Router]

  override def runtime: Runtime[Environment with OpenApi]
}