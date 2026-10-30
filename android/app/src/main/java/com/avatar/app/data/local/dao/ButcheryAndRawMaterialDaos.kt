package com.avatar.app.data.local.dao

import androidx.room.*
import com.avatar.app.data.local.entity.ButcheryOperationEntity
import com.avatar.app.data.local.entity.RawMaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ButcheryDao {
    @Query("SELECT * FROM butchery_operations ORDER BY operationDate DESC, id DESC")
    fun getAllOperations(): Flow<List<ButcheryOperationEntity>>

    @Query("SELECT * FROM butchery_operations ORDER BY operationDate DESC, id DESC")
    suspend fun getAllOperationsList(): List<ButcheryOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperations(ops: List<ButcheryOperationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(op: ButcheryOperationEntity)

    @Query("DELETE FROM butchery_operations")
    suspend fun clearOperations()
}

@Dao
interface RawMaterialDao {
    @Query("SELECT * FROM raw_materials ORDER BY id DESC")
    fun getAllMaterials(): Flow<List<RawMaterialEntity>>

    @Query("SELECT * FROM raw_materials ORDER BY id DESC")
    suspend fun getAllMaterialsList(): List<RawMaterialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(materials: List<RawMaterialEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: RawMaterialEntity)

    @Query("DELETE FROM raw_materials WHERE id = :id")
    suspend fun deleteMaterial(id: Int)

    @Query("DELETE FROM raw_materials")
    suspend fun clearMaterials()
}
