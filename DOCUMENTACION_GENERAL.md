# Documentación General del Proyecto: PerritosCutApp

## Introducción

**PerritosCutApp** es una plataforma digital integral para la gestión, visualización y registro de información sobre perros esto con el fin
de tener un sistema de adopcion más sencillo y eficiente donde se tenga un control estricto de quien adopta por medio de validaciones 
de un grupo de seguimiento a las peticiones que tendra un panel de control administrativo.
Está diseñada para ofrecer una experiencia moderna, segura y eficiente tanto a usuarios finales como a administradores, 
facilitando la interacción entre amantes de los perros, profesionales y servicios relacionados con el fin de proporcionarle
a diferentes organizaciones un sistema replicable y escalable.

## Propósito y Objetivos

El objetivo principal de PerritosCutApp es proporcionar una solución digital que permita:
- Registrar y gestionar perfiles de perros, incluyendo datos y multimedia.
- Visualizar información relevante y filtrada de perros en un catálogo público.
- Implementar un panel de control administrativo para gestionar usuarios, perros y contenido.
- Integrar una experiencia de usuario fluida y segura, con validación y protección de datos.
- Facilitar la administración y el despliegue eficiente mediante buenas prácticas DevOps y documentación técnica.

## Flujo General del Proyecto

### 1. Registro y Autenticación
- El usuario puede registrarse mediante un formulario en la interfaz, enviando los datos al endpoint `/api/registro`.
- Tras el registro, se solicita la verificación de correo electrónico, gestionada por los endpoints `/api/verify/email/request` y `/api/verify/email/confirm`.
- El login se realiza en `/api/login`, y el sistema gestiona la sesión y el refresh de tokens.
- La recuperación de contraseña se realiza desde la UI, consumiendo los endpoints `/api/forgot` y `/api/reset`.

### 2. Catálogo y Gestión de Perros
- El catálogo público de perros se muestra en la ruta `/perros`, consumiendo el endpoint `/api/perros/catalogo` con filtros por sexo, tamaño, ubicación y paginación.
- Los usuarios autenticados pueden gestionar sus propios perros (listar, crear, editar), usando endpoints como `/perros/mis` y `/perros`.
- La creación de perros incluye validación de datos y subida de imágenes.

### 3. Servicios Premium y Administración
- Los usuarios pueden acceder a servicios premium tras el registro y verificación, con rutas y vistas específicas en el frontend.
- Los administradores disponen de herramientas para gestionar usuarios, perros y contenido, accediendo a endpoints protegidos y vistas administrativas.

### 4. Seguridad Robusta y Validación
- El backend implementa autenticación y autorización mediante Spring Security, con control de acceso por roles y validación de tokens.
- Los endpoints sensibles están protegidos y solo accesibles para usuarios autenticados y con permisos adecuados.
- Se utiliza cifrado seguro para contraseñas (BCrypt) y validación exhaustiva de datos en todos los endpoints.
- El frontend gestiona la sesión y los tokens de forma segura, verificando la expiración y el estado antes de permitir el acceso a rutas protegidas.
- Los roles de usuario determinan el acceso a paneles y funcionalidades administrativas.
- Todas las llamadas HTTP pasan por interceptores que gestionan la autenticación, el refresh de tokens y el manejo de errores.
- La infraestructura usa Docker y Nginx para aislar servicios y proteger la comunicación.
- Pruebas automatizadas verifican el correcto funcionamiento de los controles de seguridad.

## Arquitectura General

El proyecto está estructurado en dos grandes capas:

### Backend
- **Lenguaje principal:** Java
- **Framework:** Spring Boot
- **Gestión de dependencias:** Maven
- **Base de datos:** Integración y migración mediante Flyway
- **Pruebas:** Unitarias e integración (JUnit)
- **Contenedores:** Docker y docker-compose para despliegue
- **Seguridad:** Autenticación, validación y control de acceso
- **Documentación técnica:** Archivos .md y ejemplos JSON
- **Controladores principales:** Autenticación, registro, recuperación, verificación, gestión de perros, imágenes, adopciones y errores personalizados

### Frontend
- **Framework principal:** Vue.js
- **Herramienta de construcción:** Vite
- **Estilos:** Tailwind CSS
- **Servidor de archivos estáticos:** Nginx
- **Estructura modular:** Componentes, páginas, vistas, utilidades y assets
- **Gestión de estado:** Pinia (stores para auth, perros, etc.)
- **Integración con backend:** Consumo de endpoints REST, validación y manejo de errores
- **Plantillas HTML:** Para formularios, errores, miscelánea, registro premium y visualización de perros

## Tecnologías Utilizadas

- **Java, Spring Boot, Maven** (backend)
- **Vue.js, Vite, Tailwind CSS, Pinia** (frontend)
- **Docker, docker-compose, Nginx** (infraestructura y despliegue)
- **Flyway** (migración de base de datos)
- **JUnit** (pruebas)
- **Insomnia** (pruebas de API)

## Estado Actual y Logros

- Arquitectura modular y escalable implementada
- Integración completa entre frontend y backend
- Pruebas automatizadas y documentación técnica
- Despliegue mediante Docker y Nginx
- Seguridad y validación en todos los flujos
- Plantillas y vistas funcionales para los principales flujos de usuario

## Planes y Visión Futura

- Mejorar la experiencia de usuario y la integración de servicios premium
- Ampliar la lógica de negocio y la presentación visual
- Fortalecer la seguridad y la gestión de roles
- Automatizar aún más el despliegue y la monitorización
- Documentar y expandir la API para integraciones externas

## Seguridad y Buenas Prácticas

- Validación de datos en backend y frontend
- Autenticación y autorización en endpoints críticos
- Uso de Docker y Nginx para aislar y proteger servicios
- Documentación clara y actualizada para facilitar auditorías y mantenimientos



---

**PerritosCutApp** representa una solución moderna y segura para la gestión de información sobre perros, con una visión clara de crecimiento 
y mejora continua, apoyada en buenas prácticas de desarrollo y despliegue.
