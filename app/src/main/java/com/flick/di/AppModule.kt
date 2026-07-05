package com.flick.di

import android.content.Context
import androidx.room.Room
import com.flick.data.db.BookmarkDao
import com.flick.data.db.CategoryDao
import com.flick.data.db.FlickDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFlickDatabase(@ApplicationContext context: Context): FlickDatabase =
        Room.databaseBuilder(context, FlickDatabase::class.java, "flick.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideBookmarkDao(database: FlickDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun provideCategoryDao(database: FlickDatabase): CategoryDao = database.categoryDao()
}
