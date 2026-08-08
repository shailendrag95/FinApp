package com.android.skg.finapp

import android.app.Application
import com.android.skg.finapp.di.AppContainer

class FinAppApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

fun Application.appContainer(): AppContainer =
    (this as FinAppApplication).container
