package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ServerEntity::class,
        HetznerAccountEntity::class,
        VpnConfigEntity::class,
        ConnectionLogEntity::class,
        FirewallAppEntity::class,
        DataSaverStatsEntity::class,
        TelcoBundleEntity::class,
        UserWalletEntity::class,
        PricingConfigEntity::class,
        ClientAccountEntity::class,
        TransactionBookkeepingEntity::class,
        PendingOrderEntity::class,
        ProcessedPaymentEntity::class,
        UnresolvedPaymentEntity::class,
        SavedRecipientEntity::class,
        InboundNotificationAuditLogEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vpnDao(): VpnDao
    abstract fun bookkeepingDao(): BookkeepingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                if (INSTANCE != null) return INSTANCE!!
                try {
                    val db = buildDatabase(context)
                    INSTANCE = db
                    db
                } catch (e: Throwable) {
                    android.util.Log.e("AppDatabase", "Database initialization issue, resetting database: ${e.message}")
                    resetDatabase(context)
                    val freshDb = buildDatabase(context)
                    INSTANCE = freshDb
                    freshDb
                }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return try {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "testcheckout.db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .allowMainThreadQueries()
                .build()
            } catch (e: Throwable) {
                android.util.Log.e("AppDatabase", "Database construction fallback triggered: ${e.message}")
                try {
                    context.applicationContext.deleteDatabase("testcheckout.db")
                } catch (_: Throwable) {}
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "testcheckout.db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .allowMainThreadQueries()
                .build()
            }
        }

        fun resetDatabase(context: Context) {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (_: Throwable) {}
                INSTANCE = null
                try {
                    context.applicationContext.deleteDatabase("testcheckout.db")
                } catch (_: Throwable) {}
            }
        }
    }
}
