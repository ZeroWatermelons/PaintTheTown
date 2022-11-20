package de.zerowatermelons.paintthetown

interface IApiAccess {
    fun getSplashzones(long: Double, lat: Double, radius: Double, callback: (List<Splashzone>) -> Unit)

    class Splashzone(val id: Long, val team: Team, val long: Double, val lat:Double, val osmid:String)

    fun assignSplashzone(osmid: String, team: Team, callback: () -> Unit)
}