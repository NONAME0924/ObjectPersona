package com.objectpersona.app.di

import android.content.Context
import androidx.room.Room
import com.objectpersona.app.data.db.AppDatabase
import com.objectpersona.app.data.db.dao.MessageDao
import com.objectpersona.app.data.db.dao.ObjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI Module — 提供 Room Database 與 DAO 實例。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "objectpersona.db"
        ).build()
    }

    @Provides
    fun provideObjectDao(database: AppDatabase): ObjectDao {
        return database.objectDao()
    }

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }
}
