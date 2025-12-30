# HR Automation System

A full-stack web application for automating HR and payroll processes with multi-tenant support.

## Tech Stack
- **Backend**: Java 17 + Spring Boot + PostgreSQL
- **Frontend**: Angular 17
- **Deployment**: Docker + Docker Compose

## Prerequisites
To run this project locally, you need:
1. **Docker** and **Docker Compose**
2. **Azure Document Intelligence** credentials (for document extraction)
3. **Google AI Studio** API key (for AI-powered features)

## Local Development Setup

### 1. Configure Environment Variables
Create a `.env` file in the project root (use `.env.example` as template):

```bash
# Database Configuration
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
POSTGRES_DB=hr_db

# Azure Document Intelligence
AZURE_FORM_RECOGNIZER_ENDPOINT=your_azure_endpoint
AZURE_FORM_RECOGNIZER_KEY=your_azure_key

# Google AI Studio
GOOGLE_AI_STUDIO_API_KEY=your_google_api_key
```

### 2. Start the Application
```bash
docker compose up -d
```

### 3. Access the Application
- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8080
- **PostgreSQL**: localhost:5432

### 4. Stop the Application
```bash
docker compose down
```

### 5. Clean All Data (Fresh Start)
```bash
docker compose down -v
```

## Deployment

For production deployment instructions, see the `deployment_guide.md` file (local only, not in Git).

## Features
- Multi-tenant payroll management
- Employee data management
- EPF/ESI report generation
- Document extraction using AI
- Billing module for FCI operations
