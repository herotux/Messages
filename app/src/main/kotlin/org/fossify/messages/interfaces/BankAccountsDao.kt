package org.fossify.messages.interfaces

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import org.fossify.messages.models.BankAccount

@Dao
interface BankAccountsDao {
    @Query("SELECT * FROM bank_accounts ORDER BY updatedAt DESC, id DESC")
    fun getAll(): List<BankAccount>

    @Query("SELECT * FROM bank_accounts WHERE id = :id LIMIT 1")
    fun getById(id: Long): BankAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(account: BankAccount): Long

    @Update
    fun update(account: BankAccount)

    @Delete
    fun delete(account: BankAccount)

    @Query("DELETE FROM bank_accounts WHERE id = :id")
    fun deleteById(id: Long)
}
