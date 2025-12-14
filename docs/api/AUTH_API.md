# Authentication API Documentation

## Base URL
```
http://localhost:8080/api/auth
```

## Endpoints

### 1. Sign Up
**POST** `/signup`

Create a new user account.

#### Request
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "securePassword123",
  "firstName": "John",
  "lastName": "Doe",
  "organizationName": "Acme Corp"  // optional
}
```

#### Response `201 Created`
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "organizationName": "Acme Corp",
    "emailVerified": false,
    "createdAt": "2024-12-14T15:30:00Z"
  }
}
```

#### Errors
| Status | Error | Description |
|--------|-------|-------------|
| 400 | VALIDATION_ERROR | Invalid input data |
| 400 | BAD_REQUEST | Username or email already exists |

---

### 2. Login
**POST** `/login`

Authenticate and receive JWT tokens.

#### Request
```json
{
  "usernameOrEmail": "john_doe",
  "password": "securePassword123"
}
```

#### Response `200 OK`
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

#### Errors
| Status | Error | Description |
|--------|-------|-------------|
| 401 | INVALID_CREDENTIALS | Wrong username/password |
| 403 | ACCOUNT_DISABLED | Account is disabled |
| 403 | ACCOUNT_LOCKED | Account is locked |

---

### 3. Refresh Token
**POST** `/refresh`

Get a new access token using refresh token.

#### Request
```json
{
  "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

#### Response `200 OK`
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

#### Errors
| Status | Error | Description |
|--------|-------|-------------|
| 400 | BAD_REQUEST | Invalid or expired refresh token |

---

### 4. Get Current User
**GET** `/me`

Get the authenticated user's profile. **Requires authentication.**

#### Headers
```
Authorization: Bearer <accessToken>
```

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "organizationName": "Acme Corp",
    "emailVerified": false,
    "createdAt": "2024-12-14T15:30:00Z"
  }
}
```

#### Errors
| Status | Error | Description |
|--------|-------|-------------|
| 401 | UNAUTHORIZED | Missing or invalid token |

---

### 5. Logout
**POST** `/logout`

Revoke refresh token to logout.

#### Request
```json
{
  "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

#### Response `200 OK`
```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null
}
```

---

## Authentication

All protected endpoints require the `Authorization` header:
```
Authorization: Bearer <accessToken>
```

## Token Lifetimes
| Token | Duration |
|-------|----------|
| Access Token | 15 minutes |
| Refresh Token | 7 days |

## Error Response Format
```json
{
  "success": false,
  "error": "ERROR_CODE",
  "message": "Human readable message",
  "path": "/api/auth/login",
  "status": 401,
  "timestamp": "2024-12-14T15:30:00Z"
}
```
