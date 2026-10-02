package com.yinfires.icecore.time;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证随机刻缩放逻辑的正确性
 */
class RandomTickScalingTest {

    @Test
    void scalingFormula() {
        // 原版默认 randomTickSpeed = 3
        int baseSpeed = 3;

        // 时间倍率 1（正常速度）：不缩放
        assertEquals(0, RandomTickScaling.compensationAttempts(baseSpeed, 1.0));

        // 时间倍率 10（时间减慢 10 倍）：额外补偿 9 倍，合计 10 倍
        assertEquals(27, RandomTickScaling.compensationAttempts(baseSpeed, 10.0));

        // 时间倍率 0.5（时间加快 2 倍）：额外补偿 1 倍
        assertEquals(3, RandomTickScaling.compensationAttempts(baseSpeed, 0.5));

        // 0.01 倍率必须得到 100 倍总随机刻密度，而不是经验系数放大
        assertEquals(297, RandomTickScaling.compensationAttempts(baseSpeed, 0.01));

        // 极端情况：倍率 100，限制在 1000 以内
        assertEquals(297, RandomTickScaling.compensationAttempts(baseSpeed, 100.0));

        // 上限测试：超大倍率被限制
        assertEquals(RandomTickScaling.MAX_COMPENSATION_PER_CHUNK_TICK,
                RandomTickScaling.compensationAttempts(baseSpeed, 0.000001));
    }

    @Test
    void edgeCases() {
        // randomTickSpeed 为 0 时保持 0
        assertEquals(0, RandomTickScaling.compensationAttempts(0, 10.0));

        // 倍率接近 1 时向上取整
        assertEquals(1, RandomTickScaling.compensationAttempts(3, 1.1));
        assertEquals(0, RandomTickScaling.compensationAttempts(3, 1.0));
    }
}
