package com.example.finapp.ui

import androidx.compose.runtime.compositionLocalOf
import com.example.finapp.di.AppContainer

val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
