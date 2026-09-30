package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.DailyCashRegisterEntity
import com.example.data.local.entities.InventoryItemEntity
import com.example.data.local.entities.KhataEntryEntity
import com.example.data.local.entities.KhataPartyEntity
import com.example.data.local.entities.ShopInvoiceEntity
import com.example.data.local.entities.ShopProfileEntity
import com.example.data.local.entities.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {

    // ---------------- Shop Profile ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: ShopProfileEntity): Long

    @Query("SELECT * FROM shop_profiles WHERE userId = :userId LIMIT 1")
    fun getProfileFlow(userId: Long): Flow<ShopProfileEntity?>

    @Query("SELECT * FROM shop_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getProfile(userId: Long): ShopProfileEntity?

    // ---------------- Khata Parties (Customers & Suppliers) ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: KhataPartyEntity): Long

    @Update
    suspend fun updateParty(party: KhataPartyEntity)

    @Delete
    suspend fun deleteParty(party: KhataPartyEntity)

    @Query("SELECT * FROM khata_parties WHERE userId = :userId ORDER BY lastTransactionAt DESC")
    fun getAllParties(userId: Long): Flow<List<KhataPartyEntity>>

    @Query("SELECT * FROM khata_parties WHERE userId = :userId AND type = :type ORDER BY lastTransactionAt DESC")
    fun getPartiesByType(userId: Long, type: String): Flow<List<KhataPartyEntity>>

    @Query("SELECT * FROM khata_parties WHERE id = :partyId LIMIT 1")
    suspend fun getPartyById(partyId: Long): KhataPartyEntity?

    @Query("UPDATE khata_parties SET currentBalance = currentBalance + :deltaAmount, lastTransactionAt = :timestamp WHERE id = :partyId")
    suspend fun updatePartyBalance(partyId: Long, deltaAmount: Double, timestamp: Long = System.currentTimeMillis())

    // ---------------- Khata Entries (Ledger Records) ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhataEntry(entry: KhataEntryEntity): Long

    @Update
    suspend fun updateKhataEntry(entry: KhataEntryEntity)

    @Delete
    suspend fun deleteKhataEntry(entry: KhataEntryEntity)

    @Query("SELECT * FROM khata_entries WHERE partyId = :partyId ORDER BY date DESC")
    fun getEntriesForParty(partyId: Long): Flow<List<KhataEntryEntity>>

    @Query("SELECT * FROM khata_entries WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecentEntries(userId: Long, limit: Int = 30): Flow<List<KhataEntryEntity>>

    @Query("SELECT * FROM khata_entries WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getEntriesBetweenDates(userId: Long, startDate: Long, endDate: Long): Flow<List<KhataEntryEntity>>

    // ---------------- Inventory & Stock ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItemEntity): Long

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    @Delete
    suspend fun deleteItem(item: InventoryItemEntity)

    @Query("SELECT * FROM inventory_items WHERE userId = :userId ORDER BY name ASC")
    fun getAllItems(userId: Long): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: Long): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND currentStock <= minStockAlert ORDER BY currentStock ASC")
    fun getLowStockItems(userId: Long): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%')")
    fun searchItems(userId: Long, query: String): Flow<List<InventoryItemEntity>>

    @Query("UPDATE inventory_items SET currentStock = currentStock + :deltaQty, updatedAt = :timestamp WHERE id = :itemId")
    suspend fun updateItemStock(itemId: Long, deltaQty: Double, timestamp: Long = System.currentTimeMillis())

    // ---------------- Stock Movements ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity): Long

    @Query("SELECT * FROM stock_movements WHERE itemId = :itemId ORDER BY date DESC")
    fun getMovementsForItem(itemId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecentMovements(userId: Long, limit: Int = 50): Flow<List<StockMovementEntity>>

    // ---------------- Shop Invoices & Bills ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: ShopInvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: ShopInvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: ShopInvoiceEntity)

    @Query("SELECT * FROM shop_invoices WHERE userId = :userId ORDER BY date DESC")
    fun getAllInvoices(userId: Long): Flow<List<ShopInvoiceEntity>>

    @Query("SELECT * FROM shop_invoices WHERE id = :invoiceId LIMIT 1")
    suspend fun getInvoiceById(invoiceId: Long): ShopInvoiceEntity?

    @Query("SELECT * FROM shop_invoices WHERE userId = :userId AND type = :type ORDER BY date DESC")
    fun getInvoicesByType(userId: Long, type: String): Flow<List<ShopInvoiceEntity>>

    // ---------------- Daily Cash Register ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRegister(register: DailyCashRegisterEntity): Long

    @Query("SELECT * FROM daily_cash_registers WHERE userId = :userId ORDER BY date DESC LIMIT 1")
    fun getLatestRegister(userId: Long): Flow<DailyCashRegisterEntity?>

    @Query("SELECT * FROM daily_cash_registers WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getRegistersBetweenDates(userId: Long, startDate: Long, endDate: Long): Flow<List<DailyCashRegisterEntity>>
}
