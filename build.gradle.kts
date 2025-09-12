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
  testImplementation("mysql:mysql-connector-java:8.0.33")
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
