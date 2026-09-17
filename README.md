# Escuela Colombiana de Ingeniería Julio Garavito
## Arquitectura de Software – ARSW
### Laboratorio – Parte 2: BluePrints API con Seguridad JWT (OAuth 2.0)

Este laboratorio extiende la **Parte 1** ([Lab_P1_BluePrints_Java21_API](https://github.com/DECSIS-ECI/Lab_P1_BluePrints_Java21_API)) agregando **seguridad a la API** usando **Spring Boot 3, Java 21 y JWT (OAuth 2.0)**.  
El API se convierte en un **Resource Server** protegido por tokens Bearer firmados con **RS256**.  
Incluye un endpoint didáctico `/auth/login` que emite el token para facilitar las pruebas.

---

## Objetivos
- Implementar seguridad en servicios REST usando **OAuth2 Resource Server**.
- Configurar emisión y validación de **JWT**.
- Proteger endpoints con **roles y scopes** (`blueprints.read`, `blueprints.write`).
- Integrar la documentación de seguridad en **Swagger/OpenAPI**.

---

## Requisitos
- JDK 21
- Maven 3.9+
- Git

---

## Ejecución del proyecto
1. Clonar o descomprimir el proyecto:
   ```bash
   git clone https://github.com/DECSIS-ECI/Lab_P2_BluePrints_Java21_API_Security_JWT.git
   cd Lab_P2_BluePrints_Java21_API_Security_JWT
   ```
   ó si el profesor entrega el `.zip`, descomprimirlo y entrar en la carpeta.

2. Ejecutar con Maven:
   ```bash
   mvn -q -DskipTests spring-boot:run
   ```

3. Verificar que la aplicación levante en `http://localhost:8080`.

---

## Endpoints principales

### 1. Login (emite token)
```
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "username": "student",
  "password": "student123"
}
```
Respuesta:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

Usuarios disponibles (`InMemoryUserService`) — con **scopes distintos** para poder probar el control de acceso:

| Usuario     | Password        | Scopes emitidos                     |
|-------------|------------------|--------------------------------------|
| `student`   | `student123`     | `blueprints.read`                    |
| `assistant` | `assistant123`   | `blueprints.read blueprints.write`   |

### 2. Endpoints de negocio (heredados y asegurados del Lab P1)
Todos viven bajo `/api/blueprints` y están protegidos por JWT + scopes:

| Método | Endpoint                                   | Scope requerido      | Descripción                              |
|--------|---------------------------------------------|-----------------------|-------------------------------------------|
| GET    | `/api/blueprints`                            | `blueprints.read`     | Lista todos los blueprints                |
| GET    | `/api/blueprints/{author}`                   | `blueprints.read`     | Lista los blueprints de un autor          |
| GET    | `/api/blueprints/{author}/{bpname}`          | `blueprints.read`     | Consulta un blueprint puntual             |
| POST   | `/api/blueprints`                            | `blueprints.write`    | Crea un nuevo blueprint                   |
| PUT    | `/api/blueprints/{author}/{bpname}/points`   | `blueprints.write`    | Agrega un punto a un blueprint existente  |

Ejemplo — consultar blueprints (requiere scope `blueprints.read`):
```
GET http://localhost:8080/api/blueprints
Authorization: Bearer <ACCESS_TOKEN>
```

Ejemplo — crear blueprint (requiere scope `blueprints.write`; con un token de `student` responde `403 Forbidden`):
```
POST http://localhost:8080/api/blueprints
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json

{
  "author": "student",
  "name": "Nuevo Plano",
  "points": [ { "x": 0, "y": 0 }, { "x": 5, "y": 5 } ]
}
```

Más ejemplos listos para ejecutar en [api.http](api.http).

---

## Swagger UI
- URL: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- Pulsa **Authorize**, ingresa el token en el formato:
  ```
  Bearer eyJhbGciOi...
  ```

---

## Estructura del proyecto
```
src/main/java/co/edu/eci/blueprints/
  ├── api/BlueprintController.java       # Endpoints de negocio (CRUD de blueprints), protegidos por scope
  ├── auth/AuthController.java           # Login didáctico para emitir tokens
  ├── config/OpenApiConfig.java          # Configuración Swagger + JWT
  ├── model/                             # Blueprint, Point (del Lab P1)
  ├── persistence/                       # BlueprintPersistence + implementación en memoria y excepciones
  ├── filters/                           # BlueprintsFilter e implementaciones (identity/redundancy/undersampling)
  ├── services/BlueprintsServices.java   # Lógica de negocio (del Lab P1)
  └── security/
       ├── SecurityConfig.java
       ├── MethodSecurityConfig.java
       ├── JwtKeyProvider.java
       ├── InMemoryUserService.java
       └── RsaKeyProperties.java
src/test/java/co/edu/eci/blueprints/
  └── BlueprintsApiSecurityTest.java     # Pruebas de seguridad (401/403/200/201) con MockMvc
src/main/resources/
  └── application.yml
```

---

## Actividades propuestas
1. **Revisar `SecurityConfig`**: los endpoints públicos (`/auth/login`, `/swagger-ui/**`, `/v3/api-docs/**`) se declaran con `permitAll()`; el resto de `/api/**` exige estar autenticado y tener al menos uno de los scopes `blueprints.read`/`blueprints.write` a nivel de filtro (`hasAnyAuthority`), y cada método del controlador refina el scope exacto requerido con `@PreAuthorize`.
2. **Explorar el flujo de login**: `POST /auth/login` valida credenciales contra `InMemoryUserService` (passwords con BCrypt) y emite un JWT firmado RS256 con claims `iss`, `sub`, `iat`, `exp` y `scope`. La llave RSA se genera en memoria al arrancar (`JwtKeyProvider`) — por eso no se usa un `jwk-set-uri` externo, sino los beans `JwtEncoder`/`JwtDecoder` definidos en `SecurityConfig`.
3. **Scopes controlando el API del Lab P1**: se integraron los endpoints originales de blueprints (`model`, `persistence`, `services` y `filters` del Lab P1) bajo `/api/blueprints`, cada uno protegido con `@PreAuthorize` según si es operación de lectura (`SCOPE_blueprints.read`) o escritura (`SCOPE_blueprints.write`). Para poder observar la diferencia, el usuario `student` solo recibe el scope de lectura y `assistant` recibe lectura+escritura (ver [InMemoryUserService.java](src/main/java/co/edu/eci/blueprints/security/InMemoryUserService.java)).
4. **Modificar el tiempo de expiración**: cambia `blueprints.security.token-ttl-seconds` en [application.yml](src/main/resources/application.yml), reinicia la app, y observa cómo el `exp` del JWT (decódalo en [jwt.io](https://jwt.io)) y el campo `expires_in` de la respuesta cambian. Con un valor bajo (p. ej. `8`) puedes ver el token expirar y las siguientes peticiones responder `401`; **ten en cuenta que Spring Security aplica por defecto una tolerancia de reloj (`clock skew`) de 60 segundos al validar el claim `exp`**, así que con TTL=8s hay que esperar más de ~68s tras emitir el token para que la petición sea rechazada.
5. **Swagger**: [OpenApiConfig.java](src/main/java/co/edu/eci/blueprints/config/OpenApiConfig.java) define el esquema `bearer-jwt`, y cada endpoint tiene anotaciones `@Tag`/`@Operation` (`/auth/login` está además marcado con `@SecurityRequirements` vacío para no mostrar el candado, ya que no requiere token).

---

## Lecturas recomendadas
- [Spring Security Reference – OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [Spring Boot – Securing Web Applications](https://spring.io/guides/gs/securing-web/)
- [JSON Web Tokens – jwt.io](https://jwt.io/introduction)

---

## Licencia
Proyecto educativo con fines académicos – Escuela Colombiana de Ingeniería Julio Garavito.
