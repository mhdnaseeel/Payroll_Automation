# Deploying HR Automation System for Free (MVP)

This guide outlines the steps to deploy your HR Automation MVP using **Render** (Backend & Database) and **Vercel** (Frontend). These platforms offer generous free tiers suitable for MVPs.

## Prerequisites

1.  **GitHub Account**: You need to host your code on GitHub.
2.  **Render Account**: Sign up at [render.com](https://render.com).
3.  **Vercel Account**: Sign up at [vercel.com](https://vercel.com).
4.  **Local Environment**: Ensure your project is committed to a GitHub repository.

---

## Part 1: Database & Backend (Render)

We will deploy the Spring Boot backend and a PostgreSQL database on Render.

### 1. Create PostgreSQL Database
1.  Log in to **Render**.
2.  Click **New +** -> **PostgreSQL**.
3.  **Name**: `fci-hr-db` (or similar).
4.  **Region**: Choose one close to you (e.g., Singapore or Frankfurt).
5.  **Instance Type**: Select **Free**.
6.  Click **Create Database**.
7.  **IMPORTANT**: Once created, copy the **Internal Database URL** (for backend) and **External Database URL** (for your local access/debugging).

### 2. Deploy Spring Boot Backend
1.  Click **New +** -> **Web Service**.
2.  Connect your GitHub repository.
3.  **Runtime**: Select **Docker** (The `Dockerfile` I created in `backend/` will be used).
4.  **Name**: `fci-hr-backend`.
5.  **Region**: Same as database.
6.  **Instance Type**: **Free**.
7.  **Environment Variables**: Add the following:
    *   `SPRING_DATASOURCE_URL`: `jdbc:postgresql://<hostname>:5432/<database_name>` (Use the **Internal URL** from step 1. Note: Render gives `postgres://...`, you must change it to `jdbc:postgresql://...` for Spring Boot).
    *   `SPRING_DATASOURCE_USERNAME`: (Copy from DB details)
    *   `SPRING_DATASOURCE_PASSWORD`: (Copy from DB details)
    *   `AZURE_FORM_RECOGNIZER_ENDPOINT`: (Your Azure Endpoint)
    *   `AZURE_FORM_RECOGNIZER_KEY`: (Your Azure Key)
    *   `GOOGLE_AI_STUDIO_API_KEY`: (Your Google Key)
    *   `PORT`: `8080`
8.  Click **Create Web Service**.
9.  Wait for the build to finish. Once deployed, copy the **Service URL** (e.g., `https://fci-hr-backend.onrender.com`).

**Note on Free Tier**: The Web Service will spin down after 15 minutes of inactivity. The first request after inactivity will take ~50 seconds to load.

---

## Part 2: Frontend (Vercel)

We will deploy the Angular frontend on Vercel.

### 1. Update Production Configuration
1.  Based on the changes I made, your frontend is now ready for production.
2.  **Verify**: Open `frontend/src/environments/environment.prod.ts` and replace the placeholder URL with your **Render Backend URL** from Part 1.
    ```typescript
    export const environment = {
      production: true,
      apiUrl: 'https://fci-hr-backend.onrender.com/api' // <--- UPDATE THIS
    };
    ```
3.  Commit and push this change to GitHub.

### 2. Deploy to Vercel
1.  Log in to **Vercel**.
2.  Click **Add New...** -> **Project**.
3.  Import your GitHub repository.
4.  **Framework Preset**: It should auto-detect **Angular**.
5.  **Root Directory**: Click "Edit" and select `frontend`.
6.  **Build Command**: `ng build` (default).
7.  **Output Directory**: `dist/fci-automation-frontend` (Vercel usually detects this, but verify against your `angular.json` outputPath).
8.  Click **Deploy**.

---

## Part 3: Verification
1.  Open your Vercel App URL.
2.  Try logging in.
3.  **Note**: Since the database is fresh, you will need to register a user or manually insert an Admin user if your app requires one specific admin to start.
4.  If you encounter CORS issues, you might need to update your Backend CORS configuration to allow the Vercel domain.

### CORS Configuration (If needed)
If the frontend cannot talk to the backend, you may need to add a `WebConfig.java` in your backend to allow the Vercel domain:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("https://your-vercel-app.vercel.app") // Add your Vercel URL
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
```

## Summary
- **Backend**: Render (Docker)
- **Database**: Render (PostgreSQL)
- **Frontend**: Vercel (Angular)

---

## Part 4: File Storage (Critical)

Your application allows users to upload documents (EPF/ESI files). Handling these files depends on where you deploy.

### Option A: VPS / Dedicated Server (Recommended for Simplicity)
If you deploy using `docker-compose` on a Virtual Private Server (e.g., EC2, DigitalOcean Droplet, Hetzner):
- **Storage Location**: Files are stored in the Docker Volume (`uploads_data`) on the server's disk.
- **Persistence**: Data persists as long as the server disk exists. You can back up the specific folder `/var/lib/docker/volumes/...` or the mapped path.
- **Setup**: The current `docker-compose.yml` is already set up for this. Just run `docker compose up -d` on the server.

### Option B: Cloud Platforms (Render, Heroku, etc.)
**WARNING**: Most free or simple cloud container platforms (like Render Free Tier) have **Ephemeral Filesystems**. This means **all uploaded files are deleted** every time the app updates or restarts.

To fix this on Render/Heroku:
1.  **Paid Disk**: You must attach a "Persistent Disk" (Render costs ~$0.25/GB/mo) and mount it to `/app/uploads`.
2.  **Cloud Storage (Best Practice)**: Modify the application code to upload files to AWS S3, Google Cloud Storage, or Azure Blob Storage instead of the local disk.

**For this MVP**: If using Render Free Tier, be aware that uploaded files will not survive restarts. For production use without code changes, a VPS (Option A) is safer.
