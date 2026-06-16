# CuidApp — Backend (Microservicios)

Backend de **CuidApp**, plataforma para el cuidado y acompañamiento en la gestión
de salud (medicamentos, recordatorios, controles y vínculo cuidador–paciente).

Este repositorio contiene los **microservicios** en Spring Boot. La app móvil
(Flutter) vive en un repositorio aparte.

---

## 📋 Descripción del proyecto

Arquitectura de **microservicios** detrás de un **API Gateway** único. Cada
servicio tiene su propia base de datos MySQL. La seguridad se basa en **JWT**:
el gateway valida el token y reenvía la identidad del usuario a los servicios.

Servicios:

| Servicio | Puerto | Responsabilidad |
|----------|--------|-----------------|
| **apigateway-new** | 8083 | Punto de entrada público; enruta y valida JWT (Spring Cloud Gateway) |
| **autenticacioncuidapp** | 8081 | Registro, login, perfil, JWT y vinculación cuidador–paciente |
| **medicamentoscuidapp** | 8082 | Gestión de medicamentos y disparo de recordatorios |
| **notificacionescuidapp** | 8084 | Notificaciones push (FCM), tokens de dispositivo, SOS y scheduler |
| **catalogocuidapp** | 8085 | Catálogo de datos de referencia |

Funcionalidades destacadas: autenticación JWT, vinculación cuidador–paciente,
**notificaciones push con Firebase Cloud Messaging** (bienvenida, recordatorios y
**alerta SOS** del paciente al cuidador).

---

## 🛠️ Tecnologías utilizadas

| Categoría | Tecnología |
|-----------|------------|
| Lenguaje | **Java 21** |
| Framework | **Spring Boot 4.0.6** (Web MVC, Data JPA, Security, Validation) |
| Gateway | **Spring Cloud Gateway** (WebFlux) |
| Base de datos | **MySQL 8** + Hibernate/JPA |
| Seguridad | **JWT** (jjwt) |
| Push | **Firebase Admin SDK** (Cloud Messaging) |
| Build | **Maven** |
| Contenedores | **Docker** y **Docker Compose** |
| Despliegue | Oracle Cloud / AWS EC2 (ver guías de deploy) |

---

## 📂 Estructura del proyecto

```
.
├── apigateway-new/            # API Gateway (entrada pública, enrutamiento, JWT)
├── autenticacioncuidapp/      # Autenticación, perfiles y vinculación
├── medicamentoscuidapp/       # Medicamentos y recordatorios
├── notificacionescuidapp/     # Push (FCM), tokens, SOS, scheduler
├── catalogocuidapp/           # Catálogo de referencia
├── docker-compose.yml         # Orquestación local (MySQL externo / Laragon)
├── docker-compose.aws.yml     # Orquestación de producción (MySQL incluido)
├── DEPLOY-ORACLE.md           # Guía de despliegue gratis (Oracle Cloud)
└── DEPLOY-AWS.md              # Guía de despliegue en AWS EC2
```

Cada microservicio sigue la estructura estándar de Spring Boot
(`controller/`, `service/`, `repository/`, `model/`, `dto/`, `config/`).

---

## 🚀 Cómo ejecutar (local con Docker)

```bash
# Levantar todos los servicios
docker compose up --build -d

# Ver estado y logs
docker compose ps
docker compose logs -f apigateway
```

El backend queda disponible en `http://localhost:8083`.

### Variables / secretos
- `JWT_SECRET`: clave para firmar los tokens (compartida gateway ↔ auth).
- Push (FCM): requiere `firebase-service-account.json` en la raíz y
  `FCM_ENABLED=true` (ver guías de deploy y `FIREBASE-SETUP.md` del frontend).
- **Nunca subir a git** `.env` ni `firebase-service-account.json` (ya excluidos en `.gitignore`).

---

## 👥 Estructura del equipo

| Nombre | Rol | Responsabilidades |
|--------|-----|-------------------|
| **Matías Samaniego** | Líder de proyecto / Full-Stack | Coordinación, integración frontend–backend, notificaciones push |
| **Francisco Gómez** | Desarrollo Backend | Microservicios, API Gateway, base de datos y seguridad (JWT) |
| **Ricardo Díaz** | Desarrollo Frontend | App Flutter, pantallas, mapa y experiencia de usuario |
