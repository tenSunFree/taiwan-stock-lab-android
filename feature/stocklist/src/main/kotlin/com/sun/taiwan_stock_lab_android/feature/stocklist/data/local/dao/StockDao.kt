package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.RefreshMetadataEntity
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.StockEntity
import kotlinx.coroutines.flow.Flow

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface StockDao {
    // Two separate queries rather than one with a dynamic ORDER BY direction bind parameter —
    // SQLite doesn't support parameterizing ASC/DESC directly, and the CASE-expression
    // workaround for it reads worse than just writing both queries out.
    @Query("SELECT * FROM stocks ORDER BY code ASC")
    fun observeAllAscendingPaged(): PagingSource<Int, StockEntity>

    @Query("SELECT * FROM stocks ORDER BY code DESC")
    fun observeAllDescendingPaged(): PagingSource<Int, StockEntity>

    @Query("SELECT * FROM stocks WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): StockEntity?

    // `change` is stored as either SQL NULL or a clean plain-decimal string (see
    // StockEntityMapper.toEntity — the raw TWSE missing-value sentinels like "-" are already
    // normalized to null by TwseNumericParser before a Stock is ever persisted), so CAST(...
    // AS REAL) here is safe: it never has to guess at un-normalized text.
    //
    // COALESCE(..., 0) makes the empty-table case (SUM() over zero rows) explicit in the SQL
    // itself, rather than relying on how the underlying driver or Room happens to map a NULL
    // aggregate result onto a non-nullable Int field.
    @Query(
        "SELECT " +
            "COALESCE(SUM(CASE WHEN change IS NOT NULL AND CAST(change AS REAL) > 0 " +
            "THEN 1 ELSE 0 END), 0) AS advancingCount, " +
            "COALESCE(SUM(CASE WHEN change IS NOT NULL AND CAST(change AS REAL) < 0 " +
            "THEN 1 ELSE 0 END), 0) AS decliningCount, " +
            "COALESCE(SUM(CASE WHEN change IS NULL OR CAST(change AS REAL) = 0 " +
            "THEN 1 ELSE 0 END), 0) AS unchangedCount " +
            "FROM stocks",
    )
    fun observeMarketSummary(): Flow<MarketSummaryRow>

    @Query("SELECT * FROM refresh_metadata WHERE id = :id LIMIT 1")
    fun observeRefreshMetadata(id: Int = RefreshMetadataEntity.SINGLETON_ID): Flow<RefreshMetadataEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stocks: List<StockEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefreshMetadata(metadata: RefreshMetadataEntity)

    @Query("DELETE FROM stocks")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(
        stocks: List<StockEntity>,
        refreshedAt: Long,
    ) {
        clearAll()
        insertAll(stocks)
        insertRefreshMetadata(RefreshMetadataEntity(lastSuccessfulRefreshAt = refreshedAt))
    }
}
