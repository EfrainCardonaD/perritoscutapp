# Guía rápida de configuración de Nginx para api.perritoscut.online

Esta guía consolida comandos y configuraciones clave para manejar **CORS**, **client_max_body_size** y errores 413 en Nginx.

---

## 1️⃣ Ver la configuración de los virtual hosts

```bash
sudo sed -n '1,200p' /etc/nginx/conf.d/api.conf
sudo sed -n '1,200p' /etc/nginx/conf.d/perritoscut.conf
```
**Qué hace:** Muestra contenido parcial de los archivos `.conf` que definen los vhosts.

```bash
sudo nginx -T | grep -nEi 'server_name|client_max_body_size|location|proxy_pass|add_header|Access-Control-Allow|OPTIONS|api.perritoscut.online'
```
**Qué hace:** Muestra todas las directivas clave de Nginx en la configuración activa.

---

## 2️⃣ Revisar errores recientes (413 y body demasiado grande)

```bash
sudo grep -inE 'too large|request body|client intended to send too large' /var/log/nginx/error.log | tail -n 50
```
**Qué hace:** Filtra errores relacionados con solicitudes que exceden `client_max_body_size`.

---

## 3️⃣ Editar archivos de configuración

- Con Vim:
```bash
sudo vim /etc/nginx/conf.d/api.conf
```
- Con Nano:
```bash
sudo nano /etc/nginx/conf.d/api.conf
```
**Qué hace:** Permite agregar/modificar directivas como `client_max_body_size`, headers CORS, manejo de errores 413 y preflight OPTIONS.

---

## 4️⃣ Configuración recomendada para api.conf

### Map de CORS (antes de server blocks)
```nginx
map $http_origin $cors_origin {
    ~^https://(www\.)?perritoscut\.online$ $http_origin;
    default "";
}
```

### Bloque HTTPS con CORS y límite de body
```nginx
server {
    listen 443 ssl;
    server_name api.perritoscut.online;

    ssl_certificate /etc/ssl/certs/cloudflare_origin.crt;
    ssl_certificate_key /etc/ssl/private/cloudflare_origin.key;

    client_max_body_size 20M;

    add_header Access-Control-Allow-Origin $cors_origin always;
    add_header Vary "Origin" always;
    add_header Access-Control-Allow-Credentials "true" always;
    add_header Access-Control-Allow-Methods "GET,POST,PUT,DELETE,OPTIONS" always;
    add_header Access-Control-Allow-Headers "Authorization,Content-Type,Origin,Accept" always;

    if ($request_method = OPTIONS) { return 204; }

    error_page 413 = @cors_413;
    location @cors_413 {
        add_header Access-Control-Allow-Origin $cors_origin always;
        add_header Vary "Origin" always;
        add_header Access-Control-Allow-Credentials "true" always;
        add_header Access-Control-Allow-Methods "GET,POST,PUT,DELETE,OPTIONS" always;
        add_header Access-Control-Allow-Headers "Authorization,Content-Type,Origin,Accept" always;
        return 413;
    }

    location / {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Bloque HTTP para redirigir a HTTPS
```nginx
server {
    listen 80;
    server_name api.perritoscut.online;
    return 301 https://$host$request_uri;
}
```

---

## 5️⃣ Validar la configuración

```bash
sudo nginx -t
```
**Qué hace:** Revisa la sintaxis de todos los archivos `.conf`. Detecta errores como variables desconocidas.

---

## 6️⃣ Recargar Nginx

```bash
sudo systemctl reload nginx
```
**Qué hace:** Aplica la nueva configuración sin reiniciar el servicio.

---

## 7️⃣ Prueba rápida

- **Preflight OPTIONS:**
```bash
curl -I -X OPTIONS https://api.perritoscut.online/api/imagenes/perritos \
-H "Origin: https://www.perritoscut.online" \
-H "Access-Control-Request-Method: POST"
```
- **Subida de archivo:**
  - Hasta 20 MB funciona sin 413.
  - >20 MB devuelve 413 con headers CORS, no bloqueado por CORS.

---

**Tip:** Siempre revisa los logs si ves errores o comportamiento extraño:
```bash
sudo tail -f /var/log/nginx/error.log
```