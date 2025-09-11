# Diseño y Exportación de Datos: perritoscutapp

## 1. Descripción del Proyecto
**Nombre:** perritoscutapp

**Descripción general:**
Sistema web para la gestión de adopción de perros. Permite a usuarios registrar perros disponibles para adopción, gestionar sus perfiles, subir imágenes y documentos, y administrar solicitudes de adopción.

**Usuarios objetivo:**
- Personas interesadas en adoptar perros.
- Refugios y protectoras de animales.
- Administradores del sistema.

**Acciones principales:**
- Registro y autenticación de usuarios.
- Publicación y gestión de perros en adopción.
- Envío y gestión de solicitudes de adopción.
- Subida de imágenes y documentos relacionados.
- Revisión y aprobación de solicitudes.

## 2. Entidades principales y relaciones
1. **usuarios**: Datos de acceso y contacto de los usuarios.
2. **perfiles_usuario**: Información adicional del usuario (nombre real, teléfono, fecha de nacimiento, etc.).
3. **perros**: Información de cada perro disponible para adopción.
4. **imagenes_perros**: Imágenes asociadas a cada perro.
5. **solicitudes_adopcion**: Solicitudes de adopción realizadas por los usuarios.
6. **documentos_solicitud**: Documentos requeridos para la solicitud de adopción.

**Relaciones:**
- Un usuario tiene un perfil_usuario.
- Un usuario puede publicar varios perros.
- Un perro puede tener varias imágenes.
- Un usuario puede realizar varias solicitudes de adopción.
- Una solicitud puede tener varios documentos asociados.

## 3. Diagrama Entidad-Relación (ER)

**Descripción textual:**
- **usuarios** (1) --- (1) **perfiles_usuario**
- **usuarios** (1) --- (N) **perros**
- **perros** (1) --- (N) **imagenes_perros**
- **usuarios** (1) --- (N) **solicitudes_adopcion**
- **perros** (1) --- (N) **solicitudes_adopcion**
- **solicitudes_adopcion** (1) --- (N) **documentos_solicitud**

**Justificación:**
El modelo permite gestionar la información de usuarios y perros, asociar imágenes y documentos, y controlar el flujo de solicitudes de adopción, cubriendo los requerimientos de una plataforma de adopción.

## 4. Esquema Relacional

### usuarios
- id (PK)
- user_name
- email
- password
- fecha_creacion
- ultimo_acceso
- activo
- rol

### perfiles_usuario
- id (PK)
- usuario_id (FK -> usuarios.id)
- nombre_real
- telefono
- fecha_nacimiento

### perros
- id (PK)
- nombre
- edad
- sexo
- tamano
- raza
- descripcion
- ubicacion
- fecha_publicacion
- estado_adopcion
- usuario_id (FK -> usuarios.id)

### imagenes_perros
- id (PK)
- perro_id (FK -> perros.id)
- url
- descripcion
- principal

### solicitudes_adopcion
- id (PK)
- perro_id (FK -> perros.id)
- solicitante_id (FK -> usuarios.id)
- estado
- mensaje
- fecha_solicitud

### documentos_solicitud
- id (PK)
- solicitud_id (FK -> solicitudes_adopcion.id)
- tipo_documento
- url_documento
- nombre_archivo

## 5. Normalización

### Primera Forma Normal (1FN)
- Todas las tablas tienen filas únicas y atributos atómicos.

### Segunda Forma Normal (2FN)
- Todas las tablas tienen PK simples o compuestas, y no hay dependencias parciales (todos los atributos dependen de la PK).

### Tercera Forma Normal (3FN)
- No existen dependencias transitivas: todos los atributos dependen únicamente de la PK.

## 6. Registros de ejemplo

### usuarios
| id | user_name | email | password | fecha_creacion | activo | rol |
|----|-----------|-------|----------|---------------|--------|-----|
| u1 | juanperez | juan@correo.com | hash1 | 2025-08-01 | 1 | ROLE_USER |
| u2 | maria123  | maria@correo.com | hash2 | 2025-08-02 | 1 | ROLE_USER |
| u3 | admin     | admin@correo.com | hash3 | 2025-08-03 | 1 | ROLE_ADMIN |
| u4 | luisito   | luis@correo.com  | hash4 | 2025-08-04 | 1 | ROLE_USER |
| u5 | carla     | carla@correo.com | hash5 | 2025-08-05 | 1 | ROLE_USER |

### perfiles_usuario
| id | usuario_id | nombre_real | telefono | fecha_nacimiento |
|----|------------|-------------|----------|------------------|
| p1 | u1         | Juan Perez  | 5551111  | 1990-01-01       |
| p2 | u2         | Maria Lopez | 5552222  | 1985-05-10       |
| p3 | u3         | Admin Root  | 5553333  | 1980-12-12       |
| p4 | u4         | Luis Garcia | 5554444  | 1992-07-07       |
| p5 | u5         | Carla Ruiz  | 5555555  | 1995-03-03       |

### perros
| id | nombre   | edad | sexo | tamano | raza      | descripcion         | ubicacion | fecha_publicacion | estado_adopcion | usuario_id |
|----|----------|------|------|--------|-----------|---------------------|-----------|-------------------|-----------------|------------|
| d1 | Rocky    | 3    | M    | Mediano| Labrador  | Juguetón y sociable | CDMX      | 2025-08-10        | Disponible      | u1         |
| d2 | Luna     | 2    | F    | Pequeño| Poodle    | Muy cariñosa        | CDMX      | 2025-08-11        | Adoptado        | u2         |
| d3 | Max      | 5    | M    | Grande | Pastor    | Protector           | Toluca    | 2025-08-12        | Disponible      | u1         |
| d4 | Nala     | 1    | F    | Mediano| Mestizo   | Energética          | Puebla    | 2025-08-13        | Disponible      | u4         |
| d5 | Toby     | 4    | M    | Pequeño| Chihuahua | Tranquilo           | CDMX      | 2025-08-14        | Disponible      | u5         |

### imagenes_perros
| id | perro_id | url                | descripcion      | principal |
|----|----------|--------------------|------------------|-----------|
| i1 | d1       | /uploads/perritos/1.jpg | Rocky jugando   | 1         |
| i2 | d2       | /uploads/perritos/2.png | Luna dormida    | 1         |
| i3 | d3       | /uploads/perritos/3.jpg | Max en el parque| 1         |
| i4 | d4       | /uploads/perritos/4.jpg | Nala corriendo  | 1         |
| i5 | d5       | /uploads/perritos/5.jpg | Toby sentado    | 1         |

### solicitudes_adopcion
| id | perro_id | solicitante_id | estado     | mensaje           | fecha_solicitud |
|----|----------|----------------|------------|-------------------|-----------------|
| s1 | d1       | u2             | Pendiente  | Quiero adoptar    | 2025-08-15      |
| s2 | d2       | u1             | Aceptada   | Luna es perfecta  | 2025-08-16      |
| s3 | d3       | u5             | Rechazada  | Max me interesa   | 2025-08-17      |
| s4 | d4       | u2             | Pendiente  | Nala es linda     | 2025-08-18      |
| s5 | d5       | u4             | Pendiente  | Toby para mi hijo | 2025-08-19      |

### documentos_solicitud
| id | solicitud_id | tipo_documento | url_documento           | nombre_archivo |
|----|--------------|---------------|-------------------------|----------------|
| ds1| s1           | INE           | /uploads/docs/ine1.pdf  | ine1.pdf       |
| ds2| s2           | Comprobante   | /uploads/docs/comp2.pdf | comp2.pdf      |
| ds3| s3           | INE           | /uploads/docs/ine3.pdf  | ine3.pdf       |
| ds4| s4           | Carta         | /uploads/docs/carta4.pdf| carta4.pdf     |
| ds5| s5           | INE           | /uploads/docs/ine5.pdf  | ine5.pdf       |

**Consulta de verificación:**
```sql
SELECT * FROM usuarios;
SELECT * FROM perfiles_usuario;
SELECT * FROM perros;
SELECT * FROM imagenes_perros;
SELECT * FROM solicitudes_adopcion;
SELECT * FROM documentos_solicitud;
```

## 7. Exportación de datos a XML y JSON

### perros.xml
```xml
<perros>
  <perro>
    <id>d1</id>
    <nombre>Rocky</nombre>
    <edad>3</edad>
    <sexo>M</sexo>
    <tamano>Mediano</tamano>
    <raza>Labrador</raza>
    <descripcion>Juguetón y sociable</descripcion>
    <ubicacion>CDMX</ubicacion>
    <estado_adopcion>Disponible</estado_adopcion>
    <usuario_id>u1</usuario_id>
  </perro>
  <perro>
    <id>d2</id>
    <nombre>Luna</nombre>
    <edad>2</edad>
    <sexo>F</sexo>
    <tamano>Pequeño</tamano>
    <raza>Poodle</raza>
    <descripcion>Muy cariñosa</descripcion>
    <ubicacion>CDMX</ubicacion>
    <estado_adopcion>Adoptado</estado_adopcion>
    <usuario_id>u2</usuario_id>
  </perro>
  <perro>
    <id>d3</id>
    <nombre>Max</nombre>
    <edad>5</edad>
    <sexo>M</sexo>
    <tamano>Grande</tamano>
    <raza>Pastor</raza>
    <descripcion>Protector</descripcion>
    <ubicacion>Toluca</ubicacion>
    <estado_adopcion>Disponible</estado_adopcion>
    <usuario_id>u1</usuario_id>
  </perro>
  <perro>
    <id>d4</id>
    <nombre>Nala</nombre>
    <edad>1</edad>
    <sexo>F</sexo>
    <tamano>Mediano</tamano>
    <raza>Mestizo</raza>
    <descripcion>Energética</descripcion>
    <ubicacion>Puebla</ubicacion>
    <estado_adopcion>Disponible</estado_adopcion>
    <usuario_id>u4</usuario_id>
  </perro>
  <perro>
    <id>d5</id>
    <nombre>Toby</nombre>
    <edad>4</edad>
    <sexo>M</sexo>
    <tamano>Pequeño</tamano>
    <raza>Chihuahua</raza>
    <descripcion>Tranquilo</descripcion>
    <ubicacion>CDMX</ubicacion>
    <estado_adopcion>Disponible</estado_adopcion>
    <usuario_id>u5</usuario_id>
  </perro>
</perros>
```

### usuarios.xml
```xml
<usuarios>
  <usuario>
    <id>u1</id>
    <user_name>juanperez</user_name>
    <email>juan@correo.com</email>
    <rol>ROLE_USER</rol>
  </usuario>
  <usuario>
    <id>u2</id>
    <user_name>maria123</user_name>
    <email>maria@correo.com</email>
    <rol>ROLE_USER</rol>
  </usuario>
  <usuario>
    <id>u3</id>
    <user_name>admin</user_name>
    <email>admin@correo.com</email>
    <rol>ROLE_ADMIN</rol>
  </usuario>
  <usuario>
    <id>u4</id>
    <user_name>luisito</user_name>
    <email>luis@correo.com</email>
    <rol>ROLE_USER</rol>
  </usuario>
  <usuario>
    <id>u5</id>
    <user_name>carla</user_name>
    <email>carla@correo.com</email>
    <rol>ROLE_USER</rol>
  </usuario>
</usuarios>
```

### solicitudes_adopcion.xml
```xml
<solicitudes>
  <solicitud>
    <id>s1</id>
    <perro_id>d1</perro_id>
    <solicitante_id>u2</solicitante_id>
    <estado>Pendiente</estado>
    <mensaje>Quiero adoptar</mensaje>
    <fecha_solicitud>2025-08-15</fecha_solicitud>
  </solicitud>
  <solicitud>
    <id>s2</id>
    <perro_id>d2</perro_id>
    <solicitante_id>u1</solicitante_id>
    <estado>Aceptada</estado>
    <mensaje>Luna es perfecta</mensaje>
    <fecha_solicitud>2025-08-16</fecha_solicitud>
  </solicitud>
  <solicitud>
    <id>s3</id>
    <perro_id>d3</perro_id>
    <solicitante_id>u5</solicitante_id>
    <estado>Rechazada</estado>
    <mensaje>Max me interesa</mensaje>
    <fecha_solicitud>2025-08-17</fecha_solicitud>
  </solicitud>
  <solicitud>
    <id>s4</id>
    <perro_id>d4</perro_id>
    <solicitante_id>u2</solicitante_id>
    <estado>Pendiente</estado>
    <mensaje>Nala es linda</mensaje>
    <fecha_solicitud>2025-08-18</fecha_solicitud>
  </solicitud>
  <solicitud>
    <id>s5</id>
    <perro_id>d5</perro_id>
    <solicitante_id>u4</solicitante_id>
    <estado>Pendiente</estado>
    <mensaje>Toby para mi hijo</mensaje>
    <fecha_solicitud>2025-08-19</fecha_solicitud>
  </solicitud>
</solicitudes>
```

### perros.json
```json
[
  {
    "id": "d1",
    "nombre": "Rocky",
    "edad": 3,
    "sexo": "M",
    "tamano": "Mediano",
    "raza": "Labrador",
    "descripcion": "Juguetón y sociable",
    "ubicacion": "CDMX",
    "estado_adopcion": "Disponible",
    "usuario_id": "u1"
  },
  {
    "id": "d2",
    "nombre": "Luna",
    "edad": 2,
    "sexo": "F",
    "tamano": "Pequeño",
    "raza": "Poodle",
    "descripcion": "Muy cariñosa",
    "ubicacion": "CDMX",
    "estado_adopcion": "Adoptado",
    "usuario_id": "u2"
  },
  {
    "id": "d3",
    "nombre": "Max",
    "edad": 5,
    "sexo": "M",
    "tamano": "Grande",
    "raza": "Pastor",
    "descripcion": "Protector",
    "ubicacion": "Toluca",
    "estado_adopcion": "Disponible",
    "usuario_id": "u1"
  },
  {
    "id": "d4",
    "nombre": "Nala",
    "edad": 1,
    "sexo": "F",
    "tamano": "Mediano",
    "raza": "Mestizo",
    "descripcion": "Energética",
    "ubicacion": "Puebla",
    "estado_adopcion": "Disponible",
    "usuario_id": "u4"
  },
  {
    "id": "d5",
    "nombre": "Toby",
    "edad": 4,
    "sexo": "M",
    "tamano": "Pequeño",
    "raza": "Chihuahua",
    "descripcion": "Tranquilo",
    "ubicacion": "CDMX",
    "estado_adopcion": "Disponible",
    "usuario_id": "u5"
  }
]
```

### usuarios.json
```json
[
  {
    "id": "u1",
    "user_name": "juanperez",
    "email": "juan@correo.com",
    "rol": "ROLE_USER"
  },
  {
    "id": "u2",
    "user_name": "maria123",
    "email": "maria@correo.com",
    "rol": "ROLE_USER"
  },
  {
    "id": "u3",
    "user_name": "admin",
    "email": "admin@correo.com",
    "rol": "ROLE_ADMIN"
  },
  {
    "id": "u4",
    "user_name": "luisito",
    "email": "luis@correo.com",
    "rol": "ROLE_USER"
  },
  {
    "id": "u5",
    "user_name": "carla",
    "email": "carla@correo.com",
    "rol": "ROLE_USER"
  }
]
```

### solicitudes_adopcion.json
```json
[
  {
    "id": "s1",
    "perro_id": "d1",
    "solicitante_id": "u2",
    "estado": "Pendiente",
    "mensaje": "Quiero adoptar",
    "fecha_solicitud": "2025-08-15"
  },
  {
    "id": "s2",
    "perro_id": "d2",
    "solicitante_id": "u1",
    "estado": "Aceptada",
    "mensaje": "Luna es perfecta",
    "fecha_solicitud": "2025-08-16"
  },
  {
    "id": "s3",
    "perro_id": "d3",
    "solicitante_id": "u5",
    "estado": "Rechazada",
    "mensaje": "Max me interesa",
    "fecha_solicitud": "2025-08-17"
  },
  {
    "id": "s4",
    "perro_id": "d4",
    "solicitante_id": "u2",
    "estado": "Pendiente",
    "mensaje": "Nala es linda",
    "fecha_solicitud": "2025-08-18"
  },
  {
    "id": "s5",
    "perro_id": "d5",
    "solicitante_id": "u4",
    "estado": "Pendiente",
    "mensaje": "Toby para mi hijo",
    "fecha_solicitud": "2025-08-19"
  }
]
```
