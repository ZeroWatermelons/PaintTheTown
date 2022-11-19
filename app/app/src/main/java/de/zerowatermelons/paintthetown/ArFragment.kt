package de.zerowatermelons.paintthetown

import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.gorisse.thomas.sceneform.scene.await
import kotlinx.coroutines.future.await

class ArFragment : Fragment(R.layout.fragment_ar) {

    private lateinit var arFragment: ArFragment
    private val arSceneView get() = arFragment.arSceneView
    private val scene get() = arSceneView.scene

    private var model: Renderable? = null
    private var modelView: ViewRenderable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arFragment = (childFragmentManager.findFragmentById(R.id.arFragment) as ArFragment).apply {
            setOnSessionConfigurationListener { session, config ->
                // Modify the AR session configuration here
            }
            setOnViewCreatedListener { arSceneView ->
                arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL)

            }

            setOnTapArPlaneListener(::onTapPlane)
            arFragment.arSceneView.scene.addOnUpdateListener(::onUpdate)
        }

        lifecycleScope.launchWhenCreated {
            loadModels()
            val session = arFragment.arSceneView.session
            val anchor = session!!.createAnchor(Pose(floatArrayOf(0f,0f,0f),floatArrayOf(0f,0f,0f,1f)))
            AnchorNode(anchor).apply {
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
            }
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
    fun onUpdate(frameTime : FrameTime) {

    }
}