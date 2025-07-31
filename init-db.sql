-- Initialize Treasure Hunt Database
-- This script runs when the PostgreSQL container starts for the first time

-- Connect to the database
\c treasure_hunt_db;

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create our custom user
CREATE USER treasure_user WITH PASSWORD 'treasure_pass_2024';

-- Grant all privileges to the user
GRANT ALL PRIVILEGES ON DATABASE treasure_hunt_db TO treasure_user;
GRANT ALL PRIVILEGES ON SCHEMA public TO treasure_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO treasure_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO treasure_user;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO treasure_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO treasure_user;

-- Make treasure_user a superuser for development
ALTER USER treasure_user WITH SUPERUSER;

-- Display success message
SELECT 'Database initialized successfully for Treasure Hunt Adventures!' as status;
