/*
*                    Written by Hans Wornik
*           Implementation of the Route Database
 */

package com.example.appdevproject26s.route

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.maplibre.spatialk.geojson.Position

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val startName: String,
    val destinationName: String,
    val tripJson: String,
    val routePointsJson: String
)

@Dao
interface RouteDao {
    @Insert
    suspend fun insertRoute(route: RouteEntity): Long

    @Query("SELECT * FROM routes ORDER BY timestamp DESC")
    suspend fun getAllRoutes(): List<RouteEntity>

    @Query("SELECT * FROM routes ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastRoute(): RouteEntity?

    @Query("DELETE FROM routes")
    suspend fun clearHistory(): Int

    @Query("DELETE FROM routes WHERE id = :routeId")
    suspend fun deleteRouteById(routeId: Int): Int
}

@Database(entities = [RouteEntity::class], version = 2, exportSchema = false)
@TypeConverters(RouteConverter::class)
abstract class RouteDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
}

object RouteConverter {
    private val gson = Gson()

    @TypeConverter
    @JvmStatic
    fun tripToJson(trip: Trip): String = gson.toJson(trip)

    @TypeConverter
    @JvmStatic
    fun jsonToTrip(json: String): Trip? = gson.fromJson(json, Trip::class.java)

    @TypeConverter
    @JvmStatic
    fun pointsToJson(points: List<Position>): String = gson.toJson(points)

    @TypeConverter
    @JvmStatic
    fun jsonToPoints(json: String): List<Position>? {
        val type = object : TypeToken<List<Position>>() {}.type
        return gson.fromJson(json, type)
    }
}
