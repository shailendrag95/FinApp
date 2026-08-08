package com.android.skg.finapp.ui

import androidx.compose.runtime.compositionLocalOf
import com.android.skg.finapp.di.AppContainer

val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
