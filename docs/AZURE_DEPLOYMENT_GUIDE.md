# 🚀 Azure App Service Deployment Guide

## 📋 Overview
This guide provides step-by-step instructions for deploying the Treasure Hunt Adventures application to Azure App Service using GitHub Actions with code deployment (not container).

## 🏗️ Azure App Service Setup

### 1. **Create Azure App Service**
```bash
# Using Azure CLI (optional)
az webapp create \
  --resource-group your-resource-group \
  --plan your-app-service-plan \
  --name treasure-hunt-adventures \
  --runtime "JAVA:17-java17"
```

### 2. **Configure App Service Settings**
In Azure Portal → App Service → Configuration → Application settings:

#### **Required Environment Variables:**
```bash
# Security Configuration
ADMIN_USERNAME=your_secure_admin_username
ADMIN_PASSWORD=your_very_secure_password_123!

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_gmail_username@gmail.com
MAIL_PASSWORD=your_gmail_app_password
MAIL_FROM_ADDRESS=noreply@yourdomain.com
MAIL_SUPPORT_ADDRESS=support@yourdomain.com
MAIL_CONNECTION_TIMEOUT=10000
MAIL_TIMEOUT=10000
MAIL_WRITE_TIMEOUT=10000

# Azure PostgreSQL Database Configuration
DB_HOST=mysillydreamspg.postgres.database.azure.com
DB_PORT=5432
DB_NAME=postgres
DB_USERNAME=pgadmin
DB_PASSWORD=your_secure_database_password
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=2
DB_CONNECTION_TIMEOUT=20000
DB_IDLE_TIMEOUT=300000
DB_MAX_LIFETIME=1200000
DB_LEAK_DETECTION=60000

# JPA Configuration
JPA_DDL_AUTO=create-drop

# Company Information
COMPANY_NAME=Treasure Hunt Adventures

# File Upload Configuration
FILE_UPLOAD_DIR=uploads/documents
MAX_PHOTO_SIZE=2097152
MAX_DOCUMENT_SIZE=5242880
UPLOAD_MAX_FILE_SIZE=5MB
UPLOAD_MAX_REQUEST_SIZE=15MB
ALLOWED_PHOTO_TYPES=image/jpeg,image/jpg,image/png
ALLOWED_DOCUMENT_TYPES=application/pdf,image/jpeg,image/jpg

# Application Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=production
HTTP2_ENABLED=true

# Monitoring Configuration
LOG_FILE_PATH=logs/treasure-hunt.log
LOG_MAX_SIZE=10MB
LOG_MAX_HISTORY=30
MANAGEMENT_PORT=8081
ACTUATOR_HEALTH_DETAILS=never
PROMETHEUS_ENABLED=false

# Security Settings
REQUIRE_SSL=false
CLEANUP_TEMP_FILE_RETENTION_HOURS=24
CLEANUP_LOG_RETENTION_DAYS=30
```

### 3. **Configure Java Runtime**
- **Java Version**: 17
- **Java Minor Version**: Latest
- **Web Server**: Embedded (Spring Boot)

## 🔧 GitHub Actions Setup

### 1. **Get Publish Profile**
1. Go to Azure Portal → App Service → Overview
2. Click "Get publish profile"
3. Download the `.publishsettings` file
4. Copy the entire content of the file

### 2. **Add GitHub Secrets**
1. Go to your GitHub repository → Settings → Secrets and variables → Actions
2. Add new repository secret:
   - **Name**: `AZUREAPPSERVICE_PUBLISHPROFILE`
   - **Value**: Paste the entire content of the publish profile

### 3. **Update Workflow Configuration**
Edit `.github/workflows/azure-deploy.yml`:
```yaml
env:
  AZURE_WEBAPP_NAME: your-actual-app-service-name  # Change this to your App Service name
```

## 🗄️ Database Configuration

### **Azure PostgreSQL Setup:**
Your application is configured to connect to:
- **Host**: `mysillydreamspg.postgres.database.azure.com`
- **Port**: `5432`
- **Database**: `postgres`
- **Username**: `pgadmin`
- **Password**: Set via `DB_PASSWORD` environment variable

### **Important Notes:**
1. **First Deployment**: Use `JPA_DDL_AUTO=create-drop` to create fresh tables
2. **Subsequent Deployments**: Change to `JPA_DDL_AUTO=validate` to preserve data
3. **SSL Required**: Connection uses `sslmode=require` for security

## 🚀 Deployment Process

### **Automatic Deployment:**
1. Push code to `main` or `master` branch
2. GitHub Actions will automatically:
   - Build the application
   - Run tests
   - Deploy to Azure App Service

### **Manual Deployment:**
1. Go to GitHub repository → Actions
2. Select "Deploy to Azure App Service" workflow
3. Click "Run workflow"

## 📊 Post-Deployment Verification

### **1. Health Check**
```bash
curl https://your-app-name.azurewebsites.net/actuator/health
```

### **2. Application Access**
- **Main Application**: `https://your-app-name.azurewebsites.net`
- **Admin Panel**: `https://your-app-name.azurewebsites.net/admin`
- **Health Endpoint**: `https://your-app-name.azurewebsites.net/actuator/health`

### **3. Log Monitoring**
- Azure Portal → App Service → Log stream
- Or use Azure CLI: `az webapp log tail --name your-app-name --resource-group your-rg`

## 🔒 Security Considerations

### **1. Environment Variables**
- All sensitive data is stored in Azure App Service configuration
- No credentials are stored in code or GitHub repository

### **2. Database Security**
- SSL connection to Azure PostgreSQL
- Connection pooling with proper timeouts
- No hardcoded database credentials

### **3. Application Security**
- Production profile enabled by default
- Debug endpoints disabled
- Error messages sanitized
- Security headers configured

## 🛠️ Troubleshooting

### **Common Issues:**

#### **1. Application Won't Start**
- Check environment variables are set correctly
- Verify database connectivity
- Check application logs in Azure Portal

#### **2. Database Connection Failed**
- Verify Azure PostgreSQL firewall rules
- Check database credentials
- Ensure SSL is properly configured

#### **3. Email Not Working**
- Verify Gmail App Password (not regular password)
- Check SMTP settings
- Ensure Gmail account allows less secure apps (if needed)

#### **4. File Upload Issues**
- Check file size limits in environment variables
- Verify upload directory permissions
- Monitor disk space usage

### **Debugging Commands:**
```bash
# Check application logs
az webapp log tail --name your-app-name --resource-group your-rg

# Check environment variables
az webapp config appsettings list --name your-app-name --resource-group your-rg

# Restart application
az webapp restart --name your-app-name --resource-group your-rg
```

## 📈 Monitoring & Maintenance

### **1. Application Insights** (Recommended)
- Enable Application Insights in Azure Portal
- Monitor performance, errors, and usage

### **2. Log Analytics**
- Configure log forwarding to Azure Monitor
- Set up alerts for errors and performance issues

### **3. Backup Strategy**
- Database: Configure automated backups for Azure PostgreSQL
- Files: Use Azure Storage for uploaded files (future enhancement)

## 🎯 Performance Optimization

### **1. App Service Plan**
- Use appropriate pricing tier for your traffic
- Consider auto-scaling for variable loads

### **2. Database Performance**
- Monitor connection pool usage
- Optimize queries if needed
- Consider read replicas for high traffic

### **3. CDN Integration** (Future)
- Use Azure CDN for static assets
- Improve global performance

## ✅ Deployment Checklist

Before going live:
- [ ] All environment variables configured in Azure App Service
- [ ] Database connectivity tested
- [ ] GitHub Actions workflow configured
- [ ] Publish profile added to GitHub secrets
- [ ] SSL certificate configured (if using custom domain)
- [ ] Admin credentials tested
- [ ] Email functionality verified
- [ ] File upload functionality tested
- [ ] Health endpoints accessible
- [ ] Monitoring configured

---

## 🎉 Ready for Production!

Your Treasure Hunt Adventures application is now configured for seamless deployment to Azure App Service using GitHub Actions. The setup provides:

- ✅ **Automated CI/CD** with GitHub Actions
- ✅ **Secure configuration** with environment variables
- ✅ **Azure PostgreSQL integration**
- ✅ **Production-ready security**
- ✅ **Comprehensive monitoring**

**Next Steps:**
1. Configure your Azure App Service
2. Set up environment variables
3. Add GitHub secrets
4. Push to main branch to trigger deployment

**🚀 Your application will be live at: `https://your-app-name.azurewebsites.net`**
