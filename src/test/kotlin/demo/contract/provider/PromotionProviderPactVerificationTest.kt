package demo.contract.provider

import au.com.dius.pact.provider.junit5.*
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import au.com.dius.pact.provider.junitsupport.State
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import demo.provider.module

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Provider("promotion-provider")
@PactFolder("pact")
class PromotionProviderPactVerificationTest {
  private lateinit var server: ApplicationEngine

  @BeforeAll
  fun startProvider() {
    server = embeddedServer(Netty, port = 8081) {
      module() // reuse routes from main
    }.start()
    System.setProperty("pact.verifier.publishResults", "false")
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider::class)
  fun pactVerificationTestTemplate(context: PactVerificationContext) {
    context.verifyInteraction()
  }

  @BeforeEach
  fun beforeEach(context: PactVerificationContext) {
    context.target = HttpTestTarget("localhost", 8081)
  }

  @State("User eligible for welcome bonus")
  fun userEligibleForWelcomeBonus() {
    // Set up test state - in this demo, no specific setup needed
    // In real scenario, you might prepare database state, etc.
  }
}
