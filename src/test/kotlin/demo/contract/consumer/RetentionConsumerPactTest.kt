package demo.contract.consumer

import au.com.dius.pact.consumer.junit5.*
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.PactSpecVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo

@ExtendWith(PactConsumerTestExt::class)
class RetentionConsumerPactTest {

  @Pact(consumer = "retention-service", provider = "promotion-provider")
  fun createPact(builder: PactDslWithProvider): RequestResponsePact =
    builder
      .given("User eligible for welcome bonus")
      .uponReceiving("Grant promotion")
        .path("/api/v1/promotions/grant")
        .method("POST")
        .headers("Content-Type", "application/json")
        .body("""{
  "userId": "u-123",
  "code": "WELCOME10"
}""")
      .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/json"))
        .body("""{
  "success": true,
  "bonusCents": 1000
}""")
      .toPact()

  @Test
  @PactTestFor(providerName = "promotion-provider", pactVersion = PactSpecVersion.V3)
  fun testGrantPact(mockServer: MockServer) {
    given()
      .baseUri(mockServer.getUrl())
      .contentType("application/json")
      .body("""{ "userId": "u-123", "code": "WELCOME10" }""")
    .`when`()
      .post("/api/v1/promotions/grant")
    .then()
      .statusCode(200)
      .body("success", equalTo(true))
      .body("bonusCents", equalTo(1000))
  }
}
