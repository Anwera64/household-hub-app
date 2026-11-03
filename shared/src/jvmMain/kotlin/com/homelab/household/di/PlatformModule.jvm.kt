package com.homelab.household.di

import com.homelab.household.data.datasource.local.FileSessionStorage
import com.homelab.household.data.datasource.local.StoredSessionLocalDataSource
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences

actual val platformModule: Module =
    module {
        single<HttpClientEngine> { CIO.create() }
        single<StoredSessionLocalDataSource> { FileSessionStorage() }
        single<Settings> { PreferencesSettings(Preferences.userRoot().node("household-hub")) }
    }
