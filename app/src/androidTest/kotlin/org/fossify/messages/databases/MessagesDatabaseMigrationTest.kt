package org.fossify.messages.databases

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.fossify.messages.models.BankAccount
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory

@RunWith(AndroidJUnit4::class)
class MessagesDatabaseMigrationTest {
    private val testDbName = "migration-test.db"

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val helper by lazy {
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MessagesDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory()
        )
    }

    @Test
    fun migration_11_12_createsBankAccountsTable() {
        helper.createDatabase(testDbName, 11).use { db ->
            assert(!hasTable(db, "bank_accounts"))
        }

        helper.runMigrationsAndValidate(
            testDbName,
            12,
            true,
            MessagesDatabase.MIGRATION_11_12
        ).use { db ->
            check(hasTable(db, "bank_accounts"))
            check(hasColumn(db, "bank_accounts", "id"))
            check(hasColumn(db, "bank_accounts", "bankId"))
            check(hasColumn(db, "bank_accounts", "cardNumber"))
            check(hasColumn(db, "bank_accounts", "holderName"))
            check(hasColumn(db, "bank_accounts", "iban"))
            check(hasColumn(db, "bank_accounts", "createdAt"))
            check(hasColumn(db, "bank_accounts", "updatedAt"))
        }
    }

    @Test
    fun migration_11_12_preservesExistingCoreTables() {
        helper.createDatabase(testDbName, 11).use { db ->
            check(hasTable(db, "messages"))
            check(hasTable(db, "conversations"))
            check(!hasTable(db, "bank_accounts"))
        }

        helper.runMigrationsAndValidate(
            testDbName,
            12,
            true,
            MessagesDatabase.MIGRATION_11_12
        ).use { db ->
            check(hasTable(db, "messages"))
            check(hasTable(db, "conversations"))
            check(hasTable(db, "bank_accounts"))
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
