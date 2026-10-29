package com.avatar.app.data.local.dao

import androidx.room.*
import com.avatar.app.data.local.entity.LotEntity
import com.avatar.app.data.local.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LotDao {
    @Query("SELECT * FROM lots ORDER BY id DESC")
    fun getAllLotsFlow(): Flow<List<LotEntity>>

    @Query("SELECT * FROM lots WHERE id = :id")
    suspend fun getLotById(id: Int): LotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLots(lots: List<LotEntity>)

    @Query("UPDATE lots SET quantity = :newQty, status = :newStatus WHERE id = :lotId")
    suspend fun updateLotQuantity(lotId: Int, newQty: Double, newStatus: String)

    @Query("DELETE FROM lots")
    suspend fun clearLots()
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY id DESC")
    fun getAllSalesFlow(): Flow<List<SaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Query("DELETE FROM sales")
    suspend fun clearSales()
}
