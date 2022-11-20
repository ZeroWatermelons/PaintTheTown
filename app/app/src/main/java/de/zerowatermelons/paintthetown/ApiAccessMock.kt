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
    private val splashzones: List<IApiAccess.Splashzone>

    init {
        val splashzones = ObjectMapper().readValue(
            assetManager.open("mock-splatmap.json"),
            JsonNode::class.java
        )
        this.splashzones = splashzones.asSequence().mapIndexedNotNull { i, it ->
            val hexColor = String.format("#%06X", 0xFFFFFF and Color.rgb(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()))
            val long = it.get("long").asDouble()
            val lat = it.get("lat").asDouble()
            val osmid = it.get("osmid").asText()
            IApiAccess.Splashzone(
                i.toLong(),
                hexColor,
                long,
                lat,
                osmid,
            )
        }.toList()
    }

    override fun getSplashzones(
        long: Double,
        lat: Double,
        radius: Double,
        callback: (List<IApiAccess.Splashzone>) -> Unit
    ) {
        callback(splashzones)
    }
}