# Document API Documentation

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

## Document Endpoints

### 1. Upload Document
**POST** `/workspaces/{workspaceId}/documents`

Upload a text file to the workspace. Files are automatically assigned the next `orderIndex`.

#### Request
`Content-Type: multipart/form-data`

| Field | Type | Description |
|-------|------|-------------|
| file | File | Text file to upload |

#### Response `201 Created`
```json
{
  "success": true,
  "message": "Document uploaded successfully",
  "data": {
    "id": "880e8400-e29b-41d4-a716-446655440003",
    "name": "chapter1.txt",
    "orderIndex": 0,
    "status": "UPLOADED",
    "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
    "storedFileUrl": "https://res.cloudinary.com/.../chapter1.txt",
    "tokenStartIndex": null,
    "tokenEndIndex": null,
    "createdAt": "2024-12-25T04:00:00Z",
    "updatedAt": "2024-12-25T04:00:00Z"
  }
}
```

#### Errors
| Status | Error | Description |
|--------|-------|-------------|
| 404 | RESOURCE_NOT_FOUND | Workspace not found |
| 400 | BAD_REQUEST | Invalid file or upload error |

---

### 2. List Documents
**GET** `/workspaces/{workspaceId}/documents`

Get all documents in a workspace, ordered by `orderIndex`.

#### Response `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": "880e8400-e29b-41d4-a716-446655440003",
      "name": "chapter1.txt",
      "orderIndex": 0,
      "status": "IMPORTED",
      "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
      "storedFileUrl": "https://res.cloudinary.com/.../chapter1.txt",
      "tokenStartIndex": 0,
      "tokenEndIndex": 150,
      "createdAt": "2024-12-25T04:00:00Z",
      "updatedAt": "2024-12-25T04:05:00Z"
    },
    {
      "id": "990e8400-e29b-41d4-a716-446655440004",
      "name": "chapter2.txt",
      "orderIndex": 1,
      "status": "IMPORTED",
      "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
      "storedFileUrl": "https://res.cloudinary.com/.../chapter2.txt",
      "tokenStartIndex": 151,
      "tokenEndIndex": 320,
      "createdAt": "2024-12-25T04:01:00Z",
      "updatedAt": "2024-12-25T04:05:00Z"
    }
  ]
}
```

---

### 3. Get Document
**GET** `/documents/{id}`

Get a single document by ID.

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "880e8400-e29b-41d4-a716-446655440003",
    "name": "chapter1.txt",
    "orderIndex": 0,
    "status": "IMPORTED",
    "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
    "storedFileUrl": "https://res.cloudinary.com/.../chapter1.txt",
    "tokenStartIndex": 0,
    "tokenEndIndex": 150,
    "createdAt": "2024-12-25T04:00:00Z",
    "updatedAt": "2024-12-25T04:05:00Z"
  }
}
```

#### Errors
| Status | Error | Description |
|--------|-------|-------------|
| 404 | RESOURCE_NOT_FOUND | Document not found |

---

### 4. Update Document Status
**PUT** `/documents/{id}/status?status={newStatus}`

Update the document's processing status.

#### Query Parameters
| Parameter | Type | Values |
|-----------|------|--------|
| status | string | `UPLOADED`, `IMPORTED`, `ANNOTATING`, `COMPLETE` |

#### Response `200 OK`
```json
{
  "success": true,
  "message": "Document status updated",
  "data": {
    "id": "880e8400-e29b-41d4-a716-446655440003",
    "status": "ANNOTATING",
    ...
  }
}
```

---

### 5. Delete Document
**DELETE** `/documents/{id}`

Delete a document and its stored file.

#### Response `200 OK`
```json
{
  "success": true,
  "message": "Document deleted successfully",
  "data": null
}
```

---

### 6. Get Document Count
**GET** `/workspaces/{workspaceId}/documents/count`

Get the number of documents in a workspace.

#### Response `200 OK`
```json
{
  "success": true,
  "data": 5
}
```

---

## Enums

### DocumentStatus
| Value | Description |
|-------|-------------|
| `UPLOADED` | Document uploaded but not yet processed |
| `IMPORTED` | Document has been tokenized |
| `ANNOTATING` | Annotation work in progress |
| `COMPLETE` | Annotation work finished |

---

## Continuous Tokenization

Documents in a workspace share a continuous token index space:

| Document | orderIndex | tokenStartIndex | tokenEndIndex |
|----------|------------|-----------------|---------------|
| chapter1.txt | 0 | 0 | 150 |
| chapter2.txt | 1 | 151 | 320 |
| chapter3.txt | 2 | 321 | 500 |

This allows treating all documents as one continuous annotation task.
