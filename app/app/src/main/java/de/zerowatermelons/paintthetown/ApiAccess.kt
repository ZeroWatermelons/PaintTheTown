package de.zerowatermelons.paintthetown

import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class ApiAccess: IApiAccess {

    private val BASE: String = "karmagiel.de/api"

    private fun<T> makeRequest(endpoint: String) : T?{
        val client = OkHttpClient()
        val req = Request.Builder().url("$BASE/$endpoint").get().build()
        val resp = client.newCall(req).execute()
        val o = JSONObject(resp.body!!.string())
        if(o.has("err"))
            return null

        return o.get("data") as T
    }

    private fun teamFromJson(tjs: JSONObject) : IApiAccess.Team{
        return IApiAccess.Team(tjs.getLong("id"), tjs.getString("color"))
    }

    private fun userFromJson(ujs: JSONObject) : IApiAccess.User{
        return IApiAccess.User(ujs.getLong("id"), ujs.getString("name"), teamFromJson(ujs.getJSONObject("team")))
    }

    override fun getTeams(): Array<IApiAccess.Team> {
        val teams = makeRequest<JSONArray>("teams")
        var kotlinTeams = mutableListOf<IApiAccess.Team>()
        for(i in 0 until teams!!.length()){
            kotlinTeams.add(teamFromJson(teams.getJSONObject(i)))
        }
        return kotlinTeams.toTypedArray()
    }

    override fun getUsers(): Array<IApiAccess.User> {
        val users = makeRequest<JSONArray>("users")
        var kotlinUsers = mutableListOf<IApiAccess.User>()
        for(i in 0 until users!!.length()){
            kotlinUsers.add(userFromJson(users.getJSONObject(i)))
        }
        return kotlinUsers.toTypedArray()
    }

    override fun getUser(name: String): IApiAccess.User {
        return makeRequest<JSONObject>("user?name=$name")?.let { userFromJson(it) }!!
    }

    override fun getSplatzones(
        long: Double,
        lat: Double,
        radius: Double,
        callback: (List<IApiAccess.Splatzone>) -> Unit
    ) {
        val arr = makeRequest<JSONArray>("splatzones?long=$long&lat=$lat&radius=$radius")
        val zones = mutableListOf<IApiAccess.Splatzone>()
        for (i in 0 until arr!!.length()){
            //return {'id': zone[0], 'owner': user, 'long': zone[2], 'lat': zone[3], 'osmid': zone[4]}
            val o = arr.getJSONObject(i)
            zones.add(IApiAccess.Splatzone(o.getLong("id"), userFromJson(o.getJSONObject("owner")), o.getDouble("long"), o.getDouble("lat"), o.getLong("osmid")))
        }

        callback(zones)
    }

    override fun uploadImageData(userID: Long, zoneID: Long, width: Int, height: Int, type: String, data64: String) {
        var o = JSONObject()
        o.put("userID", userID)
        o.put("zoneID", zoneID)
        o.put("width", width)
        o.put("height", height)
        o.put("type", type)
        o.put("data64", data64)
        val client = OkHttpClient()
        val req = Request.Builder().url("$BASE/upload").post(o.toString().toRequestBody()).build()
        client.newCall(req).execute()
    }


}