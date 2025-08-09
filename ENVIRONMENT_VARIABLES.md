# 🌍 TREASURE HUNT APPLICATION - ENVIRONMENT VARIABLES REFERENCE

This document lists all required and optional environment variables for the Treasure Hunt Registration System across different deployment environments.

## 📋 TABLE OF CONTENTS
- [🔴 CRITICAL REQUIRED VARIABLES](#-critical-required-variables)
- [🟡 OPTIONAL VARIABLES WITH DEFAULTS](#-optional-variables-with-defaults)
- [🔵 PRODUCTION-SPECIFIC VARIABLES](#-production-specific-variables)
- [🟢 DEVELOPMENT/TEST VARIABLES](#-developmenttest-variables)
- [📊 MONITORING & PERFORMANCE](#-monitoring--performance)
- [🔒 SECURITY CONFIGURATION](#-security-configuration)

---

## 🔴 CRITICAL REQUIRED VARIABLES
*These MUST be set in production - application will fail to start without them*

### Database Configuration (Production)
```bash
DB_HOST=your-database-host.com
DB_PORT=5432
DB_NAME=treasure_hunt_production
DB_USERNAME=your_db_user
DB_PASSWORD=your_secure_db_password
```

### Email Configuration (SMTP)
```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_FROM_ADDRESS=noreply@yourdomain.com
MAIL_SUPPORT_ADDRESS=support@yourdomain.com
```

### Admin Security
```bash
ADMIN_USERNAME=your_admin_username
ADMIN_PASSWORD=your_very_secure_admin_password
```

### JPA Configuration
```bash
JPA_DDL_AUTO=validate  # Options: validate, update, create, create-drop
```

### Server Configuration
```bash
SERVER_PORT=8080
```

### SSL/Security
```bash
REQUIRE_SSL=true
HTTP2_ENABLED=true
```

---

## 🟡 OPTIONAL VARIABLES WITH DEFAULTS
*These have sensible defaults but can be customized*

### Spring Profile
```bash
SPRING_PROFILES_ACTIVE=production  # Options: production, dev, test
```

### Database Pool Settings
```bash
DB_POOL_MAX_SIZE=20
DB_POOL_MIN_IDLE=8
DB_CONNECTION_TIMEOUT=30000
DB_IDLE_TIMEOUT=600000
DB_MAX_LIFETIME=1800000
DB_LEAK_DETECTION=60000
```

### Email Timeouts
```bash
MAIL_CONNECTION_TIMEOUT=10000
MAIL_TIMEOUT=10000
MAIL_WRITE_TIMEOUT=10000
```

### File Upload Configuration
```bash
UPLOAD_MAX_FILE_SIZE=50MB
UPLOAD_MAX_REQUEST_SIZE=60MB
FILE_UPLOAD_DIR=uploads/documents
IMAGE_UPLOAD_DIR=uploads/images
TEMP_UPLOAD_DIR=uploads/temp
```

### File Size Limits
```bash
MAX_PHOTO_SIZE=2097152        # 2MB in bytes
MAX_DOCUMENT_SIZE=5242880     # 5MB in bytes
MAX_IMAGE_SIZE=5242880        # 5MB in bytes
MAX_VIDEO_SIZE=52428800       # 50MB in bytes
```

### Allowed File Types
```bash
ALLOWED_PHOTO_TYPES=image/jpeg,image/jpg,image/png
ALLOWED_DOCUMENT_TYPES=application/pdf,image/jpeg,image/jpg
```

### Email Configuration
```bash
MAIL_FROM_NAME=Treasure Hunt - MySillyDreams
COMPANY_NAME=Treasure Hunt Adventures
EMAIL_RETRY_ATTEMPTS=3
EMAIL_RETRY_DELAY=5000
```

### Session Configuration
```bash
ADMIN_SESSION_TIMEOUT=30
MAX_CONCURRENT_SESSIONS=1
PREVENT_LOGIN_ON_MAX_SESSIONS=true
SESSION_TIMEOUT=30
```

### Business Rules
```bash
MIN_PARTICIPANT_AGE=18
MAX_TEAM_SIZE=100
MIN_TEAM_SIZE=2
REGISTRATION_TIMEOUT_HOURS=24
ALLOW_CANCELLATION=true
CANCELLATION_DEADLINE_HOURS=48
```

### Async Configuration
```bash
ASYNC_CORE_POOL_SIZE=2
ASYNC_MAX_POOL_SIZE=5
ASYNC_QUEUE_CAPACITY=100
```

---

## 🔵 PRODUCTION-SPECIFIC VARIABLES

### Monitoring & Health Checks
```bash
ACTUATOR_HEALTH_DETAILS=when-authorized
PROMETHEUS_ENABLED=true
MONITORING_ENABLED=true
MONITORING_INTERVAL=300
MEMORY_WARNING_THRESHOLD=80
MEMORY_CRITICAL_THRESHOLD=90
THREAD_WARNING_THRESHOLD=100
THREAD_CRITICAL_THRESHOLD=200
```

### Performance Tuning
```bash
CACHE_ENABLED=true
PLANS_CACHE_TTL=300
FEATURED_PLAN_CACHE_TTL=600
APP_SETTINGS_CACHE_TTL=1800
```

### Email Processing
```bash
EMAIL_CORE_POOL_SIZE=2
EMAIL_MAX_POOL_SIZE=4
EMAIL_QUEUE_CAPACITY=100
EMAIL_KEEP_ALIVE=60
```

### Tomcat Configuration
```bash
TOMCAT_MAX_THREADS=200
TOMCAT_MIN_THREADS=10
TOMCAT_MAX_CONNECTIONS=8192
TOMCAT_CONNECTION_TIMEOUT=20000
TOMCAT_ACCEPT_COUNT=100
```

### Logging
```bash
LOG_FILE_PATH=logs/treasure-hunt.log
LOG_MAX_SIZE=10MB
LOG_MAX_HISTORY=30
```

### Cleanup Configuration
```bash
CLEANUP_TEMP_FILE_RETENTION_HOURS=24
CLEANUP_LOG_RETENTION_DAYS=30
```

### Application Metadata
```bash
APP_NAME=Treasure Hunt Application
APP_VERSION=1.0.0
APP_DESCRIPTION=A comprehensive treasure hunt registration and management system
```

---

## 🟢 DEVELOPMENT/TEST VARIABLES

### H2 Console (Development Only)
```bash
H2_CONSOLE_ENABLED=true  # Only for dev/test profiles
```

### Error Handling (Development)
```bash
INCLUDE_ERROR_MESSAGE=always    # never for production
INCLUDE_BINDING_ERRORS=always   # never for production
```

### Management Port (Development)
```bash
MANAGEMENT_PORT=8081  # Separate port for actuator endpoints
```

---

## 📊 MONITORING & PERFORMANCE

### Cache Configuration
```bash
CACHE_ENABLED=true
PLANS_CACHE_TTL=300
FEATURED_PLAN_CACHE_TTL=600
APP_SETTINGS_CACHE_TTL=1800
```

### Performance Monitoring
```bash
MONITORING_ENABLED=true
MONITORING_INTERVAL=300
MEMORY_WARNING_THRESHOLD=80
MEMORY_CRITICAL_THRESHOLD=90
THREAD_WARNING_THRESHOLD=100
THREAD_CRITICAL_THRESHOLD=200
```

---

## 🔒 SECURITY CONFIGURATION

### File Security (Optional)
```bash
VIRUS_SCAN_ENABLED=false
MAX_SCAN_SIZE=10485760
QUARANTINE_ENABLED=false
QUARANTINE_DIR=quarantine
```

### SSL/HTTPS
```bash
REQUIRE_SSL=true
HTTP2_ENABLED=true
```

---

## 🚀 DEPLOYMENT EXAMPLES

### Minimal Production Configuration
```bash
# Database
DB_HOST=your-db-host.com
DB_PORT=5432
DB_NAME=treasure_hunt_prod
DB_USERNAME=treasure_user
DB_PASSWORD=super_secure_password

# Email
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_FROM_ADDRESS=noreply@yourdomain.com
MAIL_SUPPORT_ADDRESS=support@yourdomain.com

# Admin
ADMIN_USERNAME=admin
ADMIN_PASSWORD=very_secure_admin_password

# JPA
JPA_DDL_AUTO=validate

# Security
REQUIRE_SSL=true
HTTP2_ENABLED=true

# Server
SERVER_PORT=8080
```

### Docker Environment File (.env)
```bash
# Copy the minimal configuration above
# Add any optional variables as needed
# Mount this file as environment variables in Docker
```

### Kubernetes ConfigMap/Secret
```yaml
# Create separate ConfigMap for non-sensitive data
# Create Secret for sensitive data (passwords, keys)
# Reference in deployment.yaml
```

---

## ⚠️ SECURITY NOTES

1. **Never commit real passwords to version control**
2. **Use strong, unique passwords for production**
3. **Rotate credentials regularly**
4. **Use secrets management in cloud environments**
5. **Set `JPA_DDL_AUTO=validate` in production**
6. **Always set `REQUIRE_SSL=true` in production**
7. **Disable H2 console in production**
8. **Use `ACTUATOR_HEALTH_DETAILS=when-authorized` in production**

---

## 📝 NOTES

- All variables marked as **REQUIRED** must be set for the application to start
- Variables with defaults will use the default value if not specified
- Profile-specific variables only apply when that profile is active
- Cloud deployments should use environment-specific secrets management
- Test environments can use the provided `.env.test` file
- Development environments can use the provided `.env.dev` file
