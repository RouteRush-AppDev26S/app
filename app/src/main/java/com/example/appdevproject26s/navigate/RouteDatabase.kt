package com.example.appdevproject26s.navigate

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase


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

@Database(entities = [RouteEntity::class], version = 1, exportSchema = false)
abstract class RouteDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile
        private var INSTANCE: RouteDatabase? = null

        fun getDatabase(context: Context): RouteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RouteDatabase::class.java,
                    "route_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object RouteConverter {
    private val gson = Gson()

    fun tripToJson(trip: Trip): String = gson.toJson(trip)
    fun jsonToTrip(json: String): Trip = gson.fromJson(json, Trip::class.java)

    fun pointsToJson(points: List<Position>): String = gson.toJson(points)
    fun jsonToPoints(json: String): List<Position> {
        val type = object : TypeToken<List<Position>>() {}.type
        return gson.fromJson(json, type)
    }
}
