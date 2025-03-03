# Use Eclipse Temurin as the base image
FROM eclipse-temurin:17.0.11_9-jdk-jammy

# Define the working directory
WORKDIR /root

# Expose the application port
EXPOSE 8080

# Copy Maven wrapper and project files
COPY ../pom.xml /root
COPY ../mvnw /root/mvnw
COPY ../.mvn /root/.mvn

# Download dependencies
RUN chmod +x /root/mvnw && /root/mvnw dependency:go-offline

# Copy the source code into the container
COPY ./src /root/src

# Build the application
RUN /root/mvnw clean install -DskipTests

# Set the entry point to run the app
ENTRYPOINT ["java", "-jar", "target/spring-boot-docker.jar"]
