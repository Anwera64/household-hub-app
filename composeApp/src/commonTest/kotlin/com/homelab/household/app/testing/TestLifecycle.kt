package com.homelab.household.app.testing

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * The app's lifecycle, in the test's hands: so a test can leave the app (for Settings, say) and come
 * back. Composed through [TestApp]; starts resumed, like an app on screen.
 */
class TestLifecycle : LifecycleOwner {
    private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }

    override val lifecycle: Lifecycle get() = registry

    /** The member goes to another app or to Settings, and comes back. */
    fun leavesAndComesBack() {
        registry.currentState = Lifecycle.State.CREATED
        registry.currentState = Lifecycle.State.RESUMED
    }
}
