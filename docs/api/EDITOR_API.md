# Editor API Documentation

## Base URL
```
http://localhost:8080/api/editor
```

## Authentication
All endpoints require authentication via JWT access token:
```
Authorization: Bearer <accessToken>
```

---

## Workspace Editor Endpoints

### 1. Open Workspace in Editor
**POST** `/workspaces/{workspaceId}/open`

Open a workspace in the annotation editor. Creates or retrieves an editor session and returns workspace info with documents.

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
    "workspaceName": "My Annotation Project",
    "session": {
      "id": "cc0e8400-e29b-41d4-a716-446655440030",
      "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
      "userId": "660e8400-e29b-41d4-a716-446655440001",
      "lastDocumentIndex": 0,
      "scrollPosition": 0,
      "lastAccessedAt": "2024-12-27T10:00:00Z"
    },
    "documents": [
      {
        "documentId": "880e8400-e29b-41d4-a716-446655440003",
        "documentName": "chapter1.txt",
        "orderIndex": 0,
        "tokenCount": 150,
        "sentenceCount": 12,
        "tokenized": true
      }
    ],
    "totalDocuments": 3,
    "totalSentences": 45,
    "totalTokens": 520,
    "tokenizedDocuments": 3,
    "lastAccessedAt": "2024-12-27T10:00:00Z"
  }
}
```

---

### 2. Get Workspace Documents
**GET** `/workspaces/{workspaceId}/documents`

Get list of documents in workspace with token counts.

#### Response `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "documentId": "880e8400-e29b-41d4-a716-446655440003",
      "documentName": "chapter1.txt",
      "orderIndex": 0,
      "tokenCount": 150,
      "sentenceCount": 12,
      "tokenized": true
    },
    {
      "documentId": "990e8400-e29b-41d4-a716-446655440004",
      "documentName": "chapter2.txt",
      "orderIndex": 1,
      "tokenCount": 180,
      "sentenceCount": 15,
      "tokenized": true
    }
  ]
}
```

---

### 3. Get Document Content
**GET** `/documents/{documentId}/content`

Get document content with tokens for display in the editor.

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "documentId": "880e8400-e29b-41d4-a716-446655440003",
    "documentName": "chapter1.txt",
    "orderIndex": 0,
    "sentences": [
      {
        "index": 0,
        "text": "John Smith went to the store."
      }
    ],
    "tokens": [
      {
        "index": 0,
        "sentenceIndex": 0,
        "text": "John",
        "whitespaceAfter": " "
      },
      {
        "index": 1,
        "sentenceIndex": 0,
        "text": "Smith",
        "whitespaceAfter": " "
      }
    ],
    "totalSentences": 12,
    "totalTokens": 150,
    "globalTokenOffset": 0
  }
}
```

---

### 4. Get Document Content with Offset
**GET** `/workspaces/{workspaceId}/documents/{documentId}/content`

Get document content with workspace-level token offset for global token indexing.

#### Response `200 OK`
Same as Get Document Content, but `globalTokenOffset` reflects the document's position in the workspace token sequence.

---

### 5. Tokenize Document
**POST** `/documents/{documentId}/tokenize`

Tokenize a document (import plain text into sentences and tokens).

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "documentId": "880e8400-e29b-41d4-a716-446655440003",
    "sentenceCount": 12,
    "tokenCount": 150,
    "success": true
  }
}
```

---

### 6. Get Tokenization Status
**GET** `/workspaces/{workspaceId}/status`

Get tokenization status for all documents in a workspace.

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
    "totalDocuments": 3,
    "tokenizedDocuments": 2,
    "totalTokens": 330,
    "totalSentences": 27,
    "ready": false
  }
}
```

| Field | Description |
|-------|-------------|
| ready | `true` when all documents are tokenized |

---

## Session Management Endpoints

### 7. Get Session State
**GET** `/workspaces/{workspaceId}/session`

Get the current editor session state for the authenticated user.

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "cc0e8400-e29b-41d4-a716-446655440030",
    "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "660e8400-e29b-41d4-a716-446655440001",
    "lastDocumentIndex": 2,
    "scrollPosition": 450,
    "lastAccessedAt": "2024-12-27T10:30:00Z"
  }
}
```

---

### 8. Save Session State
**POST** `/session`

Save the current editor session state.

#### Request
```json
{
  "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
  "lastDocumentIndex": 2,
  "scrollPosition": 450
}
```

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "cc0e8400-e29b-41d4-a716-446655440030",
    "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "660e8400-e29b-41d4-a716-446655440001",
    "lastDocumentIndex": 2,
    "scrollPosition": 450,
    "lastAccessedAt": "2024-12-27T10:35:00Z"
  }
}
```

---

### 9. Close Session
**DELETE** `/workspaces/{workspaceId}/session`

Close/clear the current editor session.

#### Response `204 No Content`
No response body.

---

## Session Fields

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Session identifier |
| workspaceId | UUID | Workspace being edited |
| userId | UUID | User who owns the session |
| lastDocumentIndex | Integer | Index of last viewed document |
| scrollPosition | Integer | Last scroll position in editor |
| lastAccessedAt | Timestamp | When session was last active |
