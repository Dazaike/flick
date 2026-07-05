package com.flick.di

import android.content.Context
import com.flick.overlay.widgethost.FlickAppWidgetHost
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WidgetModule {

    @Provides
    @Singleton
    fun provideFlickAppWidgetHost(@ApplicationContext context: Context): FlickAppWidgetHost =
        FlickAppWidgetHost(context)
}
