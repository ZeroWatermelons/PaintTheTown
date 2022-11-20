package de.zerowatermelons.paintthetown

import android.content.res.AssetManager
import android.graphics.Color
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.random.Random

class ApiAccessMock(assetManager: AssetManager) : IApiAccess {
    private val splashzones: List<IApiAccess.Splatzone>

    init {
        val splatzones = ObjectMapper().readValue(
            assetManager.open("mock-splatmap.json"),
            JsonNode::class.java
        )
        this.splashzones = splatzones.asSequence().mapIndexedNotNull { i, it ->
            val hexColor = String.format("#%06X", 0xFFFFFF and Color.rgb(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()))
            val long = it.get("long").asDouble()
            val lat = it.get("lat").asDouble()
            val osmid = it.get("osmid").asLong()
            IApiAccess.Splatzone(
                i.toLong(),
                IApiAccess.User(0, "jören", IApiAccess.Team(0, "#330000")),
                long,
                lat,
                osmid,
            )
        }.toList()
    }

    override fun getSplatzones(long: Double, lat: Double, radius: Double, callback: (List<IApiAccess.Splatzone>) -> Unit){
        callback(splashzones)
    }

    override fun uploadImageData(
        userID: Long,
        zoneID: Long,
        width: Int,
        height: Int,
        type: String,
        data64: String
    ) {
        //EMPTY
    }

    override fun getTeams() : Array<IApiAccess.Team>{
        return arrayOf()
    }
    override fun getUsers() : Array<IApiAccess.User>{
        return arrayOf()
    }
    override fun getUser(name: String) : IApiAccess.User{
        return IApiAccess.User(0, "jören", IApiAccess.Team(0, "#330000"))
    }

}