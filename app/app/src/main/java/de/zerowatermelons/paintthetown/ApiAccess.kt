package de.zerowatermelons.paintthetown

class ApiAccess: IApiAccess {

    override fun getSplashzones(long: Double, lat: Double, radius: Double, callback: (List<IApiAccess.Splashzone>) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun assignSplashzone(osmid: String, team: Team, callback: () -> Unit) {
        TODO("Not yet implemented")
    }


}