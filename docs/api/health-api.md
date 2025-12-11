# Health Check API Documentation

## Overview
The Health Check API provides a centralized endpoint to verify the operational status of the Genesis application and its constituent modules. It aggregates health reports from all active modules (User, Workspace, CoreF, Import-Export, Infra) to provide a comprehensive system status.

## Endpoints

### Get System Health

**URL**: `/api/health`
**Method**: `GET`
**Auth Required**: No

#### Success Response

**Code**: `200 OK`
**Content-Type**: `application/json`

**Example Content**:
```json
{
  "status": "UP",
  "checks": {
    "User Module": true,
    "Workspace Module": true,
    "CoreF Module": true,
    "Import-Export Module": true,
    "Infra Module": true
  }
}
```

#### Fields
- `status` (string): Overall system status (e.g., "UP").
- `checks` (object): A map of individual module names to their boolean health status.
  - `key`: The name of the module (e.g., "User Module").
  - `value`: `true` if healthy, `false` otherwise.

## Implementation Details
The system uses a strategy pattern where each module implements the `ModuleHealthCheck` interface. The `HealthController` in `genesis-api` dynamically discovers and aggregates these implementations at runtime.
