# Coreference API Documentation

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

## Mention Endpoints

### 1. Create Mention
**POST** `/workspaces/{workspaceId}/mentions`

Create a new mention annotation.

#### Request
```json
{
  "documentId": "880e8400-e29b-41d4-a716-446655440003",
  "sentenceIndex": 0,
  "startTokenIndex": 5,
  "endTokenIndex": 6,
  "text": "John Smith",
  "mentionType": "PROPER",
  "clusterId": null
}
```

#### Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| documentId | UUID | Yes | Document containing the mention |
| sentenceIndex | Integer | Yes | Sentence index (0-based) |
| startTokenIndex | Integer | Yes | Start token index within sentence |
| endTokenIndex | Integer | Yes | End token index within sentence (inclusive) |
| text | String | Yes | The mention text |
| mentionType | String | No | Mention type (PROPER, NOMINAL, PRONOUN) |
| clusterId | UUID | No | Assign to cluster immediately |

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "aa0e8400-e29b-41d4-a716-446655440010",
    "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
    "documentId": "880e8400-e29b-41d4-a716-446655440003",
    "clusterId": null,
    "clusterNumber": null,
    "sentenceIndex": 0,
    "startTokenIndex": 5,
    "endTokenIndex": 6,
    "globalStartIndex": 5,
    "globalEndIndex": 6,
    "text": "John Smith",
    "mentionType": "PROPER",
    "clusterColor": null
  }
}
```

---

### 2. Get Mentions by Workspace
**GET** `/workspaces/{workspaceId}/mentions`

Get all mentions in a workspace.

#### Response `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": "aa0e8400-e29b-41d4-a716-446655440010",
      "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
      "documentId": "880e8400-e29b-41d4-a716-446655440003",
      "clusterId": "bb0e8400-e29b-41d4-a716-446655440020",
      "clusterNumber": 1,
      "sentenceIndex": 0,
      "startTokenIndex": 5,
      "endTokenIndex": 6,
      "globalStartIndex": 5,
      "globalEndIndex": 6,
      "text": "John Smith",
      "mentionType": "PROPER",
      "clusterColor": "#FF5733"
    }
  ]
}
```

---

### 3. Get Mentions by Document
**GET** `/documents/{documentId}/mentions`

Get all mentions in a specific document.

#### Response `200 OK`
Same structure as Get Mentions by Workspace.

---

### 4. Get Unassigned Mentions
**GET** `/workspaces/{workspaceId}/mentions/unassigned`

Get mentions not yet assigned to any cluster.

#### Response `200 OK`
Same structure as Get Mentions by Workspace, but only returns mentions where `clusterId` is null.

---

### 5. Get Mention by ID
**GET** `/mentions/{mentionId}`

Get a single mention by ID.

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "aa0e8400-e29b-41d4-a716-446655440010",
    ...
  }
}
```

#### Errors
| Status | Error | Description |
|--------|-------|-------------|
| 404 | RESOURCE_NOT_FOUND | Mention not found |

---

### 6. Assign Mention to Cluster
**PUT** `/mentions/{mentionId}/cluster/{clusterId}`

Assign a mention to a coreference cluster.

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "aa0e8400-e29b-41d4-a716-446655440010",
    "clusterId": "bb0e8400-e29b-41d4-a716-446655440020",
    "clusterNumber": 1,
    "clusterColor": "#FF5733",
    ...
  }
}
```

---

### 7. Unassign Mention from Cluster
**DELETE** `/mentions/{mentionId}/cluster`

Remove a mention from its current cluster.

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "aa0e8400-e29b-41d4-a716-446655440010",
    "clusterId": null,
    "clusterNumber": null,
    "clusterColor": null,
    ...
  }
}
```

---

### 8. Delete Mention
**DELETE** `/mentions/{mentionId}`

Delete a mention annotation.

#### Response `204 No Content`
No response body.

---

## Cluster Endpoints

### 9. Create Cluster
**POST** `/workspaces/{workspaceId}/clusters`

Create a new coreference cluster.

#### Request (Optional)
```json
{
  "label": "Person: John",
  "color": "#FF5733"
}
```

If no body is provided, a cluster with auto-generated number and color is created.

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "bb0e8400-e29b-41d4-a716-446655440020",
    "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
    "clusterNumber": 1,
    "label": "Person: John",
    "representativeText": null,
    "color": "#FF5733",
    "mentionCount": 0
  }
}
```

---

### 10. Get Clusters by Workspace
**GET** `/workspaces/{workspaceId}/clusters`

Get all clusters in a workspace.

#### Response `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": "bb0e8400-e29b-41d4-a716-446655440020",
      "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
      "clusterNumber": 1,
      "label": "Person: John",
      "representativeText": "John Smith",
      "color": "#FF5733",
      "mentionCount": 3
    }
  ]
}
```

---

### 11. Get Cluster by ID
**GET** `/clusters/{clusterId}`

Get a single cluster by ID.

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "bb0e8400-e29b-41d4-a716-446655440020",
    ...
  }
}
```

#### Errors
| Status | Error | Description |
|--------|-------|-------------|
| 404 | RESOURCE_NOT_FOUND | Cluster not found |

---

### 12. Get Mentions in Cluster
**GET** `/clusters/{clusterId}/mentions`

Get all mentions assigned to a specific cluster.

#### Response `200 OK`
Same structure as Get Mentions by Workspace.

---

### 13. Update Cluster
**PUT** `/clusters/{clusterId}`

Update cluster label and/or color.

#### Request
```json
{
  "label": "Person: John Smith",
  "color": "#33FF57"
}
```

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "bb0e8400-e29b-41d4-a716-446655440020",
    "label": "Person: John Smith",
    "color": "#33FF57",
    ...
  }
}
```

---

### 14. Delete Cluster
**DELETE** `/clusters/{clusterId}`

Delete a cluster. All mentions are unassigned (not deleted).

#### Response `204 No Content`
No response body.

---

## Statistics Endpoint

### 15. Get Annotation Statistics
**GET** `/workspaces/{workspaceId}/coref/stats`

Get coreference annotation statistics for a workspace.

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "mentionCount": 25,
    "clusterCount": 8,
    "unassignedCount": 3
  }
}
```

| Field | Description |
|-------|-------------|
| mentionCount | Total number of mentions in workspace |
| clusterCount | Total number of clusters in workspace |
| unassignedCount | Mentions not assigned to any cluster |

---

## Enums

### MentionType
| Value | Description |
|-------|-------------|
| `PROPER` | Proper noun mention (e.g., "John Smith") |
| `NOMINAL` | Nominal mention (e.g., "the president") |
| `PRONOUN` | Pronominal mention (e.g., "he", "she") |
