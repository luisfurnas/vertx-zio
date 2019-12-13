package vertx.zio

import io.vertx.core.AsyncResult
import io.vertx.ext.web.api.contract.openapi3.{OpenAPI3RouterFactory => OA3RouterFactory}
import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.ext.web.Router
import zio._

trait OpenApiRouting extends VertXIO {
  self: ScalaVerticle =>

  /**
   * OpenAPI module.
   */
  trait OpenApi {
    val specPath: UIO[String]
  }

  /**
   * Describes the effect of creating a router factory.
   * Gets the Java version of the VertX and the spec path, and creates an Open API 3 router factory.
   *
   * Since the creation of the factory requires an async result callback, `effectAsync` is used to tranform it into a ZIO effect.
   * This way, the result is referentially transparent and easier to reason with. Also it can be combined with other effects.
   *
   * @return An effect that requires a VertX with OpenApi, may fail with Throwable or succeed with an OA3RouterFactory.
   */
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

  /**
   * Describes the effect of creating a router, given an Open API 3 router factory.
   * Must be implemented by the implementer of this trait.
   * This way it's easier to create a router with custom options, like security handlers for example.
   *
   * @param routerFactory Router factory to use for the creation of the router.
   * @return An effect that requires a VertX, may fail with Throwable or succeed with a Router.
   */
  def createRouter(routerFactory: OA3RouterFactory): RIO[VertX, Router]
}