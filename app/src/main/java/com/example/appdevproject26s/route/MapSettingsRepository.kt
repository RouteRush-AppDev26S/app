package com.example.appdevproject26s.route

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.spatialk.geojson.Position

private val LATITUDE = doublePreferencesKey("camera_lat")
private val LONGITUDE = doublePreferencesKey("camera_lng")
private val ZOOM = doublePreferencesKey("camera_zoom")
private val BEARING = doublePreferencesKey("camera_bearing")

val Context.dataStore by preferencesDataStore(name = "settings")

class MapSettingsRepository(private val dataStore: DataStore<Preferences>) {

    val savedPosition: Flow<CameraPosition?> = dataStore.data.map { prefs ->
        val lat = prefs[LATITUDE]
        val lng = prefs[LONGITUDE]
        val zoom = prefs[ZOOM]
        val bearing = prefs[BEARING]

        if (lat != null && lng != null && zoom != null && bearing != null) {
            CameraPosition(
                target = Position(lng, lat), zoom = zoom,
                bearing = bearing,
            )
        } else null

    }

    suspend fun saveCameraPosition(position: CameraPosition) {
        dataStore.edit { prefs ->
            prefs[LATITUDE] = position.target.latitude
            prefs[LONGITUDE] = position.target.longitude
            prefs[ZOOM] = position.zoom
            prefs[BEARING] = position.bearing
        }
    }
}