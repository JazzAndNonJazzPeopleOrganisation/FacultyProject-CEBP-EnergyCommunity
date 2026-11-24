# Core Energy Service — REST API Endpoints Documentation

**Base URL:** `http://localhost:8080`  
**Swagger UI (Interactive Docs):** `http://localhost:8080/swagger-ui.html`  
**OpenAPI JSON Spec:** `http://localhost:8080/v3/api-docs`

---

## Summary Table

### 1. User API (`/api/v1/users`)
| Method | Endpoint | Description | Success Code |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users` | Get all registered users | `200 OK` |
| `GET` | `/api/v1/users/{id}` | Get a user and their houses by user ID | `200 OK` |
| `POST` | `/api/v1/users` | Create a new user account | `201 Created` |
| `PUT` | `/api/v1/users/{id}` | Update user details (supports partial updates) | `200 OK` |
| `DELETE` | `/api/v1/users/{id}` | Delete a user and their associated houses | `204 No Content` |

### 2. Admin API (`/api/v1/admin`)
| Method | Endpoint | Description | Success Code |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/admin` | Get all administrators | `200 OK` |
| `GET` | `/api/v1/admin/{id}` | Get administrator by ID | `200 OK` |
| `POST` | `/api/v1/admin` | Create a new administrator account | `201 Created` |
| `POST` | `/api/v1/admin/login` | Authenticate administrator (`usernameOrEmail`, `password`) | `200 OK` |
| `DELETE` | `/api/v1/admin/{id}` | Delete an administrator account | `204 No Content` |
| `GET` | `/api/v1/admin/users` | **Admin action:** View all user accounts | `200 OK` |
| `GET` | `/api/v1/admin/users/{userId}` | **Admin action:** View a specific user account by ID | `200 OK` |
| `DELETE` | `/api/v1/admin/users/{userId}` | **Admin action:** Delete a user account and associated data | `204 No Content` |

### 3. House & Hardware API (`/api/v1/houses`)
| Method | Endpoint | Description | Success Code |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/houses` | Get all houses with batteries and solar panels | `200 OK` |
| `GET` | `/api/v1/houses/{id}` | Get a specific house by ID | `200 OK` |
| `GET` | `/api/v1/houses/user/{userId}` | Get all houses owned by a specific user | `200 OK` |
| `POST` | `/api/v1/houses` | Create a new house | `201 Created` |
| `POST` | `/api/v1/houses/user/{userId}` | Create a new house assigned to a specific user | `201 Created` |
| `PUT` | `/api/v1/houses/{id}` | Update house details (supports partial updates) | `200 OK` |
| `DELETE` | `/api/v1/houses/{id}` | Delete a house and its hardware | `204 No Content` |
| `GET` | `/api/v1/houses/{houseId}/batteries` | Get all batteries installed in a house | `200 OK` |
| `POST` | `/api/v1/houses/{houseId}/batteries` | Add a battery unit to a house | `201 Created` |
| `PUT` | `/api/v1/houses/batteries/{batteryId}` | Update battery parameters (supports partial updates) | `200 OK` |
| `DELETE` | `/api/v1/houses/batteries/{batteryId}` | Delete a battery unit | `204 No Content` |
| `GET` | `/api/v1/houses/{houseId}/solar-panels` | Get all solar panels installed in a house | `200 OK` |
| `POST` | `/api/v1/houses/{houseId}/solar-panels` | Add a solar panel array to a house | `201 Created` |
| `PUT` | `/api/v1/houses/solar-panels/{panelId}` | Update solar panel parameters (supports partial updates) | `200 OK` |
| `DELETE` | `/api/v1/houses/solar-panels/{panelId}` | Delete a solar panel unit | `204 No Content` |

---

## 1. User API (`/api/v1/users`)

### `GET /api/v1/users`
Returns a list of all registered users and their houses.
- **Responses:** `200 OK`

### `GET /api/v1/users/{id}`
Returns a single user by ID.
- **Path Parameters:** `id` (`Long`, required)
- **Responses:** `200 OK`, `404 Not Found`

### `POST /api/v1/users`
Creates a new user.
- **Request Body:**
```json
{
  "name": "Ion Popescu",
  "email": "ion.popescu@energy.ro",
  "phone": "+40722123456"
}
```
- **Responses:** `201 Created`, `400 Bad Request` (invalid data or duplicate email)

### `PUT /api/v1/users/{id}`
Updates an existing user (supports partial updates — only non-null fields are modified).
- **Path Parameters:** `id` (`Long`, required)
- **Request Body:**
```json
{
  "name": "Ion Popescu Updated",
  "phone": "+40744000111"
}
```
- **Responses:** `200 OK`, `400 Bad Request`, `404 Not Found`

### `DELETE /api/v1/users/{id}`
Deletes a user and cascades deletion to their houses, batteries, and solar panels.
- **Path Parameters:** `id` (`Long`, required)
- **Responses:** `204 No Content`, `404 Not Found`

---

## 2. Admin API (`/api/v1/admin`)

> **Note:** Administrators are separate from regular users. They do not own houses or energy hardware; their role is strictly to view and delete user accounts.

### `POST /api/v1/admin`
Creates a new administrator account. (`password` is `WRITE_ONLY` and is never returned in JSON responses).
- **Request Body:**
```json
{
  "username": "admin_energy",
  "email": "admin@energy.ro",
  "password": "AdminPassword123!"
}
```
- **Response (`201 Created`):**
```json
{
  "id": 1,
  "username": "admin_energy",
  "email": "admin@energy.ro"
}
```
- **Responses:** `201 Created`, `400 Bad Request` (duplicate username or email)

### `POST /api/v1/admin/login`
Authenticates an administrator by username or email and password.
- **Query Parameters:**
  - `usernameOrEmail` (`String`, required)
  - `password` (`String`, required)
- **Responses:** `200 OK`, `400 Bad Request`, `401 Unauthorized`

### `GET /api/v1/admin` & `GET /api/v1/admin/{id}` & `DELETE /api/v1/admin/{id}`
Lists all admins, retrieves an admin by ID, or deletes an admin account.

### `GET /api/v1/admin/users`
Retrieves all registered user accounts for administrative inspection.
- **Responses:** `200 OK`

### `GET /api/v1/admin/users/{userId}`
Retrieves a specific user account and its associated houses.
- **Path Parameters:** `userId` (`Long`, required)
- **Responses:** `200 OK`, `404 Not Found`

### `DELETE /api/v1/admin/users/{userId}`
Deletes a user account and all associated houses, batteries, and solar panels.
- **Path Parameters:** `userId` (`Long`, required)
- **Responses:** `204 No Content`, `404 Not Found`

---

## 3. House & Hardware API (`/api/v1/houses`)

### `GET /api/v1/houses`
Retrieves all houses along with their owner, batteries, and solar panels.
- **Responses:** `200 OK`

### `GET /api/v1/houses/{id}`
Retrieves a single house by ID.
- **Path Parameters:** `id` (`Long`, required)
- **Responses:** `200 OK`, `404 Not Found`

### `GET /api/v1/houses/user/{userId}`
Retrieves all houses owned by the specified user.
- **Path Parameters:** `userId` (`Long`, required)
- **Responses:** `200 OK`, `404 Not Found`

### `POST /api/v1/houses/user/{userId}` (or `POST /api/v1/houses`)
Creates a new house (with optional initial batteries and solar panels) and assigns it to `userId`.
- **Path Parameters:** `userId` (`Long`, required for `/user/{userId}`)
- **Request Body:**
```json
{
  "name": "Casa Timisoara Nord",
  "address": "Str. Vasile Parvan nr. 2, Timisoara",
  "latitude": 45.7489,
  "longitude": 21.2087,
  "currentConsumptionKw": 1.8,
  "batteries": [
    {
      "model": "Tesla Powerwall 3",
      "capacityKwh": 13.5,
      "currentChargePercentage": 75.0,
      "maxPowerKw": 5.0,
      "status": "IDLE"
    }
  ],
  "solarPanels": [
    {
      "model": "SunPower Maxeon 6",
      "maxCapacityKw": 6.5,
      "efficiency": 22.8,
      "orientation": "SOUTH"
    }
  ]
}
```
- **Responses:** `201 Created`, `400 Bad Request`, `404 Not Found`

### `PUT /api/v1/houses/{id}`
Updates house details (supports partial updates).
- **Path Parameters:** `id` (`Long`, required)
- **Request Body:**
```json
{
  "name": "Casa Timisoara Sud",
  "currentConsumptionKw": 2.4
}
```
- **Responses:** `200 OK`, `404 Not Found`

### `DELETE /api/v1/houses/{id}`
Deletes a house and all its batteries and solar panels.
- **Path Parameters:** `id` (`Long`, required)
- **Responses:** `204 No Content`, `404 Not Found`

---

## 4. Battery Endpoints (`/api/v1/houses/.../batteries`)

### Allowed `BatteryStatus` Enum Values:
- `IDLE`
- `CHARGING`
- `DISCHARGING`
- `HOLD_FOR_EV`

### `GET /api/v1/houses/{houseId}/batteries`
Returns all batteries installed in the given house.
- **Responses:** `200 OK`, `404 Not Found`

### `POST /api/v1/houses/{houseId}/batteries`
Adds a new battery to a house.
- **Request Body:**
```json
{
  "model": "Tesla Powerwall 3",
  "capacityKwh": 13.5,
  "currentChargePercentage": 80.0,
  "maxPowerKw": 5.0,
  "status": "IDLE"
}
```
- **Responses:** `201 Created`, `400 Bad Request`, `404 Not Found`

### `PUT /api/v1/houses/batteries/{batteryId}`
Updates a battery (supports partial updates, e.g., changing only `status` or `currentChargePercentage`).
- **Request Body:**
```json
{
  "currentChargePercentage": 65.0,
  "status": "HOLD_FOR_EV"
}
```
- **Responses:** `200 OK`, `404 Not Found`

### `DELETE /api/v1/houses/batteries/{batteryId}`
Deletes a battery unit.
- **Responses:** `204 No Content`, `404 Not Found`

---

## 5. Solar Panel Endpoints (`/api/v1/houses/.../solar-panels`)

### Allowed `PanelOrientation` Enum Values:
- `NORTH`, `NORTH_EAST`, `EAST`, `SOUTH_EAST`, `SOUTH`, `SOUTH_WEST`, `WEST`, `NORTH_WEST`

### `GET /api/v1/houses/{houseId}/solar-panels`
Returns all solar panels installed in the given house.
- **Responses:** `200 OK`, `404 Not Found`

### `POST /api/v1/houses/{houseId}/solar-panels`
Adds a new solar panel array to a house.
- **Request Body:**
```json
{
  "model": "SunPower Maxeon 6",
  "maxCapacityKw": 6.5,
  "efficiency": 22.8,
  "orientation": "SOUTH"
}
```
- **Responses:** `201 Created`, `400 Bad Request`, `404 Not Found`

### `PUT /api/v1/houses/solar-panels/{panelId}`
Updates a solar panel unit (supports partial updates).
- **Request Body:**
```json
{
  "maxCapacityKw": 8.0,
  "orientation": "SOUTH_WEST"
}
```
- **Responses:** `200 OK`, `404 Not Found`

### `DELETE /api/v1/houses/solar-panels/{panelId}`
Deletes a solar panel unit.
- **Responses:** `204 No Content`, `404 Not Found`
