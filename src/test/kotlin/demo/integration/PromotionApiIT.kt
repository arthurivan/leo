package demo.integration

import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.containers.MySQLContainer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.flywaydb.core.Flyway

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PromotionApiIT {
  companion object {
    @Container
    @JvmStatic
    private val mysql = MySQLContainer("mysql:8.0")
      .withDatabaseName("loyalty")
      .withUsername("test")
      .withPassword("test")
  }

  private lateinit var wiremock: WireMockServer

  @BeforeAll
  fun setUp() {
    // Start WireMock on a random port and load stubs from resources
    wiremock = WireMockServer(WireMockConfiguration.options().dynamicPort().usingFilesUnderClasspath("wiremock"))
    wiremock.start()

    // Run Flyway migration against the containerized DB
    Flyway.configure()
      .dataSource(mysql.jdbcUrl, mysql.username, mysql.password)
      .locations("classpath:db/migration")
      .load()
      .migrate()

    // In a real suite you would start your service pointing it to mysql & wiremock URLs
    // For demo we hit the embedded Ktor ProviderApp if it runs separately, or adapt baseUri
  }

  @AfterAll
  fun tearDown() { wiremock.stop() }

  @Test
  fun `granting a promo returns success true for WELCOME codes`() {
    val body = """{ "userId": "u-123", "code": "WELCOME10" }"""

    given()
      .baseUri("http://localhost:8080") // provider app base URL
      .contentType("application/json")
      .body(body)
    .`when`()
      .post("/api/v1/promotions/grant")
    .then()
      .statusCode(200)
      .body("success", equalTo(true))
  }
}
