-- Apply once to an existing FlashChoice-Mart database before enabling grabs.
CREATE TABLE IF NOT EXISTS `seckill_grab` (
    `seckill_no` VARCHAR(32) NOT NULL PRIMARY KEY,
    `seckill_good_id` BIGINT NOT NULL,
    `member_id` BIGINT NOT NULL,
    `member_account` VARCHAR(30) NOT NULL,
    `addr_id` BIGINT NOT NULL,
    `seckill_price` DECIMAL(10,2) NOT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    `order_no` VARCHAR(32) DEFAULT NULL,
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_grab_member_good` (`member_id`, `seckill_good_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Existing installations should add this index once (MySQL 8 does not support IF NOT EXISTS here).
ALTER TABLE `order` ADD UNIQUE KEY `uk_order_seckill_no` (`seckill_no`);
