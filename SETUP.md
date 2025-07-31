# 🚀 Treasure Hunt Adventures - Quick Setup Guide

## Prerequisites ✅
- ✅ Windows (Development)
- ✅ Java 17+ installed
- ✅ Maven installed
- ✅ Docker Desktop installed and running

## 🗄️ Database Setup (PostgreSQL with Docker)

### Step 1: Start the Database
```bash
# Run the database startup script
start-database.bat
```

This will:
- Pull PostgreSQL 15 Alpine image
- Create `treasure_hunt_db` database
- Set up user `treasure_user` with password `treasure_pass_2024`
- Initialize the database with proper permissions
- Expose database on `localhost:5432`

### Step 2: Verify Database
The script will automatically test the connection. You should see:
```
✅ SUCCESS: Database is ready!
```

## 📧 Email Configuration

### Gmail Setup ✅
- **Email**: tresurhunting@gmail.com
- **App Password**: vhgf rrxt yede hnae
- **Configuration**: Already configured in `.env` file

### Email Features
- Registration confirmations
- Status update notifications
- Admin notifications
- Pre-hunt checklists

## 🔐 Admin Panel Configuration

### Default Credentials ✅
- **Username**: admin
- **Password**: admin123
- **Access URL**: http://localhost:8080/admin

### Admin Features
- Dashboard with statistics
- Plan management (CRUD)
- Registration management
- File downloads
- Status updates

## 🚀 Application Startup

### Step 1: Start the Application
```bash
# Run the application startup script
start-application.bat
```

### Step 2: Access the System
- **Main Website**: http://localhost:8080
- **Admin Panel**: http://localhost:8080/admin

## 🧪 Testing Checklist

### 1. Website Testing
- [ ] Landing page loads with sample plans
- [ ] Plan filtering works (Beginner/Intermediate/Advanced)
- [ ] Registration modal opens
- [ ] Multi-step form navigation works

### 2. Registration Flow Testing
- [ ] Step 1: Personal information form validation
- [ ] Step 2: File upload areas work
- [ ] Medical consent checkbox required
- [ ] Form submission successful
- [ ] Confirmation email received

### 3. Admin Panel Testing
- [ ] Admin login works (admin/admin123)
- [ ] Dashboard shows statistics
- [ ] Plan management functions
- [ ] Registration list displays
- [ ] Status updates work

### 4. Email Testing
- [ ] Registration confirmation emails sent
- [ ] Emails have proper formatting
- [ ] Pre-hunt checklist included
- [ ] Company branding present

## 📁 File Structure
```
TreasureHunt/
├── docker-compose.yml          # PostgreSQL container setup
├── init-db.sql                 # Database initialization
├── .env                        # Environment variables
├── start-database.bat          # Database startup script
├── start-application.bat       # Application startup script
├── pom.xml                     # Maven dependencies
├── src/
│   ├── main/
│   │   ├── java/com/treasurehunt/
│   │   │   ├── config/         # Configuration classes
│   │   │   ├── controller/     # Web controllers
│   │   │   ├── entity/         # JPA entities
│   │   │   ├── repository/     # Data repositories
│   │   │   ├── service/        # Business services
│   │   │   └── TreasureHuntApplication.java
│   │   └── resources/
│   │       ├── static/         # CSS, JS, images
│   │       ├── templates/      # Thymeleaf templates
│   │       └── application.yml # Application config
│   └── test/                   # Test classes
└── uploads/                    # File upload directory
```

## 🔧 Configuration Details

### Database Connection
- **Host**: localhost
- **Port**: 5432
- **Database**: treasure_hunt_db
- **Username**: treasure_user
- **Password**: treasure_pass_2024

### Email Settings
- **SMTP Host**: smtp.gmail.com
- **Port**: 587
- **Username**: tresurhunting@gmail.com
- **App Password**: vhgf rrxt yede hnae

### File Upload Limits
- **Photos**: 2MB max (JPG, PNG)
- **Documents**: 5MB max (PDF, JPG)
- **Storage**: uploads/documents/

## 🚨 Troubleshooting

### Database Issues
```bash
# Check container status
docker-compose ps

# View database logs
docker-compose logs postgres

# Restart database
docker-compose restart postgres
```

### Application Issues
```bash
# Check Java version
java -version

# Check Maven version
mvn -version

# Clean and rebuild
mvn clean compile
```

### Email Issues
- Verify Gmail app password is correct
- Check Gmail account has 2FA enabled
- Ensure no spaces in app password

## 🎯 Next Steps

1. **Run start-database.bat** to set up PostgreSQL
2. **Run start-application.bat** to start the application
3. **Open http://localhost:8080** to view the website
4. **Open http://localhost:8080/admin** to access admin panel
5. **Test the complete registration flow**
6. **Verify email functionality**

## 📞 Support

If you encounter any issues:
1. Check the troubleshooting section above
2. Review application logs in the console
3. Verify all prerequisites are installed
4. Ensure Docker Desktop is running

**Ready to launch your Treasure Hunt Adventures! 🗺️⚡**
