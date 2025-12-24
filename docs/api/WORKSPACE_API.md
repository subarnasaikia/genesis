# Workspace API Documentation

## Base URL
```
http://localhost:8080/api
```

## Authentication
All endpoints require authentication via JWT access token:
```
Authorization: Bearer <accessToken>
```

---

## Workspace Endpoints

### 1. Create Workspace
**POST** `/workspaces`

Create a new workspace (project) for annotation work.

#### Request
```json
{
  "name": "My Annotation Project",
  "description": "Coreference resolution annotations",  // optional
  "annotationType": "COREF"  // COREF, NER, or POS
}
```

#### Response `201 Created`
```json
{
  "success": true,
  "message": "Workspace created successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "My Annotation Project",
    "description": "Coreference resolution annotations",
    "annotationType": "COREF",
    "status": "DRAFT",
    "ownerId": "660e8400-e29b-41d4-a716-446655440001",
    "ownerUsername": "john_doe",
    "createdAt": "2024-12-25T04:00:00Z",
    "updatedAt": "2024-12-25T04:00:00Z"
  }
}
```

#### Errors
| Status | Error | Description |
|--------|-------|-------------|
| 400 | VALIDATION_ERROR | Workspace name already exists for owner |
| 401 | UNAUTHORIZED | Missing or invalid token |

---

### 2. List My Workspaces
**GET** `/workspaces`

Get all workspaces owned by the authenticated user.

#### Response `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "My Annotation Project",
      "annotationType": "COREF",
      "status": "ACTIVE",
      "ownerId": "660e8400-e29b-41d4-a716-446655440001",
      "ownerUsername": "john_doe",
      "createdAt": "2024-12-25T04:00:00Z",
      "updatedAt": "2024-12-25T04:00:00Z"
    }
  ]
}
```

---

### 3. Get Workspace
**GET** `/workspaces/{id}`

Get a workspace by ID.

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "My Annotation Project",
    "description": "Coreference resolution annotations",
    "annotationType": "COREF",
    "status": "ACTIVE",
    "ownerId": "660e8400-e29b-41d4-a716-446655440001",
    "ownerUsername": "john_doe",
    "createdAt": "2024-12-25T04:00:00Z",
    "updatedAt": "2024-12-25T04:00:00Z"
  }
}
```

#### Errors
| Status | Error | Description |
|--------|-------|-------------|
| 404 | RESOURCE_NOT_FOUND | Workspace not found |

---

### 4. Update Workspace Status
**PUT** `/workspaces/{id}/status?status={newStatus}`

Update workspace status (DRAFT → ACTIVE → ARCHIVED).

#### Query Parameters
| Parameter | Type | Values |
|-----------|------|--------|
| status | string | `DRAFT`, `ACTIVE`, `ARCHIVED` |

#### Response `200 OK`
```json
{
  "success": true,
  "message": "Workspace status updated",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "ACTIVE",
    ...
  }
}
```

---

### 5. Delete Workspace
**DELETE** `/workspaces/{id}`

Delete a workspace and all associated documents.

#### Response `200 OK`
```json
{
  "success": true,
  "message": "Workspace deleted successfully",
  "data": null
}
```

---

## Member Management

### 6. Add Member
**POST** `/workspaces/{id}/members`

Add a user to the workspace with a specific role.

#### Request
```json
{
  "userId": "770e8400-e29b-41d4-a716-446655440002",
  "role": "ANNOTATOR"  // ADMIN, ANNOTATOR, or CURATOR
}
```

#### Response `201 Created`
```json
{
  "success": true,
  "message": "Member added successfully",
  "data": null
}
```

#### Errors
| Status | Error | Description |
|--------|-------|-------------|
| 400 | VALIDATION_ERROR | User is already a member |
| 404 | RESOURCE_NOT_FOUND | User not found |

---

### 7. Remove Member
**DELETE** `/workspaces/{id}/members/{userId}`

Remove a user from the workspace.

#### Response `200 OK`
```json
{
  "success": true,
  "message": "Member removed successfully",
  "data": null
}
```

---

## Enums

### WorkspaceStatus
| Value | Description |
|-------|-------------|
| `DRAFT` | Initial state, workspace is being set up |
| `ACTIVE` | Workspace is ready for annotation work |
| `ARCHIVED` | Workspace is archived, read-only |

### AnnotationType
| Value | Description |
|-------|-------------|
| `COREF` | Coreference Resolution |
| `NER` | Named Entity Recognition |
| `POS` | Part-of-Speech Tagging |

### MemberRole
| Value | Description |
|-------|-------------|
| `ADMIN` | Full access to workspace settings |
| `ANNOTATOR` | Can create and edit annotations |
| `CURATOR` | Can review and approve annotations |
