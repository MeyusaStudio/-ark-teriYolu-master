package com.avatar.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.avatar.app.data.local.dao.ButcheryDao
import com.avatar.app.data.local.dao.LotDao
import com.avatar.app.data.local.dao.RawMaterialDao
import com.avatar.app.data.local.dao.SaleDao
import com.avatar.app.data.local.dao.UserDao
import com.avatar.app.data.local.entity.ButcheryOperationEntity
import com.avatar.app.data.local.entity.LotEntity
import com.avatar.app.data.local.entity.RawMaterialEntity
import com.avatar.app.data.local.entity.SaleEntity
import com.avatar.app.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        LotEntity::class,
        SaleEntity::class,
        ButcheryOperationEntity::class,
        RawMaterialEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun lotDao(): LotDao
    abstract fun saleDao(): SaleDao
    abstract fun butcheryDao(): ButcheryDao
    abstract fun rawMaterialDao(): RawMaterialDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "avatar_local_db.sqlite"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
