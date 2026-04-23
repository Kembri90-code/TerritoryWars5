package com.territorywars.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TerritoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun territoryDao(): TerritoryDao
}
