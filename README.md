BTC Movement Explainer 📈

An end-to-end system that detects significant Bitcoin price movements, correlates them with external signals (news, sentiment, FX, weather), and generates human-readable explanations.

This repository follows a monorepo structure containing both the frontend and backend, deployed independently using free hosting tiers.

🏗️ Architecture Overview
btc-movement-explainer/
├── frontend/        # Vite + React frontend (Vercel)
├── backend/         # Spring Boot backend (Render)
├── README.md

Components

Frontend: React (Vite), deployed on Vercel

Backend: Spring Boot REST API, deployed on Render

Database: Neon PostgreSQL (serverless)

AI: OpenAI API (optional, fallback supported)

Cron: Render Cron Job (production)

🚀 Deployment Strategy
Component	Platform	Folder
Frontend	Vercel	/frontend
Backend	Render	/backend
Database	Neon	External

Each platform deploys only its relevant subdirectory.

🔐 Configuration & Secrets Management
❌ Never committed

.env

application.yml

API keys, tokens, credentials

✅ Committed safely

application-prod.yml (no secrets)

.env.example (documentation only)

Secrets are injected using environment variables on the hosting platforms.

⚙️ Spring Boot Profiles

The backend uses Spring Profiles for environment separation.

Production Mode

Activated by setting:

SPRING_PROFILES_ACTIVE=prod


When enabled:

application-prod.yml is loaded

Secrets are read from environment variables

Internal schedulers are disabled

Logging is production-safe

⏱️ Cron Job Strategy
Development

Spring @Scheduled jobs can run locally for testing

Production (Recommended)

Render Cron Job triggers:

POST /api/admin/refresh


Internal scheduler is disabled using:

app:
  jobs:
    enabled: false


This avoids duplicate executions and prevents issues caused by free-tier service sleep.

🔐 Admin Refresh Endpoint

The backend exposes a protected admin endpoint:

POST /api/admin/refresh


Security:

Requires X-Admin-Token header

Token stored as environment variable

Intended for cron / admin use only

Never called from the frontend

🌐 Frontend Configuration

Frontend uses Vite environment variables.

Example:

VITE_API_BASE_URL=https://your-backend.onrender.com


Only variables prefixed with VITE_ are exposed to the browser.

📁 Git Ignore Strategy

Each subproject has its own .gitignore.

Backend ignores

.env

application.yml

Local/dev config files

Build artifacts

Frontend ignores

node_modules

dist/

.env

This ensures:

No secrets are committed

Clean, portable repository

🧪 Local Development
Backend
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

Frontend
cd frontend
npm install
npm run dev

🧠 Key Design Principles

Monorepo for clarity and coordination

Zero secrets in Git

Environment-driven configuration

Externalized cron execution

Free-tier friendly deployment

Deterministic fallback when AI is unavailable

📌 Notes

This project is designed for MVP / portfolio / research use

Scaling strategies (queues, locks, workers) can be added later

Architecture intentionally favors clarity over over-engineering

📄 License

MIT License (or specify if different)