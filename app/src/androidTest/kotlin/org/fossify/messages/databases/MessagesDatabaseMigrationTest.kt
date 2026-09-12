package org.fossify.messages.databases

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessagesDatabaseMigrationTest {
    private val testDbName = "migration-test.db"

    private val helper by lazy {
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MessagesDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory()
        )
    }

    @Test
    fun migrations_1_to_12_validateAsCompleteUpgradePath() {
        helper.createDatabase(testDbName, 1).use { db ->
            // Version 1 predates the tracked migrations. Seed its legacy
            // conversations table so the 2 -> 3 migration can be exercised.
            db.execSQL(
                "CREATE TABLE `conversations` (" +
                    "`thread_id` INTEGER NOT NULL PRIMARY KEY, " +
                    "`snippet` TEXT NOT NULL, " +
                    "`date` INTEGER NOT NULL, " +
                    "`read` INTEGER NOT NULL, " +
                    "`title` TEXT NOT NULL, " +
                    "`photo_uri` TEXT NOT NULL, " +
                    "`is_group_conversation` INTEGER NOT NULL, " +
                    "`phone_number` TEXT NOT NULL)"
            )
        }

        helper.runMigrationsAndValidate(
            testDbName,
            12,
            true,
            MessagesDatabase.MIGRATION_1_2,
            MessagesDatabase.MIGRATION_2_3,
            MessagesDatabase.MIGRATION_3_4,
            MessagesDatabase.MIGRATION_4_5,
            MessagesDatabase.MIGRATION_5_6,
            MessagesDatabase.MIGRATION_6_7,
            MessagesDatabase.MIGRATION_7_8,
            MessagesDatabase.MIGRATION_8_9,
            MessagesDatabase.MIGRATION_9_10,
            MessagesDatabase.MIGRATION_10_11,
            MessagesDatabase.MIGRATION_11_12
        ).use { db ->
            check(hasTable(db, "messages"))
            check(hasTable(db, "conversations"))
            check(hasTable(db, "drafts"))
            check(hasTable(db, "recycle_bin_messages"))
            check(hasTable(db, "bank_accounts"))
            check(hasColumn(db, "bank_accounts", "cardNumber"))
            check(hasColumn(db, "bank_accounts", "iban"))
        }
    }

    private fun hasTable(db: SupportSQLiteDatabase, tableName: String): Boolean {
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun hasColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
        db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == columnName) return true
            }
        }
        return false
    }
}
