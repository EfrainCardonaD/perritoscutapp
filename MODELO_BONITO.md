# Modelo relacional y justificación

## 1. Separación de identidades y perfiles
La tabla `usuarios` gestiona autenticación y estado, mientras que `perfiles_usuario` contiene datos personales editables. La relación es 1 a 1 mediante la clave única `usuario_id`.

## 2. Publicaciones y moderación
La tabla `perros` referencia al autor (`usuario_id`) y al revisor (`revisado_por`), permitiendo trazabilidad del flujo de aprobación y estados.

## 3. Medios
`imagenes_perros` tiene una relación N a 1 con `perros` y una imagen principal única por perro mediante la restricción única `UK (perro_id, principal_flag)`.

## 4. Flujo de adopción
`solicitudes_adopcion` vincula solicitante y perro; `documentos_solicitud` almacena archivos por solicitud con unicidad por tipo (`UK (solicitud_id, tipo_documento)`).

## 5. Seguridad y verificación
`tokens_reset` y `verification_tokens` referencian a usuarios para recuperación de cuentas y verificación multi-canal.

---

# Esquema relacional

```sql
usuarios(
  id PK,
  user_name UQ,
  email UQ,
  password,
  fecha_creacion,
  ultimo_acceso,
  activo,
  token,
  fecha_expiracion_token,
  rol,
  email_verificado,
  telefono,
  telefono_verificado
)

perfiles_usuario(
  id PK,
  usuario_id FK→usuarios.id UQ,
  nombre_real,
  telefono UQ,
  idioma,
  zona_horaria,
  fecha_nacimiento,
  fecha_creacion,
  fecha_actualizacion
)

perros(
  id PK,
  nombre,
  edad,
  sexo,
  tamano,
  raza,
  descripcion,
  ubicacion,
  fecha_publicacion,
  estado_adopcion,
  estado_revision,
  usuario_id FK→usuarios.id,
  revisado_por FK→usuarios.id
)

imagenes_perros(
  id PK,
  perro_id FK→perros.id,
  url,
  descripcion,
  principal,
  principal_flag,
  fecha_subida,
  UK (perro_id, principal_flag)
)

solicitudes_adopcion(
  id PK,
  perro_id FK→perros.id,
  solicitante_id FK→usuarios.id,
  estado,
  mensaje,
  fecha_solicitud,
  fecha_respuesta,
  revisado_por FK→usuarios.id,
  UK (perro_id, solicitante_id, activo_flag),
  UK (perro_id, accepted_flag)
)

documentos_solicitud(
  id PK,
  solicitud_id FK→solicitudes_adopcion.id,
  tipo_documento,
  url_documento,
  nombre_archivo,
  tipo_mime,
  tamano_bytes,
  fecha_subida,
  UK (solicitud_id, tipo_documento)
)

imagenes_perfil(
  id PK,
  perfil_usuario_id FK→perfiles_usuario.id,
  nombre_archivo,
  ruta_archivo,
  tipo_mime,
  tamano_bytes,
  url_publica,
  activa,
  fecha_subida
)

verification_tokens(
  id PK,
  usuario_id FK→usuarios.id,
  canal,
  token,
  codigo_otp,
  expira_en,
  usado_en,
  intentos,
  creado_en
)

tokens_reset(
  id PK,
  usuario_id FK→usuarios.id,
  token,
  expira_en,
  usado_en,
  creado_en
)
```

---

# Normalización

- **1FN:** Todas las tablas contienen valores atómicos; no hay campos multivaluados ni listas. Las imágenes y documentos se modelan como filas separadas.
- **2FN:** No existen claves primarias compuestas en las tablas principales; dependencias de atributos son completas respecto de la clave. Las restricciones de unicidad aseguran reglas de negocio sin dependencias parciales.
- **3FN:** No hay dependencias transitivas de atributos no-clave entre sí. Atributos derivables se implementan como columnas computadas para facilitar reglas, sin romper 3FN.

**Justificación:** La descomposición separa identidades, perfiles, publicaciones, medios, solicitudes y tokens; se minimiza redundancia, se facilita integridad referencial y la evolución del modelo.

---

# Datos de ejemplo

## usuarios
| id                                   | user_name | email                | rol        | activo | email_verificado | telefono         | telefono_verificado | fecha_creacion         |
|---------------------------------------|-----------|----------------------|------------|--------|------------------|------------------|---------------------|------------------------|
| bc43c7b5-1bc9-4edc-936a-23ba04474935 | lupita    | lupita@example.com   | ROLE_USER  | 1      | 1                | +5215511122233   | 1                   | 2025-07-19 05:56:29    |
| 87c9c26d-1922-43e6-9498-eacf94913a79 | carlos    | carlos@example.com   | ROLE_USER  | 1      | 1                | +5215588899911   | 0                   | 2025-07-24 05:56:29    |
| fc391986-e2cd-4e3c-adeb-aa720657823b | admin     | admin@perritoscut.app| ROLE_ADMIN | 1      | 1                | +5215550000000   | 1                   | 2025-05-10 05:56:29    |
| 62ca076e-f275-456e-825a-aea4bd1dce90 | monica    | monica@example.com   | ROLE_USER  | 1      | 0                | +5215566612345   | 0                   | 2025-08-06 05:56:29    |
| 32e4a661-8b98-4d34-afc1-c6d837f894ab | roberto   | roberto@example.com  | ROLE_USER  | 1      | 1                | +5215577700001   | 1                   | 2025-08-12 05:56:29    |

## perfiles_usuario
| id                                   | usuario_id                              | nombre_real      | telefono         | idioma | zona_horaria         | fecha_nacimiento |
|---------------------------------------|-----------------------------------------|------------------|------------------|--------|----------------------|------------------|
| 36caa3ad-75d2-40de-880b-57e6fd43e959 | bc43c7b5-1bc9-4edc-936a-23ba04474935   | Guadalupe Pérez  | +5215511122233   | es     | America/Mexico_City  | 1995-04-10       |
| ae6298a3-08ff-4666-9574-f94ad7a6310b | 87c9c26d-1922-43e6-9498-eacf94913a79   | Carlos Ramírez   | +5215588899911   | es     | America/Mexico_City  | 1990-09-23       |
| 4c8e1be6-fdb9-4837-8e4d-a908b50f9b67 | fc391986-e2cd-4e3c-adeb-aa720657823b   | Admin General    | +5215550000000   | es     | America/Mexico_City  | 1985-01-01       |
| 08a0e9c6-c800-4e67-8d5d-d19d9adaf8ce | 62ca076e-f275-456e-825a-aea4bd1dce90   | Mónica López     | +5215566612345   | es     | America/Mexico_City  | 1998-12-02       |
| 162916f5-e2ea-46cd-aeed-3ccbf96419e7 | 32e4a661-8b98-4d34-afc1-c6d837f894ab   | Roberto Díaz     | +5215577700001   | es     | America/Mexico_City  | 1992-07-18       |

## perros
| id                                   | nombre | edad | sexo   | tamano  | raza           | descripcion                | ubicacion   | estado_adopcion | estado_revision | usuario_id                              | revisado_por                              |
|---------------------------------------|--------|------|--------|---------|----------------|----------------------------|-------------|-----------------|----------------|-----------------------------------------|-------------------------------------------|
| dd9b3543-d4ff-449f-b845-d669f7f4597f | Bobby  | 3    | Macho  | Mediano | Mestizo        | Juguetón y amigable.       | CDMX        | Disponible      | Aprobado       | bc43c7b5-1bc9-4edc-936a-23ba04474935   | fc391986-e2cd-4e3c-adeb-aa720657823b      |
| 596c6b70-fdad-493a-b2c2-b1873d3d7eec | Luna   | 2    | Hembra | Pequeño | Chihuahua      | Muy cariñosa y tranquila.  | Guadalajara | Disponible      | Aprobado       | 87c9c26d-1922-43e6-9498-eacf94913a79   | fc391986-e2cd-4e3c-adeb-aa720657823b      |
| a6693a73-acf4-4f62-8350-53d1892b3a01 | Rex    | 4    | Macho  | Grande  | Pastor Alemán  | Fiel y protector.          | Monterrey   | En proceso      | Aprobado       | 62ca076e-f275-456e-825a-aea4bd1dce90   | fc391986-e2cd-4e3c-adeb-aa720657823b      |
| 21bb5e25-d5df-4701-977a-f4bfc179e731 | Nala   | 1    | Hembra | Mediano | Mestizo        | Cachorra muy activa.       | CDMX        | Disponible      | Pendiente      | bc43c7b5-1bc9-4edc-936a-23ba04474935   | NULL                                      |
| 85e8dbcc-cac7-4248-812e-dc59ee40b4f2 | Toby   | 5    | Macho  | Mediano | Beagle         | Le encanta pasear.         | Puebla      | Adoptado        | Aprobado       | 32e4a661-8b98-4d34-afc1-c6d837f894ab   | fc391986-e2cd-4e3c-adeb-aa720657823b      |

## imagenes_perros
| id                                   | perro_id                                | url                                         | descripcion                | principal |
|---------------------------------------|-----------------------------------------|---------------------------------------------|----------------------------|-----------|
| f6c14b28-c514-4acc-8782-15fd1f74d45b | dd9b3543-d4ff-449f-b845-d669f7f4597f   | https://img.perritoscut.app/bobby_1.jpg     | Foto principal de Bobby    | 1         |
| 9572ae94-0dcf-4ef9-ad5f-530d372f4ae1 | dd9b3543-d4ff-449f-b845-d669f7f4597f   | https://img.perritoscut.app/bobby_2.jpg     | Foto adicional de Bobby    | 0         |
| 525bdc32-f0a5-4bd2-9f12-7a8f962dd156 | 596c6b70-fdad-493a-b2c2-b1873d3d7eec   | https://img.perritoscut.app/luna_1.jpg      | Foto principal de Luna     | 1         |
| c59f2df3-6013-4792-91b7-a0ba04ef0c4c | 596c6b70-fdad-493a-b2c2-b1873d3d7eec   | https://img.perritoscut.app/luna_2.jpg      | Foto adicional de Luna     | 0         |
| ebff220d-a362-48ef-b917-bd78f27e2d36 | a6693a73-acf4-4f62-8350-53d1892b3a01   | https://img.perritoscut.app/rex_1.jpg       | Foto principal de Rex      | 1         |
| fe39feef-18e4-4374-b19f-308af617fffe | a6693a73-acf4-4f62-8350-53d1892b3a01   | https://img.perritoscut.app/rex_2.jpg       | Foto adicional de Rex      | 0         |
| 50a3fa21-c9bd-4916-8075-3ac0fdf5acc9 | 21bb5e25-d5df-4701-977a-f4bfc179e731   | https://img.perritoscut.app/nala_1.jpg      | Foto principal de Nala     | 1         |
| 2cf2f395-b7d2-4ee6-9d8a-f4b0e7f3860d | 21bb5e25-d5df-4701-977a-f4bfc179e731   | https://img.perritoscut.app/nala_2.jpg      | Foto adicional de Nala     | 0         |
| 69983e92-4289-4cbd-ab38-cc29c91cb5f9 | 85e8dbcc-cac7-4248-812e-dc59ee40b4f2   | https://img.perritoscut.app/toby_1.jpg      | Foto principal de Toby     | 1         |
| eb861cd9-e990-456c-8f36-2a9a5b074c76 | 85e8dbcc-cac7-4248-812e-dc59ee40b4f2   | https://img.perritoscut.app/toby_2.jpg      | Foto adicional de Toby     | 0         |

## imagenes_perfil
| id                                   | perfil_usuario_id                        | nombre_archivo | ruta_archivo                                 | tipo_mime   | tamano_bytes | url_publica                                               | activa |
|---------------------------------------|------------------------------------------|---------------|------------------------------------------------|------------|--------------|----------------------------------------------------------|--------|
| 46870b44-8e95-4060-8eec-31e3ec3e825f | 36caa3ad-75d2-40de-880b-57e6fd43e959    | guadalupe.jpg | /uploads/perfiles/36caa3ad-75d2-40de-880b-57e6fd43e959.jpg | image/jpeg | 150000       | https://img.perritoscut.app/perfiles/36caa3ad-75d2-40de-880b-57e6fd43e959.jpg | 1      |
| 14ecd893-e3f4-4b5e-99f8-316dc91f715b | ae6298a3-08ff-4666-9574-f94ad7a6310b    | carlos.jpg    | /uploads/perfiles/ae6298a3-08ff-4666-9574-f94ad7a6310b.jpg | image/jpeg | 150000       | https://img.perritoscut.app/perfiles/ae6298a3-08ff-4666-9574-f94ad7a6310b.jpg | 1      |
| 3f1183d4-a917-4360-8f25-1b91053b6f66 | 4c8e1be6-fdb9-4837-8e4d-a908b50f9b67    | admin.jpg     | /uploads/perfiles/4c8e1be6-fdb9-4837-8e4d-a908b50f9b67.jpg | image/jpeg | 150000       | https://img.perritoscut.app/perfiles/4c8e1be6-fdb9-4837-8e4d-a908b50f9b67.jpg | 1      |
| 15a6b714-d24c-4bed-bb7c-5a43ec33e493 | 08a0e9c6-c800-4e67-8d5d-d19d9adaf8ce    | mónica.jpg    | /uploads/perfiles/08a0e9c6-c800-4e67-8d5d-d19d9adaf8ce.jpg | image/jpeg | 150000       | https://img.perritoscut.app/perfiles/08a0e9c6-c800-4e67-8d5d-d19d9adaf8ce.jpg | 1      |
| 97544e4e-b3ba-4de3-b00c-ef86eadf6701 | 162916f5-e2ea-46cd-aeed-3ccbf96419e7    | roberto.jpg   | /uploads/perfiles/162916f5-e2ea-46cd-aeed-3ccbf96419e7.jpg | image/jpeg | 150000       | https://img.perritoscut.app/perfiles/162916f5-e2ea-46cd-aeed-3ccbf96419e7.jpg | 1      |

## solicitudes_adopcion
| id                                   | perro_id                                | solicitante_id                            | estado     | mensaje                                 | revisado_por                              |
|---------------------------------------|-----------------------------------------|-------------------------------------------|------------|------------------------------------------|-------------------------------------------|
| 4591f0de-497d-4961-87b9-b52c3da6f46e | dd9b3543-d4ff-449f-b845-d669f7f4597f   | 87c9c26d-1922-43e6-9498-eacf94913a79     | Pendiente  | Estoy interesado en darle un hogar.      | NULL                                      |
| 9ca37fa8-60b2-453d-9557-6c1ca9f9c6da | 596c6b70-fdad-493a-b2c2-b1873d3d7eec   | bc43c7b5-1bc9-4edc-936a-23ba04474935     | En revisión| Tenemos patio y tiempo para cuidarla.   | fc391986-e2cd-4e3c-adeb-aa720657823b      |
| f44d168c-a75e-460e-ac61-176a167d4001 | a6693a73-acf4-4f62-8350-53d1892b3a01   | 32e4a661-8b98-4d34-afc1-c6d837f894ab     | Aceptada   | Familia con experiencia en perros grandes.| fc391986-e2cd-4e3c-adeb-aa720657823b      |
| e54a6510-4379-4aa1-983b-4fb3cdc8321a | 21bb5e25-d5df-4701-977a-f4bfc179e731   | 87c9c26d-1922-43e6-9498-eacf94913a79     | Pendiente  | Busco una compañera activa.             | NULL                                      |
| 05c81c08-9b55-4652-95d0-a930b5d36a4f | 85e8dbcc-cac7-4248-812e-dc59ee40b4f2   | bc43c7b5-1bc9-4edc-936a-23ba04474935     | Rechazada  | Podría pasearlo a diario.               | fc391986-e2cd-4e3c-adeb-aa720657823b      |

## documentos_solicitud
| id                                   | solicitud_id                            | tipo_documento        | url_documento                                         | nombre_archivo      | tipo_mime        | tamano_bytes |
|---------------------------------------|-----------------------------------------|----------------------|-------------------------------------------------------|---------------------|------------------|--------------|
| 342f3b5b-f302-45fa-9bd9-736f8febb884 | 9ca37fa8-60b2-453d-9557-6c1ca9f9c6da   | INE                  | https://docs.perritoscut.app/sol2/ine.pdf             | ine.pdf            | application/pdf  | 250000       |
| 3b927dea-1c94-4b91-89b4-d31d299a8938 | f44d168c-a75e-460e-ac61-176a167d4001   | ComprobanteDomicilio | https://docs.perritoscut.app/sol3/comprobante.pdf     | comprobante.pdf    | application/pdf  | 300000       |
| 7325e293-cf86-45f5-8650-e72757dc82c7 | f44d168c-a75e-460e-ac61-176a167d4001   | INE                  | https://docs.perritoscut.app/sol3/ine.pdf             | ine.pdf            | application/pdf  | 200000       |
| e168ccd4-214e-4336-94ec-94568cc77dd9 | 4591f0de-497d-4961-87b9-b52c3da6f46e   | CartaCompromiso      | https://docs.perritoscut.app/sol1/carta.pdf           | carta.pdf          | application/pdf  | 180000       |
| 7317a5e7-6733-4a8b-a730-b90c7370e9fc | e54a6510-4379-4aa1-983b-4fb3cdc8321a   | INE                  | https://docs.perritoscut.app/sol4/ine.pdf             | ine.pdf            | application/pdf  | 210000       |

## tokens_reset
| id                                   | usuario_id                              | token         | expira_en              | usado_en |
|---------------------------------------|-----------------------------------------|---------------|------------------------|----------|
| 6e267045-cd09-464a-a2a7-cae5ae7b82ce | bc43c7b5-1bc9-4edc-936a-23ba04474935   | reset_tok_1   | 2025-08-19 05:56:29    | NULL     |
| b9d7ebeb-4a8c-46db-b7f5-28376da512f5 | 87c9c26d-1922-43e6-9498-eacf94913a79   | reset_tok_2   | 2025-08-19 05:56:29    | NULL     |
| ad3037d3-7676-42f1-a11c-716f79eb4c90 | 62ca076e-f275-456e-825a-aea4bd1dce90   | reset_tok_3   | 2025-08-20 05:56:29    | NULL     |
| 4b585628-b99b-4785-8d32-2469f0b6ead4 | fc391986-e2cd-4e3c-adeb-aa720657823b   | reset_tok_4   | 2025-08-25 05:56:29    | NULL     |
| c2a78964-22da-489c-bad8-99011809413a | 32e4a661-8b98-4d34-afc1-c6d837f894ab   | reset_tok_5   | 2025-08-19 05:56:29    | NULL     |

## verification_tokens
| id                                   | usuario_id                              | canal | token       | codigo_otp | expira_en              | usado_en              | intentos |
|---------------------------------------|-----------------------------------------|-------|------------|------------|------------------------|-----------------------|----------|
| 0e7bc2c6-96bc-4c26-be48-ad21889ccb28 | bc43c7b5-1bc9-4edc-936a-23ba04474935   | EMAIL | ver_tok_1  | 123456     | 2025-08-18 11:56:29    | NULL                  | 0        |
| 311b806a-df06-4c35-8aed-2888b96a2342 | 87c9c26d-1922-43e6-9498-eacf94913a79   | PHONE | ver_tok_2  | 654321     | 2025-08-18 11:56:29    | NULL                  | 1        |
| 0c71f32d-1f8b-40a1-9195-aaf90382799d | fc391986-e2cd-4e3c-adeb-aa720657823b   | EMAIL | ver_tok_3  | 777000     | 2025-08-18 17:56:29    | 2025-08-18 04:56:29   | 1        |
| 1149a608-9a0e-41b3-b601-87b42baee080 | 62ca076e-f275-456e-825a-aea4bd1dce90   | EMAIL | ver_tok_4  | 999111     | 2025-08-18 07:56:29    | NULL                  | 0        |
| b87319b4-a67b-4b92-9bfd-3e61f9a15891 | 32e4a661-8b98-4d34-afc1-c6d837f894ab   | PHONE | ver_tok_5  | 010203     | 2025-08-18 06:56:29    | NULL                  | 2        |

## imagenes
| id | descripcion              | fecha_subida           | titulo              | url                                                    |
|----|--------------------------|------------------------|---------------------|--------------------------------------------------------|
| 1  | Banner de adopciones     | 2025-08-03 05:56:29    | Adopta Hoy          | https://img.perritoscut.app/banners/adopta.jpg         |
| 2  | Logo oficial             | 2025-06-19 05:56:29    | Logo                | https://img.perritoscut.app/logo.png                   |
| 3  | Campaña esterilización   | 2025-08-13 05:56:29    | Esteriliza y Cuida  | https://img.perritoscut.app/campanas/esteriliza.jpg    |
| 4  | Portada redes            | 2025-08-08 05:56:29    | Portada Facebook    | https://img.perritoscut.app/redes/portada.jpg          |
| 5  | Voluntariado             | 2025-08-17 05:56:29    | Únete               | https://img.perritoscut.app/voluntariado.jpg           |

---

# Consultas de verificación

```sql
SELECT * FROM usuarios;
SELECT * FROM perfiles_usuario;
SELECT * FROM perros;
SELECT * FROM imagenes_perros;
SELECT * FROM imagenes_perfil;
SELECT * FROM solicitudes_adopcion;
SELECT * FROM documentos_solicitud;
SELECT * FROM tokens_reset;
SELECT * FROM verification_tokens;
SELECT * FROM imagenes;
```

---

# Exportación a XML y JSON

## Ejemplo XML (usuarios)
```xml
<usuarios>
  <usuario>
    <id>bc43c7b5-1bc9-4edc-936a-23ba04474935</id>
    <user_name>lupita</user_name>
    <email>lupita@example.com</email>
    <rol>ROLE_USER</rol>
    <activo>1</activo>
    <email_verificado>1</email_verificado>
    <telefono>+5215511122233</telefono>
    <telefono_verificado>1</telefono_verificado>
    <fecha_creacion>2025-07-19 05:56:29</fecha_creacion>
  </usuario>
  ...
</usuarios>
```

## Ejemplo JSON (perros)
```json
[
  {
    "id": "dd9b3543-d4ff-449f-b845-d669f7f4597f",
    "nombre": "Bobby",
    "edad": 3,
    "sexo": "Macho",
    "tamano": "Mediano",
    "raza": "Mestizo",
    "ubicacion": "CDMX",
    "estado_adopcion": "Disponible",
    "estado_revision": "Aprobado",
    "usuario_id": "bc43c7b5-1bc9-4edc-936a-23ba04474935"
  },
  ...
]
```

---

# Conclusión
El modelo propuesto cumple con las reglas de normalización, facilita la trazabilidad y la evolución, y permite una gestión clara de identidades, publicaciones, medios y solicitudes.

