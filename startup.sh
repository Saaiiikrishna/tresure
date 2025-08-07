#!/bin/bash

# Azure App Service Startup Script for Treasure Hunt Adventures
# This script is used by Azure App Service to start the Spring Boot application

echo "🚀 Starting Treasure Hunt Adventures Application..."
echo "=================================================="

# Set JVM options for production
export JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -Dspring.profiles.active=production"

# Create logs directory if it doesn't exist
mkdir -p logs

# Start the application
echo "📋 Starting Spring Boot application with production profile..."
java $JAVA_OPTS -jar /home/site/wwwroot/*.jar

echo "✅ Application startup script completed"
