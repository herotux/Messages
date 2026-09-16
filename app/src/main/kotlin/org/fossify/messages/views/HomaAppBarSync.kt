package org.fossify.messages.views

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout

/** Keeps CoordinatorLayout's dependent scrolling content in sync after lifecycle/layout changes. */
object HomaAppBarSync {
    fun requestSync(coordinator: CoordinatorLayout, appBar: AppBarLayout) {
        coordinator.post {
            coordinator.requestLayout()
            appBar.requestLayout()
            coordinator.dispatchDependentViewsChanged(appBar)
            coordinator.invalidate()
        }
    }
}
