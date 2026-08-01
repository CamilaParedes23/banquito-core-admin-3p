# core-admin-service

Microservicio de Administración y Configuración del Core Bancario BanQuito V2.

## Responsabilidad

Gestiona catálogos y configuración operativa del banco:

- Sucursales.
- Feriados y calendario operativo.
- Siguiente día hábil.
- Parámetros del Core.
- Ventanas operativas y horarios de corte.
- Instituciones financieras / routing codes.
- Subtipos de cuenta.
- Subtipos de transacción.
- Perfiles operativos internos de empleados.

No gestiona autenticación ni RBAC. Eso pertenece a `identity-access-service`.
No gestiona tarifario de pagos masivos. Eso pertenece a Switch Facturación.

## Paquete base

```text
com.banquito.core.admin
```

## Ejecución local

```powershell
cd C:\banquito-core\core-admin-service
mvn clean package
mvn spring-boot:run
```

## URLs

- Health: <http://localhost:8083/actuator/health>
- Swagger UI: <http://localhost:8083/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8083/api-docs>

## Prueba rápida

```powershell
$loginBody = @{
  username = "admin.core"
  password = "password"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body $loginBody

$token = $loginResponse.accessToken

Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8083/api/v1/admin/branches" `
  -Headers @{ Authorization = "Bearer $token" }
```

## Docker

```powershell
docker build -t banquito/core-admin-service:local .
docker run --rm -p 8083:8083 --env-file .env.example banquito/core-admin-service:local
```

## Variables

Revisar `.env.example`.

## Kong

Ruta prevista:

```text
/api/v1/admin/**
```

## gRPC

Contrato previsto:

```text
src/main/proto/admin_catalog_service.proto
```

Uso interno esperado:

```text
core-account-service -> core-admin-service
```

para validar sucursales, subtipos de cuenta, subtipos de transacción, parámetros, ventanas operativas, feriados y routing codes.

## Base de datos

- Motor: MySQL 8.4 LTS.
- Base: `core_administrador_db`.
- Puerto local actual en infraestructura: `33062`.
- SQL de referencia: `docs/database/02_core_admin_db.sql`.

## Seguridad y autorización agregada

Las rutas `/api/v1/admin/**` y `/api/v1/audit/**` requieren JWT válido y rol interno (`ADMIN_SEGURIDAD`, `CAJERO` u `OPERADOR_CONTABLE`). Un cliente final autenticado no puede acceder a métricas, catálogos administrativos ni auditoría.
