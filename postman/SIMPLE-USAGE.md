# Azure Graph API - Simple Postman Collection

## � Files in this folder:
- `Azure-Graph-API-Simple.postman_collection.json` - Import this into Postman
- `SIMPLE-USAGE.md` - This usage guide

## �🚀 Quick Start

### 1. Import Collection
1. Open Postman
2. Click **Import** button
3. Import `Azure-Graph-API-Simple.postman_collection.json`

### 2. Test Flow

#### Step 1: Health Check
- Run **"1. Health Check"** to verify services are running
- Should return status "SUCCESS"

#### Step 2: Login
- Run **"2. Login"** to authenticate
- Copy the `session_code` from the response
- Example response:
```json
{
  "authenticated": true,
  "session_code": "A348FE852F157F714DBE64933DDF82FC",
  "user_id": "maris@aravindmaklabsoutlook.onmicrosoft.com"
}
```

#### Step 3: Update Session Code
- In requests 3-8, replace `YOUR_SESSION_CODE_HERE` with the actual session code
- Example: Change `JSESSIONID=YOUR_SESSION_CODE_HERE` to `JSESSIONID=A348FE852F157F714DBE64933DDF82FC`

#### Step 4: Test Endpoints
- **"3. Get User Profile"** - Current user info
- **"4. Get Users (Page 0, Size 5)"** - First 5 users
- **"5. Get Users (Page 1, Size 3)"** - Next 3 users
- **"6. Get All Roles"** - Available roles
- **"7. Get All Groups"** - Security groups
- **"8. Logout"** - End session

## 📋 Request List

| # | Name | Method | Endpoint | Auth |
|---|------|--------|----------|------|
| 1 | Health Check | GET | `/health-check` | No |
| 2 | Login | POST | `/web/auth/login` | No |
| 3 | Get User Profile | GET | `/web/user/profile` | Yes |
| 4 | Get Users (Page 0, Size 5) | GET | `/web/users?page=0&size=5` | Yes |
| 5 | Get Users (Page 1, Size 3) | GET | `/web/users?page=1&size=3` | Yes |
| 6 | Get All Roles | GET | `/web/roles` | Yes |
| 7 | Get All Groups | GET | `/web/groups` | Yes |
| 8 | Logout | POST | `/web/auth/logout` | Yes |

## 🔧 Manual Session Management

Since this is a simple collection without environment variables:

1. **Login** and copy the `session_code`
2. **Manually update** each request's Cookie header
3. **Replace** `YOUR_SESSION_CODE_HERE` with the actual session code

## ✅ Expected Results

- **Health Check**: `"status":"SUCCESS"`
- **Login**: `"authenticated":true` with session code
- **User Profile**: Current user details
- **Users**: Paginated user list
- **Roles**: Array of role names
- **Groups**: Array of group objects
- **Logout**: `"success":true`

## 🚨 Troubleshooting

**Services not running?**
```bash
./scripts/deploy-all.sh
```

**Authentication failed?**
- Check username/password in login request body (password is "changeme")
- Verify services are communicating (health check)

**Session expired?**
- Run login again to get new session code
- Update all Cookie headers with new session code

Happy Testing! 🎉
