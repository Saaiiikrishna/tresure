-- =====================================================
-- CRITICAL PERFORMANCE INDEXES
-- Fixes slow queries identified in production logs
-- =====================================================

-- 1. FIX: TreasureHuntPlanRepository.findByStatusOrderByCreatedDateDesc (3472ms)
-- Add composite index for status + created_date ordering
CREATE INDEX IF NOT EXISTS idx_treasure_hunt_plans_status_created_date 
ON treasure_hunt_plans(status, created_date DESC);

-- 2. FIX: Email queue processing performance (517ms)
-- Add composite index for email queue status queries
CREATE INDEX IF NOT EXISTS idx_email_queue_status_priority_created 
ON email_queue(status, priority, created_date);

-- 3. FIX: Failed email retry queries
-- Add composite index for failed email retry logic
CREATE INDEX IF NOT EXISTS idx_email_queue_failed_retry 
ON email_queue(status, retry_count, max_retry_attempts) 
WHERE status = 'FAILED';

-- 4. FIX: Email queue pending/scheduled queries
-- Add composite index for pending and scheduled emails
CREATE INDEX IF NOT EXISTS idx_email_queue_pending_scheduled 
ON email_queue(status, scheduled_date, priority, created_date) 
WHERE status IN ('PENDING', 'SCHEDULED');

-- 5. FIX: User registration plan queries
-- Add composite index for registration counts by plan
CREATE INDEX IF NOT EXISTS idx_user_registrations_plan_status 
ON user_registrations(plan_id, status);

-- 6. FIX: User registration date queries
-- Add index for registration date ordering
CREATE INDEX IF NOT EXISTS idx_user_registrations_date 
ON user_registrations(registration_date DESC);

-- 7. FIX: App settings key lookups
-- Add index for settings key queries (if not exists)
CREATE INDEX IF NOT EXISTS idx_app_settings_key 
ON app_settings(setting_key);

-- 8. FIX: Email campaigns date ordering
-- Add index for email campaigns date queries
CREATE INDEX IF NOT EXISTS idx_email_campaigns_created_date 
ON email_campaigns(created_date DESC);

-- 9. FIX: Email campaigns status queries
-- Add index for email campaigns status
CREATE INDEX IF NOT EXISTS idx_email_campaigns_status 
ON email_campaigns(status);

-- 10. FIX: Uploaded images category queries
-- Add index for image category lookups
CREATE INDEX IF NOT EXISTS idx_uploaded_images_category_active
ON uploaded_images(image_category, is_active)
WHERE is_active = true;

-- 11. FIX: Featured plan queries (addresses slow featured plan lookup)
-- Add composite index for featured plan status queries
CREATE INDEX IF NOT EXISTS idx_treasure_hunt_plans_featured_status
ON treasure_hunt_plans(is_featured, status);

-- =====================================================
-- QUERY PERFORMANCE STATISTICS
-- =====================================================

-- Update table statistics for better query planning
ANALYZE treasure_hunt_plans;
ANALYZE email_queue;
ANALYZE user_registrations;
ANALYZE app_settings;
ANALYZE email_campaigns;
ANALYZE uploaded_images;

-- =====================================================
-- PERFORMANCE MONITORING
-- =====================================================

-- Enable query performance monitoring (PostgreSQL specific)
-- These settings help identify slow queries in production
-- Note: These are session-level settings, consider adding to postgresql.conf for permanent effect

-- Log slow queries (queries taking more than 1 second)
-- SET log_min_duration_statement = 1000;

-- Log query plans for slow queries
-- SET log_statement = 'all';

-- Enable query statistics collection
-- SET track_activities = on;
-- SET track_counts = on;
-- SET track_io_timing = on;
-- SET track_functions = 'all';

-- =====================================================
-- INDEX USAGE VERIFICATION
-- =====================================================

-- Query to check index usage (run after deployment):
-- SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'public'
-- ORDER BY idx_scan DESC;

-- Query to check table scan statistics:
-- SELECT schemaname, tablename, seq_scan, seq_tup_read, idx_scan, idx_tup_fetch
-- FROM pg_stat_user_tables
-- WHERE schemaname = 'public'
-- ORDER BY seq_scan DESC;

-- =====================================================
-- EXPECTED PERFORMANCE IMPROVEMENTS
-- =====================================================

-- 1. TreasureHuntPlan queries: 3472ms → <100ms (97% improvement)
-- 2. Email queue processing: 517ms → <50ms (90% improvement)  
-- 3. User registration queries: Significant improvement for large datasets
-- 4. App settings lookups: Near-instant with key index
-- 5. Email campaign queries: Faster dashboard loading
-- 6. Image management: Faster category-based queries

-- CRITICAL: These indexes address the specific slow queries identified in production logs
-- Monitor query performance after deployment to verify improvements
