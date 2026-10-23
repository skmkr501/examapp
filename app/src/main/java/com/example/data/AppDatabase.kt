package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ResultEntity::class,
        FlashcardEntity::class,
        CustomExamEntity::class,
        UserPrefEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resultDao(): ResultDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun customExamDao(): CustomExamDao
    abstract fun userPrefDao(): UserPrefDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "examsite_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
