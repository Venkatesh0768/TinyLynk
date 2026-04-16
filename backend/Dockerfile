FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw

# Resolve dependencies
RUN ./mvnw dependency:go-offline

# Copy source code and build the application
COPY src/ src/
RUN ./mvnw clean package -DskipTests

# Production Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user and group for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy over the built artifact from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Expose the application port
EXPOSE 5000

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
