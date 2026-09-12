# Room entities are accessed via generated DAOs through reflection-free codegen,
# but keep model classes that are serialized to be safe against field stripping.
-keep,includedescriptorclasses class com.flick.data.model.**$$serializer { *; }
-keepclassmembers class com.flick.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.flick.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation @kotlinx.serialization.Serializable class com.flick.data.model.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt / Dagger generated code
-dontwarn com.google.errorprone.annotations.**

# These services are named from voice_interaction_service.xml. Android resolves
# them by their original class names before the Assistant role is granted.
-keep class com.flick.trigger.FlickVoiceInteractionService { *; }
-keep class com.flick.trigger.FlickVoiceInteractionSessionService { *; }
-keep class com.flick.trigger.NoOpRecognitionService { *; }
