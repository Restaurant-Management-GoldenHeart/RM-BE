# Auth Setup Guide

## 1. Dependencies already added

The project already includes:

- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- `io.jsonwebtoken:jjwt-api:0.12.6`
- `io.jsonwebtoken:jjwt-impl:0.12.6`
- `io.jsonwebtoken:jjwt-jackson:0.12.6`

If you want to add them manually in another project, use:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-validation'
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
```

## 2. Environment variable

You must provide a JWT secret before running the application.

Example in PowerShell:

```powershell
$env:JWT_SECRET="this-is-a-very-strong-secret-key-with-at-least-32-chars"
./gradlew bootRun
```

Notes:

- secret should be at least 32 characters
- do not commit the real secret into source code

## 3. Endpoints

Base path:

```text
/api/v1/auth
```

Available endpoints:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

## 4. Request examples

Register:

```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "StrongPass123",
  "fullName": "John Doe"
}
```

Login:

```json
{
  "username": "john_doe",
  "password": "StrongPass123"
}
```

## 5. Response flow

After login:

- `accessToken` is returned in JSON body
- `refreshToken` is returned in `HttpOnly` cookie
- a hashed copy of the refresh token is stored in the `refresh_tokens` table

For protected APIs:

```http
Authorization: Bearer <accessToken>
```

When access token expires:

- call `POST /api/v1/auth/refresh`
- backend reads refresh token from cookie
- backend validates the refresh token in both JWT layer and database layer
- backend revokes the old refresh token record
- backend returns a new access token and rotates refresh token

## 6. Refresh token table

The backend now stores refresh tokens in table:

```text
refresh_tokens
```

Important notes:

- the database stores only `token_hash`, not the raw token
- one user can have many refresh tokens over time
- logout revokes the current token in database
- refresh also revokes the old token and creates a new one
