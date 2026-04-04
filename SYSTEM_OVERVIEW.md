# HR & Payroll Automation System Overview

A comprehensive guide to the technical stack and end-to-end functionality of the FCI Payroll and HR Automation system.

---

## 🛠 Technical Stack

### Frontend (User Interface)
- **Framework**: [Angular 17](https://angular.dev/) (Standalone Components Architecture)
- **Styling**: [Bootstrap 5.3](https://getbootstrap.com/) & [Bootstrap Icons](https://icons.getbootstrap.com/)
- **State Management**: [RxJS Observables](https://rxjs.dev/) for reactive data flows.
- **Form Handling**: Reactive and Template-driven forms for complex data entry.
- **PDF/Excel Preview**: Custom viewers for generated documents.

### Backend (Server Logic)
- **Framework**: [Spring Boot 3.3.0](https://spring.io/projects/spring-boot)
- **Language**: Java 17
- **Security**: [Spring Security](https://spring.io/projects/spring-security) with JWT-based (JSON Web Token) authentication.
- **Data Persistence**: [Spring Data JPA](https://spring.io/projects/spring-data-jpa) with Hibernate.
- **Integrations**:
    - **Mistral AI**: Used for intelligent data extraction and content analysis.
    - **Tesseract OCR (Tess4J)**: For optical character recognition from scanned documents.
    - **Apache POI**: Processing and generating Excel files.
    - **OpenPDF (LibrePDF)**: Dynamic PDF report generation.
    - **Jackson**: JSON serialization/deserialization.

### Infrastructure & Database
- **Database**: [PostgreSQL 16](https://www.postgresql.org/) (Primary persistent storage).
- **Containerization**: [Docker](https://www.docker.com/) & [Docker Compose](https://docs.docker.com/compose/) for environment orchestration.
- **WebServer**: Nginx (serving the compiled Angular frontend).

---

## 🔄 End-to-End System Workflow

### 1. Authentication & Role-Based Access (RBAC)
The system employs strict role-based access to ensure data security:
- **ADMIN**: Manages the Employee Master list, system configurations, and global settings.
- **USER (Payroll Admin)**: Handles monthly attendance, payroll entries, and generates final reports.
- **SALARY**: Exclusive access to the Salary Calculator for manual/ad-hoc calculations.
- **BILL**: Operates the Billing Modules (Receipts, Issues, QC, and Bill Generation).

Upon login, the backend issues a **JWT** which specifies the user's role. The frontend uses an `authGuard` and route metadata to dynamically hide/show navigation links and protect restricted pages.

### 2. Employee Management
Admins maintain the "Source of Truth" for all staff:
- **Excel Import**: Supports bulk uploading employees from Excel files.
- **Duplicate Prevention**: An in-memory mapping logic prevents duplicate `member_id` entries during bulk uploads, even if they appear multiple times in the source file.

### 3. Payroll Processing Engine
This is the core of the application:
1. **Period Initialization**: A payroll admin starts a "Period" (Month/Year).
2. **Attendance Management**: 
   - **Casual Attendance**: Daily attendance tracking for casual workers.
   - **Monthly Entry**: Bulk entry or import of working days and earnings.
3. **Automated Calculation**: The system automatically calculates:
   - **EPF (Member & Contractor Shares)**
   - **ESI (Member & Contractor Shares)**
   - **Bonus Distributions**
   - **Net Payable Amounts**
4. **Finalization & Lock**: Once verified, the period is "Finalized," locking all data from further edits to ensure audit integrity.

### 4. Billing & Operations
A specialized module for industrial operations tracking:
- **Receipt/Issue Tracking**: Logging incoming and outgoing materials (bags/items).
- **QC Module**: For quality control checks and status updates.
- **Bill Generator**: Converts operation logs into finalized billing documents.

### 5. AI & Document Extraction
Advanced features for digitizing physical workflows:
- **OCR Pipeline**: Physical work slips are scanned; Tesseract OCR and Mistral AI work together to extract text and structure it into system entities (e.g., `WorkSlip`).
- **Tesseract Training**: The system includes a dedicated training folder for improving OCR accuracy on specific forms.

### 6. Reporting & Exports
Dynamic generation of industrial-standard reports:
- **Main Payroll File**: Comprehensive overview of all earnings and deductions.
- **Bank Payment Details**: Exportable format for bank transfers.
- **Wage Summary**: Aggregated data for accounting.
- **Attendance Register**: Detailed daily logs for verification.

---

## 📁 Project Directory Structure
- `/frontend`: Angular source code, assets, and Nginx configuration.
- `/backend`: Spring Boot source code, Maven configuration, and database scripts.
- `/docker-compose.yml`: Defines the `hr_db`, `hr_backend`, and `hr_frontend` containers.
- `/data`: Shared data used for development/testing.
- `/tesseract_training`: Resources for fine-tuning OCR models.
