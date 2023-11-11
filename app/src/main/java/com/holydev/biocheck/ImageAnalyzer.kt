package com.holydev.biocheck

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.system.measureTimeMillis


class ImageAnalyzer(mainActivity: MainActivity) : ImageAnalysis.Analyzer {
    val ctx = mainActivity

    @RequiresApi(Build.VERSION_CODES.Q)
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val time = measureTimeMillis {
            val mediaImage = imageProxy.image

            if (mediaImage != null) {
                val buffer = mediaImage.planes!![0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.position(0);
                buffer.get(bytes)
                val bitmapImage =
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
//                val new_bitmap = BITMAP_RESIZER(bitmapImage, 720, 1280)
                val mutableBitmap: Bitmap =
                    bitmapImage.copy(Bitmap.Config.ARGB_8888, true)

                val image = InputImage.fromBitmap(mutableBitmap, 0)
                // Pass image to an ML Kit Vision API
                val defaultDetector = FaceMeshDetection.getClient(
                    FaceMeshDetectorOptions.Builder()
                        .setUseCase(FaceMeshDetectorOptions.FACE_MESH)
                        .build()
                )

                val result = defaultDetector.process(image)
                    .addOnSuccessListener { result ->


                        val canvas = Canvas(mutableBitmap)
                        val p = Paint()
                        p.style = Paint.Style.FILL_AND_STROKE
                        p.isAntiAlias = true
                        p.isFilterBitmap = true
                        p.isDither = true
                        p.color = Color.GREEN
                        for (faceMesh in result) {
                            //  val bounds: Rect = faceMesh.boundingBox
                            // Gets all points
                            for (faceMeshpoint in faceMesh.allPoints) {
                                val position = faceMeshpoint.position
                                canvas.drawCircle(position.x, position.y, 3f, p)
                            }
                            // Gets triangle info
                            /*val triangles: List<Triangle<FaceMeshPoint>> = faceMesh.allTriangles
                            for (triangle in triangles) {
                                // 3 Points connecting to each other and representing a triangle area.
                                val connectedPoints = triangle.allPoints
                            }*/
                        }
                        canvas.drawBitmap(mutableBitmap, 0f, 0f, p)
                        saveMediaToStorage(mutableBitmap)

                    }
                    .addOnFailureListener { e ->
                        e.message?.let { Log.e("mesh-1", it) }
                    }
            }
        }
        Log.d("timer", time.toString())
    }

    fun saveMediaToStorage(bitmap: Bitmap) {
        //Generating a file name
        val filename = "${System.currentTimeMillis()}.jpg"

        //Output stream
        var fos: OutputStream? = null

        //For devices running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //getting the contentResolver
            ctx.contentResolver?.also { resolver ->

                //Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {

                    //putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                //Inserting the contentValues to contentResolver and getting the Uri
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                //Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            //These for devices running on android < Q
            //So I don't think an explanation is needed here
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            //Finally writing the bitmap to the output stream that we opened
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
//            ctx?.toast("Saved to Photos")
        }
    }

    fun BITMAP_RESIZER(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val ratioX = newWidth / bitmap.width.toFloat()
        val ratioY = newHeight / bitmap.height.toFloat()
        val middleX = newWidth / 2.0f
        val middleY = newHeight / 2.0f
        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)
        val canvas = Canvas(scaledBitmap)
        canvas.setMatrix(scaleMatrix)
        canvas.drawBitmap(
            bitmap,
            middleX - bitmap.width / 2,
            middleY - bitmap.height / 2,
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        return scaledBitmap
    }
}