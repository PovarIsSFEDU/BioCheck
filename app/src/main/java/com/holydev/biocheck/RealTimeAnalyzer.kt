package com.holydev.biocheck

import android.content.res.Resources
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import java.util.Vector
import kotlin.system.measureTimeMillis

private fun PointF3D.delta(oldPosition: FaceMeshPoint?): Float {
    return if (oldPosition != null) {
        val vector = Vector<Float>(3)
        vector.add(Math.abs(this.x - oldPosition.position.x))
        vector.add(Math.abs(this.y - oldPosition.position.y))
        vector.add(Math.abs(this.z - oldPosition.position.z))
        vector.sum()
    } else 0f
}

class RealTimeAnalyzer(mainActivity: MainActivity) : ImageAnalysis.Analyzer {
    private var previousMeshPoints: MutableList<FaceMeshPoint>? = null
    private var noMicroTickCounter = 0
    private var frameCounter = 0
    private var STARTED_NOW = 0;
    private var attack_limit = 15;
    private var attack_limit_pixels = 1;
    private var parentUI = mainActivity

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val time = measureTimeMillis {
            val mediaImage = imageProxy.image

            if (mediaImage != null) {
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                // Pass image to an ML Kit Vision API
                val defaultDetector = FaceMeshDetection.getClient(
                    FaceMeshDetectorOptions.Builder()
                        .setUseCase(FaceMeshDetectorOptions.FACE_MESH)
                        .build()
                )

                val result = defaultDetector.process(image)
                    .addOnSuccessListener { result ->
                        for (faceMesh in result) {
                            //  val bounds: Rect = faceMesh.boundingBox
                            // Gets all points
                            if (previousMeshPoints == null) {
                                previousMeshPoints = faceMesh.allPoints
                                STARTED_NOW = 1
                            }
                            var localCounter = 0
                            for ((index, newFaceMeshpoint) in faceMesh.allPoints.withIndex()) {
                                val newPosition = newFaceMeshpoint.position
                                val oldPosition = previousMeshPoints?.get(index)
                                if (newPosition.delta(oldPosition) < attack_limit_pixels) {
                                    localCounter++
                                }
                            }
                            previousMeshPoints = faceMesh.allPoints
                            if (localCounter >= (previousMeshPoints?.size ?: 468) / 2) {
                                if (STARTED_NOW == 0) {
                                    STARTED_NOW = 1
                                } else {
                                    noMicroTickCounter++
                                }
                            }
                            frameCounter++
                            if (frameCounter == attack_limit && noMicroTickCounter < attack_limit) {
                                /*Toast.makeText(
                                    parentUI,
                                    "NoAttack",
                                    Toast.LENGTH_SHORT
                                ).show()*/
                                val textView: TextView =
                                    parentUI.findViewById(R.id.attackStatus) as TextView
                                textView.setText(R.string.no_attack)
                                Log.d("Attack_Status", "No Attack")
                                frameCounter = 0
                                noMicroTickCounter = 0
                            } else if (frameCounter == attack_limit && noMicroTickCounter == attack_limit) {
//                                Toast.makeText(
//                                    parentUI,
//                                    "Attack!!!",
//                                    Toast.LENGTH_SHORT
//                                ).show()
                                val textView: TextView =
                                    parentUI.findViewById(R.id.attackStatus) as TextView
                                textView.setText(R.string.attack)
                                Log.d("Attack_Status", "Attack!!!")
                                frameCounter = 0
                                noMicroTickCounter = 0
                            }
                            // Gets triangle info
                            /*val triangles: List<Triangle<FaceMeshPoint>> = faceMesh.allTriangles
                            for (triangle in triangles) {
                                // 3 Points connecting to each other and representing a triangle area.
                                val connectedPoints = triangle.allPoints
                            }*/
                        }
//                        canvas.drawBitmap(mutableBitmap, 0f, 0f, p)
//                        saveMediaToStorage(mutableBitmap)
                        imageProxy.close()
                        Log.d("Attack_current", frameCounter.toString())
                        Log.d("Attack_current_no_move", noMicroTickCounter.toString())
                    }
                    .addOnFailureListener { e ->
                        e.message?.let { Log.e("mesh-1", it) }
                    }
            }
        }
        Log.d("timer", time.toString())


    }
}
