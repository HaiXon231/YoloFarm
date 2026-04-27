# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21-jammy AS builder

WORKDIR /build
COPY pom.xml .
# Download dependencies first for better caching
RUN mvn dependency:go-offline -B

COPY src ./src
# Package without running tests to speed up the docker build
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

# Expose port 7860 for HuggingFace Spaces
EXPOSE 7860

# Default port environment variable for Spring Boot
ENV SERVER_PORT=7860

# Set user for better security (HuggingFace runs as user 1000)
RUN useradd -m -u 1000 user
USER user

# Entry point
ENTRYPOINT ["java", "-jar", "app.jar"]
