-- =====================================================================
-- FlashChoice-Mart 增量脚本：t_refresh_token 刷新令牌表
-- 用途：JWT 双 Token 认证接入时单独执行（主脚本 flashchoice-mart.sql 已含同款 DDL）
-- 可重复执行：CREATE TABLE IF NOT EXISTS
-- 说明：由 user-api（audience=admin）与 member-api（audience=member）共用；
--       Refresh Token 一次性轮换，刷新/登出时删除旧记录实现主动失效
-- =====================================================================

USE `flashchoice-mart`;

CREATE TABLE IF NOT EXISTS `t_refresh_token` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `audience`    VARCHAR(16)  NOT NULL                COMMENT '令牌类型（admin 后台用户 / member 会员）',
    `user_id`     BIGINT       NOT NULL                COMMENT '用户ID（按 audience 关联 user/member 主键）',
    `token`       VARCHAR(600) NOT NULL                COMMENT '刷新令牌（JWT 串）',
    `expire_time` DATETIME     NOT NULL                COMMENT '过期时间',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_token` (`token`),
    KEY `idx_audience_user` (`audience`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '刷新令牌';
