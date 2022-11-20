package de.zerowatermelons.paintthetown

import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.SectionIndexer
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.filament.TextureSampler
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

const val STATUS = "winStatus"

class ArFragment : Fragment(R.layout.fragment_ar) {



    private lateinit var arFragment: ArFragment
    private val arSceneView get() = arFragment.arSceneView
    private val scene get() = arSceneView.scene
    private var session: Session? = null
    private var anchor: Anchor? = null

    private var model: Renderable? = null
    private var sModel: Renderable? = null
    private var modelView: ViewRenderable? = null

    private var anchorNode: AnchorNode? = null

    private var SECTIONS = 16
    private var SECTION_SIZE = 2*kotlin.math.PI / SECTIONS
    private var pizza: BooleanArray = BooleanArray(SECTIONS)
    private var lastSlice: Int = -1

    private var n: TransformableNode? = null

    inner class Bullet(var speed: Vector3) {

        fun update(dt: Float){
            this.speed.y -= 9.81f * dt
            if(this.speed.y < 0 && this.node.localPosition.y < -4f) {
                this.done = true
                this.node.localPosition = Vector3(node.localPosition.x, -4f, node.localPosition.z)
                anchorNode!!.removeChild(this.node)
            }

            this.node.localPosition = Vector3(this.node.localPosition.x + this.speed.x * dt, this.node.localPosition.y + this.speed.y * dt, this.node.localPosition.z + this.speed.z * dt)

        }

        lateinit var node : TransformableNode
        var done : Boolean = false

        init {
            this.node = TransformableNode(arFragment.transformationSystem).apply {
                renderable = sModel
                renderableInstance.setCulling(false)
                renderableInstance.animate(true).start()

                val len = kotlin.math.sqrt(speed.x * speed.x + speed.z * speed.z)
                var nx = speed.x / len
                var nz = speed.z / len

                localPosition = Vector3(nx, 0.0f , nz)
                scaleController.minScale=0.001f
                scaleController.maxScale=999f
                var randomFloat: Float = Random.nextFloat() * 0.4f;
                localScale = Vector3(randomFloat, randomFloat,  randomFloat)
            }
            anchorNode!!.addChild(node)
            this.node.select()
            var mis = node.renderableInstance.filamentAsset!!.materialInstances
            var ts = TextureSampler()
            for (mi in mis) {
                mi.setColorWrite(true)
                mi.setParameter("baseColorFactor", 0.0f, 0.0f, 1.0f)
            }
        }
    }

    private var bullets = mutableListOf<Bullet>()

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
        sModel = ModelRenderable.builder()
            .setSource(context, Uri.parse("models/sphere.glb"))
            .setIsFilamentGltf(true)
            .await()
    }

    private fun onTapPlane(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {
        if (model == null || sModel==null) {
            Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show()
            return
        }
        return

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
private var cnt : Int = 0
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
        }
        if(anchor == null)
            return;
        //anchor's initialized from here on out


        val rot = convertQuaternionToYaw(frame.camera.displayOrientedPose.rotation) + Math.PI;
        var index = (rot / SECTION_SIZE).toInt()
        println("DDDD index: " + index + ",  rot: " + rot)
        if (index < 0)
            index = 0
        if (index >= SECTIONS)
            index = SECTIONS-1

        if(lastSlice == -1)
            lastSlice = index

        fireShotForRotation(index)

        val r1 = (SECTIONS - kotlin.math.max(index, lastSlice)) + kotlin.math.min(index, lastSlice)
        val r2 = kotlin.math.max(index, lastSlice) - kotlin.math.min(index, lastSlice)
        var dsum = 0;
        if(r1 < r2){
            for( i in  kotlin.math.max(index, lastSlice)  until SECTIONS){
                fireShotForRotation(i)
                dsum++
            }
            for(i in 0.. kotlin.math.min(index, lastSlice)){
                fireShotForRotation(i)
                dsum++
            }
        }else{
            for(i in kotlin.math.min(index, lastSlice)..kotlin.math.max(index, lastSlice)){
                fireShotForRotation(i)
                dsum++
            }
        }




        lastSlice = index

        //rot = Math.max(0.0, Math.min(2*Math.PI, rot));

        val bit = bullets.iterator()
        while(bit.hasNext()){
            val b = bit.next()
            b.update(frameTime.deltaSeconds)
            //println("CCCC " + b.)
            if(b.done) {
                genSplatFromBullet(b)
                bit.remove()
            }
        }


        // BULLSHIT
        if(cnt == 0){
            n = TransformableNode(arFragment.transformationSystem).apply {
                renderable = sModel
                renderableInstance.animate(true).start()

                localPosition= Vector3(0f,0f,0f)
                scaleController.minScale=0.001f
                scaleController.maxScale=999f
                var randomFloat: Float = Random.nextFloat() * 0.4f;
                localScale = Vector3(0.25f, 0.25f, 0.25f)
            }
            anchorNode!!.addChild(n)
            cnt++
        }

        n?.select()
        n?.localPosition = Vector3(translateX(0f, 1f, rot.toFloat()), 0.5f, translateZ(0f, 1f, rot.toFloat()))

        println("EEEE" + n?.localPosition?.x + ", " + n?.localPosition?.z)

        //check if finished
        finishCheck()
    }

    fun finishCheck(){
        for (slice in pizza) {
            if(!slice)
                return
        }
        //finish
        var b = Bundle()
        b.putBoolean(STATUS, true)
        //findNavController().navigate(R.id.action_ArFragment_to_SecondFragment, b)
    }

    fun fireShotForRotation(index: Int){
        if(pizza[index])
            return
        println("CCCC " + pizza.asList())
        pizza[index] = true;
        val rot = index * SECTION_SIZE + 0.5f * SECTION_SIZE
        val angle = rot.toFloat()
        for(i in 1..15)
            addChildSplatR(Random.nextDouble(-4.0, 4.0).toFloat(), -1.8f, Random.nextDouble(1.0, 7.0).toFloat(), angle)
    }

    fun addChildSplatR(x: Float, y: Float, z: Float, angle: Float){
        var xnew = translateX(x, z, angle)
        var znew = translateZ(x, z, angle)
        addChildSplat(xnew, y, znew)
    }

    fun translateX(x: Float, z: Float, angle: Float) : Float{
        return x * kotlin.math.cos(angle) + z * -kotlin.math.sin(angle)
    }

    fun translateZ(x: Float, z: Float, angle: Float) : Float{
        return x * kotlin.math.sin(angle) + z * kotlin.math.cos(angle)
    }

    fun addChildSplat(x: Float, y: Float, z: Float) {

        var b = Bullet(Vector3(2.0f*x, Random.nextDouble(3.0, 5.0).toFloat(), 2.0f*z))
        //var b = Bullet(Vector3(0.0f, 5.0f, 50.0f))
        this.bullets.add(b)
    }


    fun genSplatFromBullet(b: Bullet) {
        // Create the transformable model and add it to the anchor.
        var c = TransformableNode(arFragment.transformationSystem).apply {
            renderable = model
            renderableInstance.setCulling(false)
            renderableInstance.animate(true).start()

            localPosition = b.node.localPosition
            scaleController.minScale=0.001f
            scaleController.maxScale=999f
            localScale = Vector3(b.node.localScale.x * 5.0f, 1f, b.node.localScale.z * 5.0f)
        }
        anchorNode!!.addChild(c)
        c.select()
        var mis = c.renderableInstance.filamentAsset!!.materialInstances

        var ts = TextureSampler()
        for (mi in mis) {
            mi.setColorWrite(true)
            mi.setParameter("baseColorFactor", 1.0f, 0.0f, 0.0f)
        }

    }



    fun convertQuaternionToYaw(quaternion: Quaternion): Float {
        return kotlin.math.atan2(
            2 * (quaternion.w * quaternion.z + quaternion.x * quaternion.y),
            1 - 2 * (quaternion.y * quaternion.y + quaternion.z * quaternion.z)
        )
    }
}


/*
*
        if (counter == 0) {
            addChildSplat(Random.nextFloat() * 2 - 1, -1.8f, 5f - Random.nextFloat())
            addChildSplat(Random.nextFloat() * 2 - 3, -1.8f, 5f + Random.nextFloat())
            addChildSplat(Random.nextFloat() * 2 + 1, -1.8f, 5f + Random.nextFloat())

            counter++
        }
*
* */

