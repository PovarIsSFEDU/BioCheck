package com.holydev.biocheck

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.common.Triangle
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

private const val DOTS = 1

class RealTimeAnalyzer(mainActivity: MainActivity) : ImageAnalysis.Analyzer {
    private var previousMeshPoints: MutableList<FaceMeshPoint>? = null
    private var previousPolygons: List<Triangle<FaceMeshPoint>>? = null
    private var noMicroTickCounter = 0
    private var noRescaleCounter = 0
    private var frameCounter = 0
    private var STARTED_NOW = 0;
    private var analyze_limit = 15;
    private var attack_limit = 8;
    private var attack_limit_pixels = 2;
    private var predictor_count = 0;
    private var parentUI = mainActivity
    private val cls = Classifier(assetFilePath(parentUI, "model.pt"))

    private var algoType = DOTS

    //    private var algoType = POLY


    @RequiresApi(Build.VERSION_CODES.N)
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

                defaultDetector.process(image)
                    .addOnSuccessListener { result ->

                        val p = Paint()
                        p.style = Paint.Style.STROKE
                        p.isAntiAlias = true
                        p.isFilterBitmap = true
                        p.isDither = true
                        p.color = Color.GREEN

                        for (faceMesh in result) {
                            val bounds: Rect = faceMesh.boundingBox

                            val bitmap = Bitmap.createBitmap(
                                image.height,
                                image.width,
                                Bitmap.Config.ARGB_8888
                            )

                            val cropped_bitmap = Bitmap.createBitmap(
                                imageProxy.toBitmap().rotate(-90f),
                                0,
                                0,
                                imageProxy.height,
                                imageProxy.width
                            )

                            val predictResult = cls.predict(cropped_bitmap)
                            val predict = predictResult[1] >= 0.4
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.TRANSPARENT)

                            // Счетчик признаков совпадения двух кадров между собой
                            var localCounter = 0
//                            algoType = parentUI.getModeltype()
                            if (algoType == DOTS) {
                                if (previousMeshPoints == null) {
                                    previousMeshPoints = faceMesh.allPoints
                                    STARTED_NOW = 1
                                }

                                for ((index, newFaceMeshpoint) in faceMesh.allPoints.withIndex()) {
                                    val newPosition = newFaceMeshpoint.position
                                    canvas.drawCircle(newPosition.x, newPosition.y, 2f, p)
                                    val oldPosition = previousMeshPoints?.get(index)
                                    if (newPosition.delta(oldPosition) < attack_limit_pixels) {
                                        localCounter++
                                    }
                                }
                                previousMeshPoints = faceMesh.allPoints
                            } else {
                                // Gets triangle info
                                val polygons: List<Triangle<FaceMeshPoint>> = faceMesh.allTriangles

                                if (previousPolygons == null) {
                                    previousPolygons = faceMesh.allTriangles
                                    STARTED_NOW = 1
                                }

                                for ((index, polygon) in polygons.withIndex()) {
                                    val st_width = p.strokeWidth
                                    Log.e("width", st_width.toString())
                                    p.strokeWidth = 1F
                                    val path = Path()
                                    path.moveTo(
                                        polygon.allPoints[0].position.x,
                                        polygon.allPoints[0].position.y
                                    )
                                    path.lineTo(
                                        polygon.allPoints[1].position.x,
                                        polygon.allPoints[1].position.y
                                    )
                                    path.lineTo(
                                        polygon.allPoints[2].position.x,
                                        polygon.allPoints[2].position.y
                                    )
                                    path.lineTo(
                                        polygon.allPoints[0].position.x,
                                        polygon.allPoints[0].position.y
                                    )
                                    canvas.drawPath(path, p)
//                                    if (abs(1 - polygon.scaleDelta(previousPolygons?.get(index))) <= eps) {
//                                        localCounter++
//                                    }

                                }
                            }
                            canvas.drawBitmap(bitmap, 0f, 0f, p)

                            if (localCounter >= 240) {
                                if (STARTED_NOW == 0) {
                                    STARTED_NOW = 1
                                } else {
                                    noMicroTickCounter++
                                }
                            }

                            if (predict) {
                                predictor_count++
                            }

                            val att_st = noMicroTickCounter * 0.4 + predictor_count * 0.6

                            frameCounter++
                            if (frameCounter == analyze_limit && att_st < attack_limit) {
                                val textView: TextView =
                                    parentUI.findViewById(R.id.attackStatus) as TextView
//                                textView.setText(R.string.no_attack)
                                textView.setText("noa\nnoMTick - ${noMicroTickCounter}\n predCnt - ${predictor_count}\n att_st - ${att_st}")
                                Log.d("Attack_Status", "No Attack")
                                frameCounter = 0
                                noMicroTickCounter = 0
                                predictor_count = 0
                            } else if (frameCounter == analyze_limit && att_st >= attack_limit) {
                                val textView: TextView =
                                    parentUI.findViewById(R.id.attackStatus) as TextView
//                                textView.setText(R.string.attack)
                                textView.setText("att \nnoMTick - ${noMicroTickCounter}\npredCnt - ${predictor_count}\natt_st - ${att_st}")
                                Log.d("Attack_Status", "Attack!!!")
                                frameCounter = 0
                                noMicroTickCounter = 0
                                predictor_count = 0
                            }
                            val mask = parentUI.getMask()
                            if (parentUI.getFlipper()) {
                                mask.setImageBitmap(flip(bitmap))
                            } else {
                                mask.setImageBitmap(bitmap)
                            }
                        }
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        e.message?.let { Log.e("mesh-1", it) }
                    }
            }
        }
        Log.d("timer", time.toString())

    }
}

fun flip(d: Bitmap): Bitmap {
    val m = Matrix()
    m.preScale(-1f, 1f)
    val dst = Bitmap.createBitmap(d, 0, 0, d.width, d.height, m, false)
    dst.density = DisplayMetrics.DENSITY_DEFAULT
    return dst
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}