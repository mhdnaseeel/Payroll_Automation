# FCI Payroll Automation Web App

This is a full-stack application replacing the legacy Excel tool.

## Prerequisites
To run this project, you need the following installed on your machine:
1.  **Java 17** (or higher)
2.  **Node.js & NPM**
3.  **PostgreSQL** (Optional for prototype, uses in-memory/setup dependent)

## How to Run (Verified Test Environment)

> [!IMPORTANT]
> The project assumes you are running in the **Isolated Test Environment**. This ensures no data pollution in production.

### Quick Start
To start the application with `testadmin` credentials and a clean database:

```bash
docker-compose -f docker-compose.test.yml up --build
```

### Access Points
- **Frontend**: [http://localhost:81](http://localhost:81)
- **Backend API**: [http://localhost:8081](http://localhost:8081)
- **Database**: `hr_test_db` (Port 5433)

### Default Credentials
Use these credentials to login to the Test Environment.

| Role | Username | Password |
| :--- | :--- | :--- |
| **Admin** | `testadmin` | `test` |
| **User** | `testuser` | `test` |
| **Bill** | `testbill` | `test` |

*Note: This environment runs on separate ports and uses a separate database volume.*
