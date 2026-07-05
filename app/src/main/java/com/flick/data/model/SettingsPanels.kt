package com.flick.data.model

import android.provider.Settings

data class SettingsPanelOption(val label: String, val action: String)

object SettingsPanels {
    val all: List<SettingsPanelOption> = listOf(
        SettingsPanelOption("Wi-Fi", Settings.Panel.ACTION_WIFI),
        SettingsPanelOption("Internet", Settings.Panel.ACTION_INTERNET_CONNECTIVITY),
        SettingsPanelOption("NFC", Settings.Panel.ACTION_NFC),
        SettingsPanelOption("Volume", Settings.Panel.ACTION_VOLUME),
    )
}
