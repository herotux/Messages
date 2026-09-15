package org.fossify.commons.compose.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Public semantic surface for Homa UI primitives. Keep appearance in MaterialTheme. */
object HomaDesignSystem {
    object Insets {
        val content: Dp = 16.dp
        val section: Dp = 24.dp
    }

    object Motion {
        const val standardMillis: Int = 300
        const val fastMillis: Int = 150
    }

    object Elevation {
        val card: Dp = 1.dp
        val raised: Dp = 3.dp
    }
}
