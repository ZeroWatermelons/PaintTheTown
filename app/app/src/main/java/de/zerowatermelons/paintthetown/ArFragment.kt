package de.zerowatermelons.paintthetown

import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.gorisse.thomas.sceneform.rotation
import com.gorisse.thomas.sceneform.scene.await
import kotlinx.coroutines.future.await
import kotlin.random.Random

class ArFragment : Fragment(R.layout.fragment_ar) {

    private lateinit var arFragment: ArFragment
    private val arSceneView get() = arFragment.arSceneView
    private val scene get() = arSceneView.scene
    private var session: Session? = null
    private var anchor: Anchor? = null

    private var model: Renderable? = null
    private var modelView: ViewRenderable? = null

    private var anchorNode: AnchorNode? = null

    private var counter: Int = 0

    private var pizza: BooleanArray = BooleanArray(12)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arFragment = (childFragmentManager.findFragmentById(R.id.arFragment) as ArFragment).apply {
            setOnSessionConfigurationListener { session, config ->
                // Modify the AR session configuration here
            }
            setOnViewCreatedListener { arSceneView ->
                arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL)
                arSceneView.scene.addOnUpdateListener(this@ArFragment::onUpdate)
            }

            //setOnTapArPlaneListener(::onTapPlane)
        }

        lifecycleScope.launchWhenCreated {
            loadModels()
        }
    }

    private suspend fun loadModels() {
        model = ModelRenderable.builder()
            .setSource(context, Uri.parse("models/circle.glb"))
            .setIsFilamentGltf(true)
            .await()
    }

    private fun onTapPlane(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {
        if (model == null) {
            Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the Anchor.
        scene.addChild(AnchorNode(hitResult.createAnchor()).apply {
            // Create the transformable model and add it to the anchor.
            addChild(TransformableNode(arFragment.transformationSystem).apply {
                renderable = model
                renderableInstance.setCulling(false)
                renderableInstance.animate(true).start()

                // Add the View
                addChild(Node().apply {
                    // Define the relative position
                    localPosition = Vector3(0.0f, 1f, 0.0f)
                    localScale = Vector3(0.7f, 0.7f, 0.7f)
                    renderable = modelView
                })
            })
        })
    }

    fun onUpdate(frameTime: FrameTime) {
        if (session == null) {
            session = arSceneView.session
            if (session == null) {
                return
            }
        }
        val session = session!!
        val frame = session.update()
        println(frame.camera.trackingState)
        if (frame.camera.trackingState == TrackingState.TRACKING && anchor == null) {
            anchor =
                session.createAnchor(Pose(floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f, 1f)))
            anchorNode = AnchorNode(anchor).apply {
                // Create the transformable model and add it to the anchor.
                addChild(TransformableNode(arFragment.transformationSystem).apply {
                    renderable = model
                    renderableInstance.setCulling(false)
                    renderableInstance.animate(true).start()

                    localPosition = Vector3(0.0f, -10f, -10.0f)
                    localScale = Vector3(0.7f, 0.7f, 0.7f)
                })
            }
            scene.addChild(anchorNode)
        } else {
            return
        }

        //anchor's initialized from here on out


        if (counter == 0) {
            addChildSplat(Random.nextFloat() * 2 - 1, -1.8f, 5f - Random.nextFloat())
            addChildSplat(Random.nextFloat() * 2 - 3, -1.8f, 5f + Random.nextFloat())
            addChildSplat(Random.nextFloat() * 2 + 1, -1.8f, 5f + Random.nextFloat())

            counter++
        }

        
    }

    fun addChildSplat(x: Float, y: Float, z: Float) {
        // Create the transformable model and add it to the anchor.
        anchorNode!!.addChild(TransformableNode(arFragment.transformationSystem).apply {
            renderable = model
            renderableInstance.setCulling(false)
            renderableInstance.animate(true).start()

            localPosition =
                Vector3(Random.nextFloat() * 2 - 1, -1.8f, -(5f - Random.nextFloat()))
            localScale = Vector3(0.1f, 0.1f, 0.1f)
        })
    }


    fun convertQuaternionToYaw(quaternion: Quaternion): Float {
        return kotlin.math.atan2(
            2 * (quaternion.w * quaternion.z + quaternion.x * quaternion.y),
            1 - 2 * (quaternion.y * quaternion.y + quaternion.z * quaternion.z)
        )
    }
}

