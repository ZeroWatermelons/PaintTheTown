package de.zerowatermelons.paintthetown

import android.content.res.AssetManager
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.random.Random

class ApiAccessMock(assetManager: AssetManager) : IApiAccess {
    companion object {
        private var splatzones: MutableList<IApiAccess.Splatzone>?= null
    }

    init {
        if(splatzones == null) {
            val splashzones = ObjectMapper().readValue(
                assetManager.open("mock-splatmap.json"),
                JsonNode::class.java)
            ApiAccessMock.splatzones = splashzones.asSequence().mapIndexedNotNull { i, it ->
                val team = Team.values()[Random.nextInt(Team.values().size)]
                val long = it.get("long").asDouble()
                val lat = it.get("lat").asDouble()
                val osmid = it.get("osmid").asText()
                IApiAccess.Splatzone(
                    i.toLong(),
                    IApiAccess.User(i.toLong(), "jören", IApiAccess.Team(0, team)),
                    long,
                    lat,
                    osmid,
                )
            }.toMutableList()
        }
    }

    override fun getSplatzones(
        long: Double,
        lat: Double,
        radius: Double,
        callback: (List<IApiAccess.Splatzone>) -> Unit
    ) {
        callback(splatzones!!)
    }

    override fun assignSplashzone(osmid: String, user: IApiAccess.User, callback: () -> Unit) {
        val idx = splatzones!!.indexOfFirst { it.osmid == osmid }
        val splashzoneData = splatzones!![idx]
        splatzones!![idx] = IApiAccess.Splatzone(
            splashzoneData.id,
            user,
            splashzoneData.long,
            splashzoneData.lat,
            splashzoneData.osmid
        )
        callback()
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
        return IApiAccess.User(0, "jören", IApiAccess.Team(0, Team.BLUE))
    }

}