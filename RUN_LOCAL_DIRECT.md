# Run Services Locally - Direct Spring Boot

This guide shows how to run both services directly using Spring Boot Maven plugin without building JARs.

## Prerequisites

- Java 17+
- Maven 3.6+
- Valid Azure credentials

## Step 1: Start Azure Auth Service

Open **Terminal 1** and run:

```bash
# Set environment variables and run
export AZURE_CLIENT_ID=9f6cf3b0-c7fe-4220-86fd-a58ac086a692
export AZURE_CLIENT_SECRET=changeme
export AZURE_TENANT_ID=4f402a0a-52d8-435d-9834-23cc12ede3ff
export SPRING_PROFILES_ACTIVE=local

# Run directly with Maven
./mvnw spring-boot:run
```

**Expected Output:**
```
Started AzureGraphApiApplication in X.XXX seconds
Tomcat started on port 8080 (http)
```

## Step 2: Start Container Login Service

Open **Terminal 2** and run:

```bash
# Navigate to container login service
cd container-login-service

# Set environment variables and run
export SPRING_PROFILES_ACTIVE=local
export AZURE_ENTRA_AUTH_SERVICE_URL=http://localhost:8080

# Run directly with Maven
./mvnw spring-boot:run
```

**Expected Output:**
```
Started LoginServiceApplication in X.XXX seconds
Tomcat started on port 8089 (http)
```

## Step 3: Test Services

```bash
# Test Azure Auth Service
curl http://localhost:8080/health

# Test Container Login Service
curl http://localhost:8089/

# Test login endpoint
curl -X POST http://localhost:8089/web/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"maris@aravindmaklabsoutlook.onmicrosoft.com","password":"changeme"}'
```

## Postman Testing

**Login Endpoint:**
- **Method:** POST
- **URL:** `http://localhost:8089/web/auth/login`
- **Headers:** `Content-Type: application/json`
- **Body:**
```json
{
    "username": "maris@aravindmaklabsoutlook.onmicrosoft.com",
    "password": "changeme"
}
```

## Service URLs

- **Azure Auth Service:** http://localhost:8080
- **Container Login Service:** http://localhost:8089

## Stop Services

Press `Ctrl+C` in each terminal to stop the services.

## One-liner Commands (Alternative)

If you prefer single commands:

**Terminal 1:**
```bash
AZURE_CLIENT_ID=9f6cf3b0-c7fe-4220-86fd-a58ac086a692 AZURE_CLIENT_SECRET=changeme AZURE_TENANT_ID=4f402a0a-52d8-435d-9834-23cc12ede3ff SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

**Terminal 2:**
```bash
cd container-login-service && SPRING_PROFILES_ACTIVE=local AZURE_ENTRA_AUTH_SERVICE_URL=http://localhost:8080 ./mvnw spring-boot:run
```

## Benefits

- ✅ No JAR building required
- ✅ Faster startup time
- ✅ Hot reload for development
- ✅ Easy debugging
- ✅ Automatic code changes pickup
