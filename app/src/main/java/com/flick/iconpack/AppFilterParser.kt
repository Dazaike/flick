package com.flick.iconpack

import android.content.Context
import org.xmlpull.v1.XmlPullParser
import javax.inject.Inject
import javax.inject.Singleton

/** Maps a "ComponentInfo{pkg/cls}" key to the icon pack's drawable resource name. */
@Singleton
class AppFilterParser @Inject constructor() {

    fun parse(context: Context, iconPackPackage: String): Map<String, String> {
        return parseFromResources(context, iconPackPackage)
            ?: parseFromAssets(context, iconPackPackage)
            ?: emptyMap()
    }

    private fun parseFromResources(context: Context, iconPackPackage: String): Map<String, String>? {
        return runCatching {
            val res = context.packageManager.getResourcesForApplication(iconPackPackage)
            val resId = res.getIdentifier("appfilter", "xml", iconPackPackage)
            if (resId == 0) return null
            parseXml(res.getXml(resId))
        }.getOrNull()
    }

    private fun parseFromAssets(context: Context, iconPackPackage: String): Map<String, String>? {
        return runCatching {
            val packContext = context.createPackageContext(iconPackPackage, 0)
            packContext.assets.open("appfilter.xml").use { stream ->
                val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(stream, null)
                parseXml(parser)
            }
        }.getOrNull()
    }

    private fun parseXml(parser: XmlPullParser): Map<String, String> {
        val mapping = mutableMapOf<String, String>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (component != null && drawable != null) {
                    mapping[component] = drawable
                }
            }
            eventType = parser.next()
        }
        return mapping
    }
}
