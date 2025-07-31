@echo off
echo ========================================
echo  Treasure Hunt Adventures - Database Setup
echo ========================================
echo.

echo Checking if Docker Desktop is running...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker is not installed or not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

echo Docker is available!
echo.

echo Stopping any existing containers...
docker-compose down

echo.
echo Starting PostgreSQL database...
docker-compose up -d postgres

echo.
echo Waiting for database to be ready...
timeout /t 10 /nobreak >nul

echo.
echo Checking database health...
docker-compose ps

echo.
echo Testing database connection...
docker exec treasure-hunt-postgres pg_isready -U treasure_user -d treasure_hunt_db

if %errorlevel% equ 0 (
    echo.
    echo ✅ SUCCESS: Database is ready!
    echo.
    echo Database Details:
    echo   Host: localhost
    echo   Port: 5432
    echo   Database: treasure_hunt_db
    echo   Username: treasure_user
    echo   Password: treasure_pass_2024
    echo.
    echo You can now start the Spring Boot application!
) else (
    echo.
    echo ❌ ERROR: Database is not responding
    echo Please check Docker logs: docker-compose logs postgres
)

echo.
pause
