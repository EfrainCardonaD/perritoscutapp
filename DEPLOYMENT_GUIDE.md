# Guía de Despliegue - CUT Application

## 🚀 Estrategias de Despliegue

### 1. Despliegue Local (Desarrollo)

#### Prerrequisitos
- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Git

#### Pasos
```bash
# 1. Clonar repositorio
git clone [URL_REPOSITORIO]
cd Graficos

# 2. Configurar base de datos
mysql -u root -p
CREATE DATABASE cut;
CREATE USER 'cut_user'@'localhost' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON cut.* TO 'cut_user'@'localhost';
FLUSH PRIVILEGES;

# 3. Configurar variables de entorno
export DB_URL="jdbc:mysql://localhost:3306/cut"
export DB_USERNAME="cut_user"
export DB_PASSWORD="secure_password"
export JWT_SECRET="your-super-secure-256-bit-secret-key-here"

# 4. Ejecutar aplicación
./mvnw spring-boot:run
```

### 2. Despliegue con Docker

#### Docker Compose (Recomendado)
```yaml
# docker-compose.yml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DB_URL=jdbc:mysql://db:3306/cut
      - DB_USERNAME=cut_user
      - DB_PASSWORD=secure_password
      - JWT_SECRET=your-super-secure-256-bit-secret-key-here
    depends_on:
      - db
    networks:
      - cut-network

  db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=root_password
      - MYSQL_DATABASE=cut
      - MYSQL_USER=cut_user
      - MYSQL_PASSWORD=secure_password
    volumes:
      - db_data:/var/lib/mysql
    networks:
      - cut-network

volumes:
  db_data:

networks:
  cut-network:
```

#### Comandos Docker
```bash
# Construcción y ejecución
docker-compose up -d

# Ver logs
docker-compose logs -f app

# Parar servicios
docker-compose down

# Rebuild después de cambios
docker-compose up -d --build
```

### 3. Despliegue en Producción

#### Con Docker en Servidor
```bash
# 1. Preparar servidor
sudo apt update
sudo apt install docker.io docker-compose
sudo systemctl start docker
sudo systemctl enable docker

# 2. Transferir archivos
scp -r . usuario@servidor:/opt/cut-app/

# 3. Configurar variables de entorno para producción
echo "DB_URL=jdbc:mysql://production-db:3306/cut" > .env
echo "DB_USERNAME=production_user" >> .env
echo "DB_PASSWORD=super_secure_production_password" >> .env
echo "JWT_SECRET=production-256-bit-secret-key" >> .env

# 4. Ejecutar en producción
docker-compose -f docker-compose.prod.yml up -d
```

#### docker-compose.prod.yml
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_URL=${DB_URL}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
    restart: unless-stopped
    depends_on:
      - db
    networks:
      - cut-network

  db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
      - MYSQL_DATABASE=cut
      - MYSQL_USER=${DB_USERNAME}
      - MYSQL_PASSWORD=${DB_PASSWORD}
    volumes:
      - /opt/cut-data:/var/lib/mysql
    restart: unless-stopped
    networks:
      - cut-network

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - app
    restart: unless-stopped
    networks:
      - cut-network

networks:
  cut-network:
```

### 4. Configuración de Nginx (Proxy Reverso)

#### nginx.conf
```nginx
events {
    worker_connections 1024;
}

http {
    upstream app {
        server app:8080;
    }

    server {
        listen 80;
        server_name tu-dominio.com;
        return 301 https://$server_name$request_uri;
    }

    server {
        listen 443 ssl http2;
        server_name tu-dominio.com;

        ssl_certificate /etc/nginx/ssl/cert.pem;
        ssl_certificate_key /etc/nginx/ssl/key.pem;

        # Configuraciones de seguridad SSL
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;

        # Headers de seguridad
        add_header X-Frame-Options DENY;
        add_header X-Content-Type-Options nosniff;
        add_header X-XSS-Protection "1; mode=block";

        location / {
            proxy_pass http://app;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Rate limiting
        limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
        location /api/ {
            limit_req zone=api burst=20 nodelay;
            proxy_pass http://app;
        }
    }
}
```

## 🔧 Configuraciones por Ambiente

### Desarrollo (application-dev.properties)
```properties
# Logging detallado
logging.level.com.cut.cardona=DEBUG
logging.level.org.springframework.security=DEBUG
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# DevTools
spring.devtools.restart.enabled=true
spring.h2.console.enabled=true

# Configuración relajada para desarrollo
management.endpoints.web.exposure.include=*
```

### Testing (application-test.properties)
```properties
# Base de datos en memoria para tests
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop

# JWT para testing
JWT_SECRET=test-secret-key-for-testing-only
```

### Producción (application-prod.properties)
```properties
# Logging optimizado
logging.level.com.cut.cardona=INFO
logging.level.org.springframework.security=WARN
spring.jpa.show-sql=false

# Seguridad
server.error.include-stacktrace=never
server.error.include-message=never

# Actuator restringido
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when-authorized

# Pool de conexiones optimizado
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
```

## 📊 Monitoreo y Observabilidad

### Health Checks
```bash
# Verificar estado de la aplicación
curl http://localhost:8080/actuator/health

# Respuesta esperada
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

### Prometheus Metrics (Opcional)
```properties
# Agregar al pom.xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

# application.properties
management.endpoints.web.exposure.include=prometheus
management.metrics.export.prometheus.enabled=true
```

### Logging Centralizado
```xml
<!-- logback-spring.xml -->
<configuration>
    <springProfile name="prod">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/var/log/cut-app/application.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>/var/log/cut-app/application.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                </providers>
            </encoder>
        </appender>
    </springProfile>
</configuration>
```

## 🔐 Seguridad en Producción

### SSL/TLS
```bash
# Generar certificados con Let's Encrypt
sudo apt install certbot
sudo certbot certonly --standalone -d tu-dominio.com

# Configurar renovación automática
sudo crontab -e
0 12 * * * /usr/bin/certbot renew --quiet
```

### Firewall
```bash
# Configurar UFW
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

### Variables de Entorno Seguras
```bash
# Usar archivos .env seguros
chmod 600 .env
chown root:root .env

# O usar servicios de secretos
# AWS Secrets Manager, HashiCorp Vault, etc.
```

## 🚀 CI/CD Pipeline

### GitHub Actions Example
```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run tests
        run: ./mvnw test

  deploy:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Deploy to server
        run: |
          ssh usuario@servidor 'cd /opt/cut-app && git pull && docker-compose up -d --build'
```

## 📋 Checklist de Despliegue

### Pre-Despliegue
- [ ] Tests unitarios pasando
- [ ] Tests de integración pasando
- [ ] Configuración de BD validada
- [ ] Variables de entorno configuradas
- [ ] Certificados SSL instalados
- [ ] Firewall configurado

### Post-Despliegue
- [ ] Health check exitoso
- [ ] Logs sin errores críticos
- [ ] Endpoints principales funcionando
- [ ] Autenticación funcionando
- [ ] Rate limiting activo
- [ ] Monitoreo configurado

## 🆘 Troubleshooting

### Problemas Comunes

#### Error de Conexión a BD
```bash
# Verificar conectividad
docker exec -it cut-app-db-1 mysql -u cut_user -p cut

# Verificar logs
docker-compose logs db
```

#### Error JWT
```bash
# Verificar variable de entorno
echo $JWT_SECRET

# Verificar logs de autenticación
docker-compose logs app | grep JWT
```

#### Performance Issues
```bash
# Verificar recursos
docker stats

# Verificar logs de la aplicación
docker-compose logs app | grep ERROR
```
