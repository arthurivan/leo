package demo.provider

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class GrantReq(val userId: String, val code: String)
@Serializable
data class GrantResp(val success: Boolean, val bonusCents: Long)

fun main() {
  embeddedServer(io.ktor.server.netty.Netty, port = 8080) { module() }.start(wait = true)
}

fun Application.module() {
  install(ContentNegotiation) { json() }
  routing {
    post("/api/v1/promotions/grant") {
      val req = call.receive<GrantReq>()
      // Simplified business rule for demo
      val ok = req.code.startsWith("WELCOME")
      call.respond(HttpStatusCode.OK, GrantResp(success = ok, bonusCents = if (ok) 1000 else 0))
    }
  }
}
