
# Stage 1: Build Spring Boot application
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven configuration
COPY pom.xml .

# Copy application source code
COPY src ./src

# Copy data directory
COPY data ./data

# Build executable JAR
RUN mvn clean package -DskipTests


# Stage 2: Run application
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy generated JAR
COPY --from=build /app/target/*.jar app.jar

# Copy data directory into runtime container
COPY --from=build /app/data ./data

# Render uses the PORT environment variable
EXPOSE 10000

# Start Spring Boot application
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-10000} -jar app.jar"]
