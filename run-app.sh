#!/bin/bash
echo "Starting the Promotion Provider application..."
echo "The application will be available at http://localhost:8080"
echo "API endpoint: POST http://localhost:8080/api/v1/promotions/grant"
echo ""
echo "Example request:"
echo 'curl -X POST http://localhost:8080/api/v1/promotions/grant \\'
echo '  -H "Content-Type: application/json" \\'
echo '  -d '"'"'{"userId": "u-123", "code": "WELCOME10"}'"'"''
echo ""
echo "Press Ctrl+C to stop the application"
echo ""

./gradlew run
