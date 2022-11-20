package de.zerowatermelons.paintthetown

import android.content.res.AssetManager
import android.graphics.Color
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.geojson.FeatureCollection
import org.geojson.GeoJsonObject
import org.geojson.Point
import kotlin.random.Random

class ApiAccessMock(assetManager: AssetManager) : IApiAccess {
    companion object {
        private var splashzones: MutableList<IApiAccess.Splashzone>?= null
    }

    init {
        if(splashzones == null) {
            val splashzones = ObjectMapper().readValue(
                assetManager.open("mock-splatmap.json"),
                JsonNode::class.java
            )
            ApiAccessMock.splashzones = splashzones.asSequence().mapIndexedNotNull { i, it ->
                val team = Team.values()[Random.nextInt(Team.values().size)]
                val long = it.get("long").asDouble()
                val lat = it.get("lat").asDouble()
                val osmid = it.get("osmid").asText()
                IApiAccess.Splashzone(
                    i.toLong(),
                    team,
                    long,
                    lat,
                    osmid,
                )
            }.toMutableList()
        }
    }

    override fun getSplashzones(
        long: Double,
        lat: Double,
        radius: Double,
        callback: (List<IApiAccess.Splashzone>) -> Unit
    ) {
        callback(splashzones!!)
    }

    override fun assignSplashzone(osmid: String, team: Team, callback: () -> Unit) {
        val idx = splashzones!!.indexOfFirst { it.osmid == osmid }
        val splashzoneData = splashzones!![idx]
        splashzones!![idx] = IApiAccess.Splashzone(
            splashzoneData.id,
            team,
            splashzoneData.long,
            splashzoneData.lat,
            splashzoneData.osmid
        )
        callback()
    }
}