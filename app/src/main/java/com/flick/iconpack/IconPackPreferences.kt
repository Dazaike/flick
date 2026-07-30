package com.flick.iconpack

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.iconPackDataStore by preferencesDataStore(name = "icon_pack_prefs")
private val ACTIVE_ICON_PACK_KEY = stringPreferencesKey("active_icon_pack_package")

@Singleton
class IconPackPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val activePackPackage: Flow<String?> = context.iconPackDataStore.data.map { it[ACTIVE_ICON_PACK_KEY] }

    suspend fun setActivePack(packageName: String?) {
        context.iconPackDataStore.edit { prefs ->
            if (packageName == null) prefs.remove(ACTIVE_ICON_PACK_KEY) else prefs[ACTIVE_ICON_PACK_KEY] = packageName
        }
    }

    suspend fun getActivePack(): String? = activePackPackage.first()
}
