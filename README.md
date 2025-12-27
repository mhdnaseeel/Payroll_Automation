# FCI Payroll Automation Web App

This is a full-stack application replacing the legacy Excel tool.

## Prerequisites
To run this project, you need the following installed on your machine:
1.  **Java 17** (or higher)
2.  **Node.js & NPM**
3.  **PostgreSQL** (Optional for prototype, uses in-memory/setup dependent)

## How to Run

### 1. Backend (Spring Boot)
The backend handles calculations, database, and report generation.

1.  Open your terminal.
2.  Navigate to the `backend` folder:
    ```bash
    cd backend
    ```
3.  Run the application:
    *   **Mac/Linux**:
        ```bash
        ./gradlew bootRun
        ```
        *(If `./gradlew` is missing, run `gradle wrapper` first if you have gradle installed, or open the project in IntelliJ IDEA).*
    *   **Windows**:
        ```cmd
        gradlew.bat bootRun
        ```

The Server will start at: `http://localhost:8080`.

### 2. Frontend (Angular)
The frontend provides the User Interface.

1.  Open a new terminal.
2.  Navigate to the `frontend` folder:
    ```bash
    cd frontend
    ```
3.  Install dependencies:
    ```bash
    npm install
    ```
4.  Start the app:
    ```bash
    npm start
    ```

The App will open at: `http://localhost:4200`.



## Features
*   **Admin Dashboard**: Manage Employees.
*   **Payroll Entry**: Monthly data input (Days, Wages).
*   **Reports**: Download PDF/Excel/CSV reports.
