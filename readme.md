# Kotlin QA

A pragmatic template for testing Kotlin/Java microservice APIs the way a large iGaming platform (hundreds of services, MySQL, CI/CD, contracts) would expect.

### Prerequisites

- **Java 21**
- **Docker**

### Run the Application

```bash
# Start the provider application
./run-app.sh
# or
./gradlew run
```

The application will be available at <http://localhost:8080>

### Test the API

```bash
curl -X POST http://localhost:8080/api/v1/promotions/grant \
  -H "Content-Type: application/json" \
  -d '{"userId": "u-123", "code": "WELCOME10"}'
```

Expected response:

```json
{ "success": true, "bonusCents": 1000 }
```

### Run Tests

```bash
# Build and run all tests
./gradlew build

# Run specific test types
./gradlew test --tests RetentionConsumerPactTest    # Consumer contract test
./gradlew test --tests PromotionProviderPactVerificationTest  # Provider verification
./gradlew test --tests PromotionApiIT              # Integration test (requires Docker)
```

## Project Structure

```
leovegas-kotlin-qa-template/
├─ build.gradle.kts              #  Gradle build configuration
├─ settings.gradle.kts           #  Gradle settings
├─ gradle.properties             #  Gradle properties
├─ README.md                     #  This file
├─ run-app.sh                    #  Application startup script
├─ .github/workflows/ci.yml      #  GitHub Actions CI
├─ src/main/kotlin/demo/provider/
│  └─ ProviderApp.kt            #  Ktor provider application
├─ src/test/kotlin/demo/
│  ├─ integration/
│  │  └─ PromotionApiIT.kt      #  Integration test
│  └─ contract/
│     ├─ consumer/
│     │  └─ RetentionConsumerPactTest.kt    #  Pact consumer test
│     └─ provider/
│        └─ PromotionProviderPactVerificationTest.kt  #  Pact provider test
├─ src/test/resources/
│  ├─ db/migration/V1__init.sql  #  Database migration
│  └─ wiremock/mappings/
│     └─ grant_kyc_ok.json      #  WireMock stub
└─ pact/
   └─ retention-service-promotion-provider.json  #  Generated Pact contract
```

## 🧪 What's Included & Working

### Test Framework Stack

- **JUnit 5** + **Kotest** assertions
- **REST-assured** for HTTP testing
- **Ktor client** for HTTP requests
- **MockK** for mocking
- **Testcontainers** with MySQL support
- **WireMock** for external service stubbing
- **Pact** for consumer-driven contract testing
- **Flyway** for database migrations

### Application Components

- **Ktor server** with JSON content negotiation
- **Promotion API** with business logic
- **Database schema** with migration support
- **External service integration** (mocked via WireMock)

### Contract Testing

- Consumer test generates Pact contracts
- Provider verification tests ensure API compatibility
- Contract files stored in `pact/` directory

### Integration Testing

- Full stack testing with real MySQL via Testcontainers
- External dependencies mocked with WireMock
- Database state management via Flyway

### CI/CD Pipeline

- GitHub Actions workflow configured
- Java 21 and Docker support
- Test reporting and artifacts

## API Endpoints

### POST /api/v1/promotions/grant

Grants a promotional bonus based on the provided code.

**Request:**

```json
{
	"userId": "string",
	"code": "string"
}
```

**Response (success=true for codes starting with "WELCOME"):**

```json
{
	"success": true,
	"bonusCents": 1000
}
```

**Response (success=false for other codes):**

```json
{
	"success": false,
	"bonusCents": 0
}
```

## 🔧 Testing Strategy

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test with real database and mocked external services
3. **Contract Tests**:
   - Consumer tests define expected API contracts
   - Provider tests verify the implementation matches contracts
4. **End-to-End**: Full system testing (integration test demonstrates this)

## Test Results Summary

- **Consumer Contract Test**: `RetentionConsumerPactTest` - **PASSED**
- **Provider Verification Test**: `PromotionProviderPactVerificationTest` - **PASSED**
- **Build Process**: Complete compilation and dependency resolution - **SUCCESS**
- **Pact Contract Generation**: Contract file created in `pact/` directory - **SUCCESS**

## Known Issues & Limitations

1. **Integration Test**: May fail if Docker is not properly configured or running
2. **Database Tests**: Require Docker for Testcontainers MySQL
3. **Port Conflicts**: Application uses port 8080, provider tests use 8081

## Development Workflow

1. **Make changes** to the provider or consumer code
2. **Run consumer tests** to generate updated Pact contracts:

   ```bash
   ./gradlew test --tests "*Consumer*"
   ```

3. **Run provider verification** to ensure compatibility:

   ```bash
   ./gradlew test --tests "*Provider*"
   ```

4. **Run integration tests** for end-to-end validation:

   ```bash
   ./gradlew test --tests "*IT"
   ```
