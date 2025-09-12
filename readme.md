# Kotlin QA Starter Template (LeoVegas‑style Platform)

A pragmatic template for testing Kotlin/Java microservice APIs the way a large iGaming platform (hundreds of services, MySQL, CI/CD, contracts) would expect.

**What you get**

* JUnit 5 + Kotest assertions
* REST-assured (HTTP) and/or Ktor client
* Testcontainers (MySQL) + Flyway migrations
* WireMock (stub external deps)
* Pact (consumer + provider) for contract testing
* Gradle config + GitHub Actions CI

---

## Repository layout

```
leovegas-kotlin-qa-template/
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradle.properties
├─ README.md
├─ .github/
│  └─ workflows/
│     └─ ci.yml
├─ src/
│  ├─ main/
│  │  └─ kotlin/
│  │     └─ demo/provider/ProviderApp.kt         # Minimal provider (Ktor) to run provider contract tests
│  └─ test/
│     ├─ kotlin/
│     │  ├─ demo/integration/PromotionApiIT.kt   # Integration test: REST-assured + Testcontainers MySQL + WireMock
│     │  ├─ demo/contract/consumer/RetentionConsumerPactTest.kt
│     │  └─ demo/contract/provider/PromotionProviderPactVerificationTest.kt
│     └─ resources/
│        ├─ db/migration/V1__init.sql            # Flyway migration for test DB
│        └─ wiremock/mappings/grant_kyc_ok.json  # Example WireMock stub
└─ pact/                                         # Where generated pacts are written (consumer tests)
```

---

## Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
plugins {
  kotlin("jvm") version "1.9.24"
  id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24"
}

group = "demo"
version = "0.1.0"

repositories { mavenCentral() }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

dependencies {
  // Test runners & assertions
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
  testImplementation("io.kotest:kotest-assertions-core:5.9.1")

  // HTTP clients
  testImplementation("io.rest-assured:rest-assured:5.4.0")
  testImplementation("io.ktor:ktor-client-core:2.3.12")
  testImplementation("io.ktor:ktor-client-java:2.3.12")
  testImplementation("io.ktor:ktor-client-content-negotiation:2.3.12")
  testImplementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

  // Mocking & stubbing
  testImplementation("io.mockk:mockk:1.13.12")
  testImplementation("org.wiremock:wiremock-standalone:3.9.1")

  // Testcontainers (MySQL + JUnit 5)
  testImplementation("org.testcontainers:junit-jupiter:1.20.1")
  testImplementation("org.testcontainers:mysql:1.20.1")
  testImplementation("mysql:mysql-connector-java:8.4.0")
  testImplementation("org.flywaydb:flyway-core:10.17.2")

  // Pact (consumer & provider)
  testImplementation("au.com.dius.pact.consumer:junit5:4.6.14")
  testImplementation("au.com.dius.pact.provider:junit5:4.6.14")

  // Ktor server for a minimal provider under test (for provider verification)
  implementation("io.ktor:ktor-server-core:2.3.12")
  implementation("io.ktor:ktor-server-netty:2.3.12")
  implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
  implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
}

tasks.test {
  useJUnitPlatform()
  // Useful when running in CI with Testcontainers
  systemProperty("pact.writer.overwrite", "true")
  systemProperty("pact.rootDir", project.rootProject.file("pact").absolutePath)
}
```

```kotlin
// settings.gradle.kts
rootProject.name = "leovegas-kotlin-qa-template"
```

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
kotlin.code.style=official
```

---

## Minimal provider (Ktor) for provider contract verification

```kotlin
// src/main/kotlin/demo/provider/ProviderApp.kt
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
```

---

## Flyway migration (used during tests with Testcontainers)

```sql
-- src/test/resources/db/migration/V1__init.sql
CREATE TABLE IF NOT EXISTS promotion_grants (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL,
  code VARCHAR(64) NOT NULL,
  bonus_cents BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## WireMock stub (external dep simulation)

```json
// src/test/resources/wiremock/mappings/grant_kyc_ok.json
{
  "request": {
    "method": "POST",
    "url": "/kyc/verify",
    "bodyPatterns": [ { "matchesJsonPath": "$.userId" } ]
  },
  "response": {
    "status": 200,
    "jsonBody": { "kyc": "ok" },
    "headers": { "Content-Type": "application/json" }
  }
}
```

---

## Integration test: REST-assured + Testcontainers + WireMock

```kotlin
// src/test/kotlin/demo/integration/PromotionApiIT.kt
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
    private val mysql = MySQLContainer<Nothing>("mysql:8.0")
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
```

> Tip: In a real project you’d launch your service under test in-process (SpringBootTest/Ktor TestApplication) or via Docker Compose from the test, passing `mysql.jdbcUrl` + `wiremock.baseUrl()` as env vars.

---

## Pact – Consumer test (Retention → Promotions)

```kotlin
// src/test/kotlin/demo/contract/consumer/RetentionConsumerPactTest.kt
package demo.contract.consumer

import au.com.dius.pact.consumer.junit5.*
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
        .body("""{\n  \"userId\": \"u-123\",\n  \"code\": \"WELCOME10\"\n}""")
      .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/json"))
        .body("""{\n  \"success\": true,\n  \"bonusCents\": 1000\n}""")
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
```

This test writes a pact file under `pact/retention-service-promotion-provider.json`.

---

## Pact – Provider verification (run against our Ktor provider)

```kotlin
// src/test/kotlin/demo/contract/provider/PromotionProviderPactVerificationTest.kt
package demo.contract.provider

import au.com.dius.pact.provider.junit5.*
import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.Provider
import au.com.dius.pact.provider.junit5.PactFolder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import io.ktor.server.engine.*
import demo.provider.module

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Provider("promotion-provider")
@PactFolder("pact")
class PromotionProviderPactVerificationTest {
  private lateinit var server: ApplicationEngine

  @BeforeAll
  fun startProvider() {
    server = io.ktor.server.netty.embeddedServer(io.ktor.server.netty.Netty, port = 8081) {
      module() // reuse routes from main
    }.start()
    System.setProperty("pact.verifier.publishResults", "false")
  }

  @au.com.dius.pact.provider.junit5.PactTestFor(target = PactVerificationTarget::class)
  fun target(context: PactVerificationContext) {
    context.target = HttpTestTarget("localhost", 8081, "/")
  }
}
```

Run order:

1. Consumer test generates pact JSON.
2. Provider test boots Ktor and verifies compatibility.

---

## GitHub Actions CI

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
      - uses: gradle/gradle-build-action@v3
        with:
          gradle-version: wrapper
      - name: Run tests
        env:
          TESTCONTAINERS_RYUK_DISABLED: "false"
          TESTCONTAINERS_CHECKS_DISABLE: "true"
        run: |
          ./gradlew --no-daemon test
      - name: Publish test reports (artifact)
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: build/reports/tests/test
```

---

## README (quick start)

````md
# Kotlin QA Template

## Prereqs
- JDK 21, Docker (for Testcontainers)

## Run tests
```bash
./gradlew test
````

## What to customize

* Replace demo routes with your service URLs or start your app in test.
* Add more Flyway migrations and DB helpers.
* Add more Pact interactions (edge cases, 4xx/5xx).
* WireMock: add failure cases to test resilience.

## Why this stack

* **Integration realism**: Testcontainers spins real MySQL.
* **Contract safety**: Pact keeps consumer/provider in sync.
* **Extensibility**: Ktor or Spring can be swapped in.

```

---

### Notes for a LeoVegas‑style environment
- Keep **contracts per domain** (e.g., Retention ↔ Promotions, Payments ↔ Providers).
- Run **nightly full regression** in CI; run **tagged smoke** on PRs.
- Track flaky tests; add **idempotent fixtures** and **unique test data** per run.
- For scale, shard tests across CI executors; cache Gradle.

```

