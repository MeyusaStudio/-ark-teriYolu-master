package com.avatar.app.data.local.dao

import androidx.room.*
import com.avatar.app.data.local.entity.OfflineButcheryEntity
import com.avatar.app.data.local.entity.OfflineRawMaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineButcheryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(op: OfflineButcheryEntity): Long

    @Query("SELECT * FROM offline_butchery_operations WHERE syncStatus != 'synced' ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<OfflineButcheryEntity>

    @Query("SELECT * FROM offline_butchery_operations ORDER BY createdAt DESC")
    fun getAllOfflineOperations(): Flow<List<OfflineButcheryEntity>>

    @Query("SELECT * FROM offline_butchery_operations WHERE syncStatus != 'synced' ORDER BY createdAt DESC")
    suspend fun getAllOfflineOperationsList(): List<OfflineButcheryEntity>

    @Query("SELECT COUNT(*) FROM offline_butchery_operations WHERE syncStatus != 'synced'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM offline_butchery_operations WHERE syncStatus != 'synced'")
    suspend fun getPendingCount(): Int

    @Query("UPDATE offline_butchery_operations SET syncStatus = :status, serverId = :serverId, syncErrorMessage = :errorMsg WHERE localId = :localId")
    suspend fun updateSyncStatus(localId: Long, status: String, serverId: Int?, errorMsg: String? = null)

    @Query("DELETE FROM offline_butchery_operations WHERE localId = :localId")
    suspend fun deleteOperation(localId: Long)

    @Query("DELETE FROM offline_butchery_operations WHERE syncStatus = 'synced'")
    suspend fun deleteSyncedOperations()
}

@Dao
interface OfflineRawMaterialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(mat: OfflineRawMaterialEntity): Long

    @Query("SELECT * FROM offline_raw_materials WHERE syncStatus != 'synced' ORDER BY createdAt ASC")
    suspend fun getPendingMaterials(): List<OfflineRawMaterialEntity>

    @Query("SELECT * FROM offline_raw_materials ORDER BY createdAt DESC")
    fun getAllOfflineMaterials(): Flow<List<OfflineRawMaterialEntity>>

    @Query("SELECT * FROM offline_raw_materials WHERE syncStatus != 'synced' ORDER BY createdAt DESC")
    suspend fun getAllOfflineMaterialsList(): List<OfflineRawMaterialEntity>

    @Query("SELECT COUNT(*) FROM offline_raw_materials WHERE syncStatus != 'synced'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM offline_raw_materials WHERE syncStatus != 'synced'")
    suspend fun getPendingCount(): Int

    @Query("UPDATE offline_raw_materials SET syncStatus = :status, serverId = :serverId, syncErrorMessage = :errorMsg WHERE localId = :localId")
    suspend fun updateSyncStatus(localId: Long, status: String, serverId: Int?, errorMsg: String? = null)

    @Query("DELETE FROM offline_raw_materials WHERE localId = :localId")
    suspend fun deleteMaterial(localId: Long)

    @Query("DELETE FROM offline_raw_materials WHERE syncStatus = 'synced'")
    suspend fun deleteSyncedMaterials()
}
