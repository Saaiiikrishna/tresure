# Treasure Hunt Adventures - Registration System

A comprehensive Spring Boot web application for managing treasure hunt registrations with multi-step forms, file uploads, email notifications, and admin panel.

## 🌟 Features

### Public Features
- **Responsive Landing Page** with beige theme and modern design
- **Interactive Plan Display** with filtering by difficulty level
- **Multi-step Registration Form** with validation and file uploads
- **Real-time Form Validation** with user-friendly error messages
- **File Upload Support** for photos, ID documents, and medical certificates
- **Email Confirmations** with detailed registration information
- **YouTube Video Integration** for plan previews

### Admin Features
- **Secure Admin Panel** with Spring Security authentication
- **Dashboard** with statistics and charts
- **Plan Management** (Create, Read, Update, Delete)
- **Registration Management** with status updates
- **File Download** capabilities for uploaded documents
- **Search and Filter** functionality for registrations

### Technical Features
- **Spring Boot 3.x** with Java 17
- **PostgreSQL Database** with JPA/Hibernate
- **Email Integration** with JavaMailSender and Thymeleaf templates
- **File Storage** with validation and security
- **RESTful APIs** for frontend integration
- **Comprehensive Validation** with Bean Validation
- **Responsive Design** with Bootstrap 5

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 13+
- Gmail account for email functionality (optional)

### 1. Database Setup
```sql
-- Create database
CREATE DATABASE treasure_hunt_db;

-- Create user (optional)
CREATE USER treasure_user WITH PASSWORD 'treasure_pass';
GRANT ALL PRIVILEGES ON DATABASE treasure_hunt_db TO treasure_user;
```

### 2. Configuration
Update `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/treasure_hunt_db
    username: treasure_user
    password: treasure_pass
  
  mail:
    username: your-email@gmail.com
    password: your-app-password

app:
  security:
    admin:
      username: admin
      password: admin123
```

### 3. Gmail Setup (Optional)
1. Enable 2-Factor Authentication on your Gmail account
2. Generate an App Password: Google Account → Security → App passwords
3. Use the generated password in `application.yml`

### 4. Run the Application
```bash
# Clone the repository
git clone <repository-url>
cd treasure-hunt-registration

# Run with Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/treasure-hunt-registration-1.0.0.jar
```

### 5. Access the Application
- **Main Website**: http://localhost:8080
- **Admin Panel**: http://localhost:8080/admin
- **Admin Credentials**: admin / admin123 (change in production!)

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/treasurehunt/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST and Web controllers
│   │   ├── entity/          # JPA entities
│   │   ├── repository/      # Data repositories
│   │   ├── service/         # Business logic services
│   │   └── TreasureHuntApplication.java
│   └── resources/
│       ├── static/          # CSS, JS, images
│       ├── templates/       # Thymeleaf templates
│       └── application.yml  # Configuration
└── test/                    # Test classes
```

## 🗄️ Database Schema

### Tables
- **treasure_hunt_plans**: Adventure plans with pricing and details
- **user_registrations**: User registration data and status
- **uploaded_documents**: File metadata and storage information

### Key Relationships
- One plan can have many registrations
- One registration can have multiple documents
- Cascade delete for data integrity

## 🔧 Configuration Options

### File Upload Settings
Configure via environment variables:
```bash
# File upload limits (in bytes)
MAX_PHOTO_SIZE=2097152          # 2MB for registration photos
MAX_DOCUMENT_SIZE=5242880       # 5MB for documents
MAX_IMAGE_SIZE=5242880          # 5MB for admin image uploads
MAX_VIDEO_SIZE=52428800         # 50MB for admin video uploads

# File upload directories
FILE_UPLOAD_DIR=uploads/documents
IMAGE_UPLOAD_DIR=uploads/images

# Allowed file types
ALLOWED_PHOTO_TYPES=image/jpeg,image/jpg,image/png
ALLOWED_DOCUMENT_TYPES=application/pdf,image/jpeg,image/jpg
```

### Email Settings
```yaml
app:
  email:
    from: your-email@gmail.com
    support: support@treasurehuntadventures.com
    company-name: Treasure Hunt Adventures
```

### Security Settings
```yaml
app:
  security:
    admin:
      username: admin
      password: admin123  # Change in production!
```

## 🎨 Frontend Components

### Main Landing Page
- Hero section with call-to-action
- Plan cards with filtering
- Responsive grid layout
- Smooth scrolling navigation

### Registration Modal
- **Step 1**: Personal information form
- **Step 2**: File uploads and medical consent
- Real-time validation
- Progress indicators

### Admin Panel
- Dashboard with statistics
- Plan management interface
- Registration management
- Responsive design

## 📧 Email Templates

### Registration Confirmation
- Personalized greeting
- Plan details and pricing
- Registration number
- Pre-hunt checklist
- Contact information

### Status Updates
- Registration confirmation
- Cancellation notifications
- Admin notifications

## 🔒 Security Features

### Authentication
- Spring Security with form-based login
- BCrypt password encoding
- Session management
- CSRF protection for forms

### File Upload Security
- File type validation
- Size restrictions
- Secure file naming
- Path traversal prevention

### Data Validation
- Bean Validation annotations
- Custom validators
- Client-side validation
- Server-side validation

## 🧪 Testing

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=RegistrationServiceTest

# Run with coverage
mvn test jacoco:report
```

### Test Categories
- **Unit Tests**: Service layer testing
- **Integration Tests**: Controller and repository testing
- **Security Tests**: Authentication and authorization

## 🚀 Deployment

### Production Checklist
1. **Change default admin credentials**
2. **Configure production database**
3. **Set up SSL/TLS certificates**
4. **Configure email settings**
5. **Set up file storage location**
6. **Configure logging levels**
7. **Set up monitoring and health checks**

### Environment Variables
```bash
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
export GMAIL_USERNAME=your_email@gmail.com
export GMAIL_APP_PASSWORD=your_app_password
export ADMIN_USERNAME=your_admin_username
export ADMIN_PASSWORD=your_secure_password
```

### Docker Deployment (Optional)
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/treasure-hunt-registration-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 📊 API Endpoints

### Public APIs
- `GET /api/plans` - Get all available plans
- `GET /api/plans/{id}` - Get specific plan
- `POST /api/register` - Submit registration
- `GET /api/health` - Health check

### Admin APIs (Secured)
- `GET /admin/registrations/{id}` - Get registration details
- `POST /admin/registrations/{id}/status` - Update registration status
- `POST /admin/plans/{id}/toggle-status` - Toggle plan status

## 🐛 Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Check PostgreSQL is running
   - Verify connection details in `application.yml`
   - Ensure database exists

2. **Email Not Sending**
   - Verify Gmail credentials
   - Check App Password is correct
   - Ensure 2FA is enabled on Gmail

3. **File Upload Errors**
   - Check file size limits
   - Verify upload directory permissions
   - Ensure allowed file types are correct

4. **Admin Login Issues**
   - Verify admin credentials in configuration
   - Check password encoding
   - Clear browser cache

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

For support and questions:
- Email: support@treasurehuntadventures.com
- Documentation: [Project Wiki]
- Issues: [GitHub Issues]

---

**Built with ❤️ using Spring Boot, PostgreSQL, and Bootstrap**
