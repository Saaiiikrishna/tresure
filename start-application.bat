@echo off
echo ========================================
echo  Treasure Hunt Adventures - Application Startup
echo ========================================
echo.

echo Loading environment variables from .env file...
for /f "usebackq tokens=1,2 delims==" %%a in (".env") do (
    if not "%%a"=="" if not "%%a:~0,1%"=="#" (
        set "%%a=%%b"
    )
)

echo Checking prerequisites...
echo.

echo 1. Checking Java version...
java -version
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH!
    pause
    exit /b 1
)

echo.
echo 2. Checking Maven...
mvn -version
if %errorlevel% neq 0 (
    echo ERROR: Maven is not installed or not in PATH!
    pause
    exit /b 1
)

echo.
echo 3. Checking database connection...
docker exec treasure-hunt-postgres pg_isready -U treasure_user -d treasure_hunt_db >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Database is not running!
    echo Please run start-database.bat first.
    pause
    exit /b 1
)

echo ✅ All prerequisites met!
echo.

echo Starting Treasure Hunt Adventures application...
echo.
echo Application will be available at:
echo   🌐 Main Website: http://localhost:8080
echo   🔐 Admin Panel:  http://localhost:8080/admin
echo.
echo Admin Credentials:
echo   Username: %ADMIN_USERNAME%
echo   Password: %ADMIN_PASSWORD%
echo.
echo Gmail Configuration:
echo   Email: %GMAIL_USERNAME%
echo   Status: Configured ✅
echo.

echo Starting Spring Boot application...
mvn spring-boot:run

pause
