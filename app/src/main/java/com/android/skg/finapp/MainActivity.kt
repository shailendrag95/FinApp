package com.android.skg.finapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.android.skg.finapp.ui.navigation.FinAppRoot
import com.android.skg.finapp.ui.LocalAppContainer
import com.android.skg.finapp.ui.theme.FinAppTheme
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class MainActivity : FragmentActivity() {
    private val container by lazy { (application as FinAppApplication).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme by container.preferencesManager.isDarkTheme.collectAsState(initial = false)
            FinAppTheme(darkTheme = darkTheme, dynamicColor = false) {
                androidx.compose.runtime.CompositionLocalProvider(LocalAppContainer provides container) {
                    FinAppRoot()
                }
            }
        }
        startAutoLockMonitor()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        container.appLockManager.touchActivity()
    }

    private fun startAutoLockMonitor() {
        lifecycleScope.launch {
            container.preferencesManager.autoLockMinutes.collect { minutes ->
                while (true) {
                    kotlinx.coroutines.delay(5.seconds)
                    if (container.appLockManager.shouldAutoLock(minutes)) {
                        container.appLockManager.lock()
                        recreate()
                        break
                    }
                }
            }
        }
    }
}
