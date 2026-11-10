-- Add read_at column to notifications table for user-facing inbox read status.
ALTER TABLE notifications ADD COLUMN read_at TIMESTAMP;
CREATE INDEX idx_notifications_user_read ON notifications(user_id, read_at);
