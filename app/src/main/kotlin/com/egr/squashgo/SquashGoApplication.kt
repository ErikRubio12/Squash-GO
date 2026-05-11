package com.egr.squashgo

import android.app.Application
import com.egr.squashgo.core.config.SupabaseConfig
import com.egr.squashgo.core.config.android.ConfigBootstrap
import com.egr.squashgo.messaging.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SquashGoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ConfigBootstrap.configure(
            SupabaseConfig(
                url = BuildConfig.SUPABASE_URL,
                anonKey = BuildConfig.SUPABASE_ANON_KEY,
            ),
        )
        NotificationChannels.registerAll(this)
    }
}
