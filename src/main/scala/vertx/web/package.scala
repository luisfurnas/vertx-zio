package vertx

package object web {

  final case class ServerConfig(host: String, port: Int)

  val HTTP_HOST = "http_host"
  val HTTP_PORT = "http_port"

  val DEFAULT_HTTP_HOST = "localhost"
  val DEFAULT_HTTP_PORT = 8080
}
