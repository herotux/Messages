package org.fossify.commons.compose.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomaDesignTokensTest {
    @Test
    fun spacing_is_monotonic() {
        val values = listOf(
            HomaDesignTokens.Spacing.xxs,
            HomaDesignTokens.Spacing.xs,
            HomaDesignTokens.Spacing.sm,
            HomaDesignTokens.Spacing.md,
            HomaDesignTokens.Spacing.lg,
            HomaDesignTokens.Spacing.xl,
            HomaDesignTokens.Spacing.xxl,
            HomaDesignTokens.Spacing.xxxl,
        )
        assertTrue(values.zipWithNext().all { (a, b) -> a <= b })
    }

    @Test
    fun component_touch_target_meets_material_minimum() {
        assertEquals(48f, HomaDesignTokens.Component.minTouchTarget.value, 0f)
    }

    @Test
    fun motion_tokens_are_positive() {
        assertTrue(HomaDesignTokens.Motion.fastMillis > 0)
        assertTrue(HomaDesignTokens.Motion.standardMillis >= HomaDesignTokens.Motion.fastMillis)
    }
}
