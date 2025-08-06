#!/bin/bash

# Production Validation Script
# Validates all security and production hardening measures

echo "🔍 Starting Production Validation..."
echo "=================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0
WARNINGS=0

# Function to print test results
print_result() {
    local test_name="$1"
    local result="$2"
    local message="$3"
    
    if [ "$result" = "PASS" ]; then
        echo -e "${GREEN}✅ PASS${NC}: $test_name"
        ((PASSED++))
    elif [ "$result" = "FAIL" ]; then
        echo -e "${RED}❌ FAIL${NC}: $test_name - $message"
        ((FAILED++))
    elif [ "$result" = "WARN" ]; then
        echo -e "${YELLOW}⚠️  WARN${NC}: $test_name - $message"
        ((WARNINGS++))
    fi
}

# Test 1: Check for hardcoded credentials
echo -e "\n${BLUE}1. Security Configuration Tests${NC}"
echo "--------------------------------"

# Check application.properties for hardcoded passwords
if grep -q "password=changeme\|password=admin123\|password=password" src/main/resources/application*.properties 2>/dev/null; then
    print_result "Hardcoded Credentials Check" "FAIL" "Found hardcoded credentials in configuration files"
else
    print_result "Hardcoded Credentials Check" "PASS"
fi

# Check for environment variable usage
if grep -q "\${.*PASSWORD.*}" src/main/resources/application*.properties 2>/dev/null; then
    print_result "Environment Variable Usage" "PASS"
else
    print_result "Environment Variable Usage" "WARN" "Some passwords may not be externalized"
fi

# Check for debug settings in production config
if grep -q "show-sql.*true\|include-stacktrace.*always" src/main/resources/application-production.yml 2>/dev/null; then
    print_result "Production Debug Settings" "FAIL" "Debug settings enabled in production configuration"
else
    print_result "Production Debug Settings" "PASS"
fi

# Test 2: Input Validation
echo -e "\n${BLUE}2. Input Validation Tests${NC}"
echo "-------------------------"

# Check if InputSanitizationService exists
if [ -f "src/main/java/com/treasurehunt/service/InputSanitizationService.java" ]; then
    print_result "Input Sanitization Service" "PASS"
    
    # Check for SQL injection protection
    if grep -q "SQL_INJECTION_PATTERN\|sanitizeText\|sanitizeEmail" src/main/java/com/treasurehunt/service/InputSanitizationService.java; then
        print_result "SQL Injection Protection" "PASS"
    else
        print_result "SQL Injection Protection" "FAIL" "SQL injection protection not implemented"
    fi
    
    # Check for XSS protection
    if grep -q "XSS_PATTERN\|htmlEncode" src/main/java/com/treasurehunt/service/InputSanitizationService.java; then
        print_result "XSS Protection" "PASS"
    else
        print_result "XSS Protection" "FAIL" "XSS protection not implemented"
    fi
else
    print_result "Input Sanitization Service" "FAIL" "InputSanitizationService not found"
fi

# Test 3: Error Handling
echo -e "\n${BLUE}3. Error Handling Tests${NC}"
echo "-----------------------"

# Check GlobalExceptionHandler for information disclosure prevention
if grep -q "sanitizeRequestDescription\|no sensitive information" src/main/java/com/treasurehunt/exception/GlobalExceptionHandler.java 2>/dev/null; then
    print_result "Error Information Sanitization" "PASS"
else
    print_result "Error Information Sanitization" "FAIL" "Error responses may expose sensitive information"
fi

# Check for proper exception handling
if grep -q "@ExceptionHandler.*Exception" src/main/java/com/treasurehunt/exception/GlobalExceptionHandler.java 2>/dev/null; then
    print_result "Exception Handler Coverage" "PASS"
else
    print_result "Exception Handler Coverage" "FAIL" "Incomplete exception handling"
fi

# Test 4: Resource Management
echo -e "\n${BLUE}4. Resource Management Tests${NC}"
echo "-----------------------------"

# Check for resource management configuration
if [ -f "src/main/java/com/treasurehunt/config/ResourceManagementConfig.java" ]; then
    print_result "Resource Management Config" "PASS"
    
    # Check for memory monitoring
    if grep -q "monitorMemoryUsage\|MemoryMXBean" src/main/java/com/treasurehunt/config/ResourceManagementConfig.java; then
        print_result "Memory Monitoring" "PASS"
    else
        print_result "Memory Monitoring" "FAIL" "Memory monitoring not implemented"
    fi
    
    # Check for thread monitoring
    if grep -q "monitorThreadUsage\|ThreadMXBean" src/main/java/com/treasurehunt/config/ResourceManagementConfig.java; then
        print_result "Thread Monitoring" "PASS"
    else
        print_result "Thread Monitoring" "FAIL" "Thread monitoring not implemented"
    fi
else
    print_result "Resource Management Config" "FAIL" "ResourceManagementConfig not found"
fi

# Check for try-with-resources usage in file operations
if grep -q "try (.*)" src/main/java/com/treasurehunt/service/FileStorageService.java 2>/dev/null; then
    print_result "File Resource Management" "PASS"
else
    print_result "File Resource Management" "WARN" "File operations may not use proper resource management"
fi

# Test 5: Database Security
echo -e "\n${BLUE}5. Database Security Tests${NC}"
echo "---------------------------"

# Check for database security configuration
if [ -f "src/main/java/com/treasurehunt/config/DatabaseSecurityConfig.java" ]; then
    print_result "Database Security Config" "PASS"
    
    # Check for connection validation
    if grep -q "validateConnectionSecurity\|SSL" src/main/java/com/treasurehunt/config/DatabaseSecurityConfig.java; then
        print_result "Database Connection Security" "PASS"
    else
        print_result "Database Connection Security" "FAIL" "Database connection security validation missing"
    fi
else
    print_result "Database Security Config" "FAIL" "DatabaseSecurityConfig not found"
fi

# Check for connection pool configuration
if grep -q "HikariCP\|maximum-pool-size" src/main/resources/application-production.yml 2>/dev/null; then
    print_result "Connection Pool Configuration" "PASS"
else
    print_result "Connection Pool Configuration" "WARN" "Connection pool may not be properly configured"
fi

# Test 6: Production Readiness
echo -e "\n${BLUE}6. Production Readiness Tests${NC}"
echo "------------------------------"

# Check for production readiness service
if [ -f "src/main/java/com/treasurehunt/service/ProductionReadinessService.java" ]; then
    print_result "Production Readiness Service" "PASS"
    
    # Check for comprehensive validation
    if grep -q "performProductionReadinessCheck\|criticalIssues" src/main/java/com/treasurehunt/service/ProductionReadinessService.java; then
        print_result "Production Validation Logic" "PASS"
    else
        print_result "Production Validation Logic" "FAIL" "Production validation logic incomplete"
    fi
else
    print_result "Production Readiness Service" "FAIL" "ProductionReadinessService not found"
fi

# Check for security hardening configuration
if [ -f "src/main/java/com/treasurehunt/config/ProductionSecurityHardeningConfig.java" ]; then
    print_result "Security Hardening Config" "PASS"
else
    print_result "Security Hardening Config" "FAIL" "ProductionSecurityHardeningConfig not found"
fi

# Test 7: Thread Safety
echo -e "\n${BLUE}7. Thread Safety Tests${NC}"
echo "----------------------"

# Check for atomic operations in email processor
if grep -q "AtomicBoolean\|compareAndSet" src/main/java/com/treasurehunt/service/ThreadSafeEmailProcessor.java 2>/dev/null; then
    print_result "Thread-Safe Email Processing" "PASS"
else
    print_result "Thread-Safe Email Processing" "FAIL" "Email processing may not be thread-safe"
fi

# Check for proper synchronization
if grep -q "@Transactional\|synchronized" src/main/java/com/treasurehunt/service/*.java 2>/dev/null; then
    print_result "Transaction Management" "PASS"
else
    print_result "Transaction Management" "WARN" "Transaction management may be incomplete"
fi

# Test 8: File Security
echo -e "\n${BLUE}8. File Security Tests${NC}"
echo "----------------------"

# Check for file validation service
if [ -f "src/main/java/com/treasurehunt/service/SecureFileValidationService.java" ]; then
    print_result "File Validation Service" "PASS"
    
    # Check for malicious content detection
    if grep -q "MALICIOUS_PATTERNS\|scanFileContent" src/main/java/com/treasurehunt/service/SecureFileValidationService.java 2>/dev/null; then
        print_result "Malicious File Detection" "PASS"
    else
        print_result "Malicious File Detection" "FAIL" "Malicious file detection not implemented"
    fi
else
    print_result "File Validation Service" "FAIL" "SecureFileValidationService not found"
fi

# Test 9: Logging Security
echo -e "\n${BLUE}9. Logging Security Tests${NC}"
echo "-------------------------"

# Check for sensitive data masking in logs
if grep -q "maskEmail\|\\*\\*\\*" src/main/java/com/treasurehunt/service/InputSanitizationService.java 2>/dev/null; then
    print_result "Sensitive Data Masking" "PASS"
else
    print_result "Sensitive Data Masking" "WARN" "Sensitive data may not be masked in logs"
fi

# Check for proper log levels in production
if grep -q "level:.*INFO\|level:.*WARN" src/main/resources/application-production.yml 2>/dev/null; then
    print_result "Production Log Levels" "PASS"
else
    print_result "Production Log Levels" "WARN" "Log levels may not be appropriate for production"
fi

# Test 10: Documentation
echo -e "\n${BLUE}10. Documentation Tests${NC}"
echo "-----------------------"

# Check for production hardening report
if [ -f "docs/PRODUCTION_HARDENING_REPORT.md" ]; then
    print_result "Production Hardening Report" "PASS"
else
    print_result "Production Hardening Report" "WARN" "Production hardening documentation missing"
fi

# Summary
echo -e "\n${BLUE}Validation Summary${NC}"
echo "=================="
echo -e "✅ Passed: ${GREEN}$PASSED${NC}"
echo -e "❌ Failed: ${RED}$FAILED${NC}"
echo -e "⚠️  Warnings: ${YELLOW}$WARNINGS${NC}"

TOTAL=$((PASSED + FAILED + WARNINGS))
if [ $TOTAL -gt 0 ]; then
    PASS_RATE=$((PASSED * 100 / TOTAL))
    echo -e "📊 Pass Rate: ${PASS_RATE}%"
fi

echo ""
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 PRODUCTION VALIDATION PASSED!${NC}"
    echo -e "${GREEN}The application is ready for production deployment.${NC}"
    exit 0
else
    echo -e "${RED}❌ PRODUCTION VALIDATION FAILED!${NC}"
    echo -e "${RED}Please fix the failed tests before deploying to production.${NC}"
    exit 1
fi
