package com.example.fcmcommon.util;

/**
 * 雪花 ID 生成器（64 位：1 符号位 + 41 时间戳 + 5 数据中心 + 5 机器 + 12 序列号）
 *
 * <p>时间戳起点 2024-01-01，41 位可用约 69 年；单节点每毫秒最多 4096 个 ID。
 * 线程安全（synchronized），同一实例内序列号按毫秒自然递增。
 *
 * <p>使用约定：每个服务实例的 workerId 需唯一（本地单实例固定传 1 即可）；
 * 时钟回拨超过 5ms 直接拒绝生成，避免产生重复 ID 破坏唯一约束。
 */
public class SnowflakeIdGenerator {

    /** 起始时间戳（2024-01-01 00:00:00 UTC） */
    private static final long EPOCH = 1704067200000L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    /** 可容忍的时钟回拨毫秒数（超出直接拒绝） */
    private static final long MAX_BACKWARD_MS = 5L;

    private final long workerId;
    private final long dataCenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId, long dataCenterId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId 必须在 0~" + MAX_WORKER_ID + " 之间");
        }
        if (dataCenterId < 0 || dataCenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException("dataCenterId 必须在 0~" + MAX_DATACENTER_ID + " 之间");
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
    }

    /** 生成下一个 ID（线程安全） */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            long backward = lastTimestamp - timestamp;
            if (backward > MAX_BACKWARD_MS) {
                throw new IllegalStateException("检测到时钟回拨 " + backward + "ms，拒绝生成雪花 ID");
            }
            // 小幅回拨：自旋等待时钟追平
            timestamp = waitUntil(lastTimestamp);
        }
        if (timestamp == lastTimestamp) {
            // 同毫秒内序列号自增；溢出（满 4096）则等待下一毫秒
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitUntil(lastTimestamp + 1);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (dataCenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /** 自旋等待直到系统时钟到达目标毫秒 */
    private long waitUntil(long targetMillis) {
        long timestamp = System.currentTimeMillis();
        while (timestamp < targetMillis) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
