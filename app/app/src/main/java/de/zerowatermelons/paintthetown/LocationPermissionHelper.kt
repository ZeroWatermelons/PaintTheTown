/*
From https://github.com/mapbox/mapbox-maps-android/blob/main/app/src/main/java/com/mapbox/maps/testapp/utils/LocationPermissionHelper.kt
 */
package de.zerowatermelons.paintthetown

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.mapbox.common.location.compat.permissions.PermissionsListener
import com.mapbox.common.location.compat.permissions.PermissionsManager
import java.lang.ref.WeakReference

const val TAG = "LocationPermissionHelper"
class LocationPermissionHelper(val activity: WeakReference<Activity>) {
    private lateinit var permissionsManager: PermissionsManager

    fun checkPermissions(onMapReady: () -> Unit) {
        Log.d(TAG, "checkPermissions")
        if (PermissionsManager.areLocationPermissionsGranted(activity.get())) {
            Log.d(TAG, "checkPermissions: granted")
            onMapReady()
        } else {
            Log.d(TAG, "checkPermissions: not granted")
            permissionsManager = PermissionsManager(object : PermissionsListener {
                override fun onExplanationNeeded(permissionsToExplain: List<String>) {
                    Toast.makeText(
                        activity.get(), "You need to accept location permissions.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        onMapReady()
                    } else {
                        activity.get()?.finish()
                    }
                }
            })
            permissionsManager.requestLocationPermissions(activity.get())
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}