# Etapa de build
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copiar descriptor y descargar dependencias primero para aprovechar cache
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Copiar el código fuente y compilar
COPY src ./src
RUN mvn -q -DskipTests package

# Etapa de runtime mínima
FROM eclipse-temurin:17-jre-jammy

# Crear usuario no root
RUN useradd -m appuser
USER appuser
WORKDIR /app

# Copiar el JAR construido
COPY --from=builder /app/target/*.jar /app/app.jar

EXPOSE 8080

# Variables JVM opcionales
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
