package com.example.fcmcommon.util;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SnowflakeIdGenerator 单元测试：覆盖唯一性、单调递增、并发安全与参数校验
 */
class SnowflakeIdGeneratorTest {

    @Test
    void generatedIdsShouldBeUniqueAndIncreasing() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        Set<Long> ids = ConcurrentHashMap.newKeySet();

        long prev = generator.nextId();
        ids.add(prev);
        for (int i = 0; i < 10_000; i++) {
            long id = generator.nextId();
            assertThat(id).as("同一实例生成的 ID 应单调递增").isGreaterThan(prev);
            ids.add(id);
            prev = id;
        }
        assertThat(ids).as("生成的 ID 应无重复").hasSize(10_001);
    }

    @Test
    void concurrentGenerationShouldNotProduceDuplicates() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        int total = 16_000;

        IntStream.range(0, total).parallel().forEach(i -> ids.add(generator.nextId()));

        assertThat(ids).as("并发生成不应产生重复 ID").hasSize(total);
    }

    @Test
    void invalidWorkerIdOrDataCenterIdShouldBeRejected() {
        assertThatThrownBy(() -> new SnowflakeIdGenerator(-1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SnowflakeIdGenerator(32, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SnowflakeIdGenerator(1, 32))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void idShouldBePositiveAndWithinLongRange() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(31, 31);
        assertThat(generator.nextId()).isPositive();
    }
}
