package com.egr.squashgo.core.config.android

import com.egr.squashgo.core.config.SupabaseConfig

object ConfigBootstrap {

    private var supabaseConfig: SupabaseConfig? = null

    fun configure(supabaseConfig: SupabaseConfig) {
        this.supabaseConfig = supabaseConfig
    }

    internal fun requireSupabaseConfig(): SupabaseConfig =
        supabaseConfig
            ?: error("ConfigBootstrap.configure() must be called in Application.onCreate()")
}
