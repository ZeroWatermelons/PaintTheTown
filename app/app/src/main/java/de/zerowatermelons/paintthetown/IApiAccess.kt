package de.zerowatermelons.paintthetown

interface IApiAccess {

    class Team(val id: Long, val color: String)
    class User(val id: Long, val name: String, val team: Team)
    class Splatzone(val id: Long, val owner: User, val long: Double, val lat: Double, val osmid: Long)


    fun getTeams() : Array<Team>
    fun getUsers() : Array<User>
    fun getUser(name: String) : User


    fun getSplatzones(long: Double, lat: Double, radius: Double, callback: (List<Splatzone>) -> Unit)

    fun uploadImageData(userID: Long, zoneID: Long, width: Int, height: Int, type: String, data64: String)

}