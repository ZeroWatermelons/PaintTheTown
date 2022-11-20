package de.zerowatermelons.paintthetown

import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonObject
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.generated.FillLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.LocationPuck3D
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.slaviboy.graphics.PointD
import com.slaviboy.voronoi.Delaunay
import com.slaviboy.voronoi.Voronoi
import de.zerowatermelons.paintthetown.databinding.FragmentSecondBinding
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.random.Random

const val OSM_ID = "osm_id"
const val OSM_ISSUES = "osm-issues"
const val STYLE = "mapbox://styles/fabiannowak/clanul8lp005d14o33kq78sg5/draft"
/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private lateinit var locationPermissionHelper: LocationPermissionHelper

    private lateinit var mapView: MapView
    private lateinit var fab: FloatingActionButton
    private lateinit var voronoiManager: VoronoiManager
    private lateinit var apiAccess: IApiAccess

    private var trackPosition: Boolean = true
    private var point: Point? = null

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        if (trackPosition) {
            //mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
        }
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        point = it
        if (trackPosition) {
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
            mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
        }
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
            voronoiManager.onMapViewportChanged()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            voronoiManager.onMapViewportChanged()
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    private val onMapClickListener = OnMapClickListener { point ->
        val mapboxMap = mapView.getMapboxMap()
        val screenpoint = mapboxMap.pixelForCoordinate(point)
        val boxFrom = ScreenCoordinate(screenpoint.x - 10, screenpoint.y - 10)
        val boxTo = ScreenCoordinate(screenpoint.x + 10, screenpoint.y + 10)
        mapboxMap.queryRenderedFeatures(
            RenderedQueryGeometry(ScreenBox(boxFrom, boxTo)), RenderedQueryOptions(
                listOf(OSM_ISSUES), Value.valueOf("")
            )
        ) {
            val featureId = it.value?.getOrNull(0)?.feature?.getStringProperty("osm_id")
            if (featureId != null) {
                findNavController().navigate(
                    R.id.action_SecondFragment_to_ArFragment,
                    Bundle().apply {
                        putString(OSM_ID, featureId)
                    })
            }
        }
        false
    }
    private val onVoronoiCalculated = { featureCollection:FeatureCollection ->
        val mapboxMap = mapView.getMapboxMap()
        val geojsonsource = mapboxMap.getStyle()?.getSource("voronoi") as GeoJsonSource
        geojsonsource.featureCollection(featureCollection)
        Unit
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fab = view.findViewById(R.id.fab)
        fab.hide()
        fab.setOnClickListener {
            reenableCameraTracking()
        }
        mapView = view.findViewById(R.id.mapView)

        apiAccess = ApiAccessMock(requireContext().assets)

        locationPermissionHelper = LocationPermissionHelper(WeakReference(this.activity))
        locationPermissionHelper.checkPermissions {
            onMapReady()
        }

    }

    private fun onMapReady() {
        val mapboxMap = mapView.getMapboxMap()
        mapboxMap.setCamera(
            CameraOptions.Builder()
                .zoom(17.0)
                .build()
        )
        mapboxMap.loadStyleUri(
            STYLE
        ) {
            it.addSource(geoJsonSource("voronoi"))
            it.addLayerAbove(
                fillLayer("voronoilayer", "voronoi") {
                    fillOpacity(0.2)
                    fillColor(get("color"))
                }, "road-simple"
            )
            initLocationComponent()
            setupGesturesListener()
        }
        voronoiManager = VoronoiManager()
        voronoiManager.onJsonUpdated = onVoronoiCalculated
        voronoiManager.onMapViewportChanged()
    }

    private fun initLocationComponent() {
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck3D(modelUri = "asset://models/blue.gltf")
            // this.locationPuck = LocationPuck2D(
            //     bearingImage = AppCompatResources.getDrawable(
            //         this@SecondFragment.context!!,
            //         R.drawable.mapbox_user_puck_icon2,
            //     ),
            //     scaleExpression = interpolate {
            //         linear()
            //         zoom()
            //         stop {
            //             literal(0.0)
            //             literal(0.6)
            //         }
            //         stop {
            //             literal(20.0)
            //             literal(1.0)
            //         }
            //     }.toJson()
            // )
            this.pulsingEnabled = true
        }
        locationComponentPlugin.addOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
        locationComponentPlugin.addOnIndicatorBearingChangedListener(
            onIndicatorBearingChangedListener
        )
    }

    private fun setupGesturesListener() {
        mapView.gestures.addOnMoveListener(onMoveListener)
        mapView.gestures.addOnMapClickListener(onMapClickListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun reenableCameraTracking() {
        mapView.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        if (point != null) {
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(point).build())
        }
        fab.hide()
    }

    private fun onCameraTrackingDismissed() {
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        fab.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    inner class VoronoiManager {
        inner class CancelDelayedRunnable(val afterDelay: () -> Unit): Runnable {
            private val obj = Object()
            private var cancelled: Boolean = false
            var running: Boolean = false
            override fun run() {
                synchronized(obj) {
                    obj.wait(500)
                }
                if (cancelled) return
                running=true
                afterDelay()
                running=false
            }
            fun cancelWait() {
                cancelled = true
                synchronized(obj) {
                    obj.notifyAll()
                }
            }
        }
        val EXTEND_BOX = 1.5f
        var voronoi: Voronoi? = null
        var onJsonUpdated: ((FeatureCollection) -> Unit)? = null
        private var runnable: CancelDelayedRunnable? = null
        private var executor: Executor = Executors.newSingleThreadExecutor()

        fun onMapViewportChanged() {
            val screenSize = mapView.getMapboxMap().getSize()
            println("Viewport changed")
            runnable?.cancelWait()
            runnable = CancelDelayedRunnable {
                scheduleUpdatePoints(screenSize)
            }.also {
                executor.execute(it)
            }
        }
        fun scheduleUpdatePoints(screenSize: Size){
            val screenCenter = ScreenCoordinate(
                (screenSize.width/2).toDouble(),
                (screenSize.height/2).toDouble()
            )
            val screenExtendedMin = ScreenCoordinate(screenCenter.x-screenCenter.x*EXTEND_BOX, screenCenter.y-screenCenter.y*EXTEND_BOX)
            val screenExtendedMax = ScreenCoordinate(screenCenter.x+screenCenter.x*EXTEND_BOX, screenCenter.y+screenCenter.y*EXTEND_BOX)
            println(screenExtendedMin)
            println(screenExtendedMax)

//            mapboxMap.queryRenderedFeatures(RenderedQueryGeometry(ScreenBox(screenExtendedMin,screenExtendedMax)), RenderedQueryOptions(
//                listOf(OSM_ISSUES),Value.valueOf("")
//            )) { result ->
//                val list = result.value
//                if(list == null) {
//                    onJsonUpdated?.invoke(FeatureCollection.fromFeatures(emptyArray()))
//                    return@queryRenderedFeatures
//                }
//                println(list)
//                val points = list.mapNotNull {
//                    it.feature.geometry() as? Point
//                }.toList()
//                if (points.isEmpty()) {
//                    onJsonUpdated?.invoke(FeatureCollection.fromFeatures(emptyArray()))
//                    return@queryRenderedFeatures
//                }
            apiAccess.getSplatzones(0.0,0.0,0.0) { result ->
                val points = result.map {
                    Point.fromLngLat(it.long,it.lat)
                }
                val colors = result.map {
                    it.owner.team.color
                }
                val maxlatitude = points.maxOf { it.latitude() }
                val maxlongitude = points.maxOf { it.longitude() }
                val minlatitude = points.minOf { it.latitude() }
                val minlongitude = points.minOf { it.longitude() }
                val centerLat = (maxlatitude + minlatitude) / 2
                val centerLong = (maxlongitude + minlongitude) / 2
                val latspan = maxlatitude - minlatitude
                val longspan = maxlongitude - minlongitude
                val boundsLatMin = centerLat - (latspan / 2) * EXTEND_BOX
                val boundsLatMax = centerLat + (latspan / 2) * EXTEND_BOX
                val boundsLongMin = centerLong - (longspan / 2) * EXTEND_BOX
                val boundsLongMax = centerLong + (longspan / 2) * EXTEND_BOX

                voronoi =
                    Delaunay.from(ArrayList(points.map { PointD(it.longitude(), it.latitude()) }))
                        .voronoi(boundsLongMin, boundsLatMin, boundsLongMax, boundsLatMax)
                getCells()
                //TODO use actual color list (via db access etc)
                val jsoncells = geojsonCells(colors)
                Handler(Looper.getMainLooper()).post {onJsonUpdated?.invoke(jsoncells)}
            }
        }

        fun getCells():ArrayList<ArrayList<Double>>{
            return voronoi?.getCellsCoordinates()?: arrayListOf()
        }

        fun geojsonCells(colors: List<String>): FeatureCollection{
            val cells = getCells()
            val collection = FeatureCollection.fromFeatures(cells.mapIndexed { i, cell ->
                val points = List(cell.size/2) {
                    j -> Point.fromLngLat(cell[j*2],cell[j*2+1])
                }
                val geometry = Polygon.fromLngLats(listOf(points))
                val feature = Feature.fromGeometry(geometry, JsonObject().apply {
                    //TODO handle color list"
                    addProperty("color", colors[i])
                })
                feature
            }.toTypedArray())
            println(collection.toJson())
            return collection
        }
    }
}