package com.territorywars.data.local

import androidx.room.*
import com.territorywars.domain.model.GeoPoint
import com.territorywars.domain.model.Territory
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "territories")
data class TerritoryEntity(
    @PrimaryKey
    val id: String,
    val ownerId: String,
    val ownerUsername: String,
    val areaM2: Double,
    val perimeterM: Double,
    val color: String,
    val capturedAtMs: Long,
    val updatedAtMs: Long,
    val polygonJson: String // JSON array of lat/lng points
)

@Dao
interface TerritoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(territories: List<TerritoryEntity>)

    @Query("SELECT * FROM territories WHERE id = :id")
    suspend fun getById(id: String): TerritoryEntity?

    @Query("SELECT * FROM territories ORDER BY capturedAtMs DESC LIMIT :limit")
    fun getAllFlow(limit: Int): Flow<List<TerritoryEntity>>

    @Query("SELECT * FROM territories WHERE ownerId = :userId ORDER BY capturedAtMs DESC")
    fun getUserTerritoriesFlow(userId: String): Flow<List<TerritoryEntity>>

    @Query("DELETE FROM territories WHERE updatedAtMs < :olderThanMs")
    suspend fun deleteOlderThan(olderThanMs: Long)

    @Query("DELETE FROM territories")
    suspend fun deleteAll()
}

// Extension для конвертации
fun TerritoryEntity.toDomain(): Territory {
    val points = try {
        emptyList<GeoPoint>()
    } catch (e: Exception) {
        emptyList()
    }
    return Territory(
        id = id,
        ownerId = ownerId,
        ownerUsername = ownerUsername,
        ownerColor = color,
        clanId = null,
        clanColor = null,
        polygon = points,
        areaM2 = areaM2,
        perimeterM = perimeterM,
        capturedAt = java.util.Date(capturedAtMs).toString(),
        updatedAt = java.util.Date(updatedAtMs).toString()
    )
}

fun Territory.toEntity(): TerritoryEntity {
    return TerritoryEntity(
        id = id,
        ownerId = ownerId,
        ownerUsername = ownerUsername,
        areaM2 = areaM2,
        perimeterM = perimeterM,
        color = ownerColor,
        capturedAtMs = 0L,
        updatedAtMs = 0L,
        polygonJson = "[]"
    )
}
