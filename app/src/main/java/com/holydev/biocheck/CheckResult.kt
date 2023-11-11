package com.holydev.biocheck

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.PointF3D
import com.holydev.biocheck.databinding.ActivityCheckResultBinding


class CheckResult : AppCompatActivity() {

    private val checkerBinding: ActivityCheckResultBinding by lazy {
        ActivityCheckResultBinding.inflate(LayoutInflater.from(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(checkerBinding.root)
        val arguments = intent.extras
        val bitmapImage = BitmapFactory.decodeByteArray(
            intent.getByteArrayExtra("bitmapStream"),
            0,
            intent.getByteArrayExtra("bitmapStream")!!.size
        )
        val meshpoints_x = arguments?.getFloatArray("meshpoints_x")
        val meshpoints_y = arguments?.getFloatArray("meshpoints_y")
        val canvas = Canvas(bitmapImage)
        val p = Paint()
        p.style = Paint.Style.FILL_AND_STROKE
        p.isAntiAlias = true
        p.isFilterBitmap = true
        p.isDither = true
        p.color = Color.GREEN

        if (meshpoints_x != null && meshpoints_y != null) {
            for ((index, point_x) in meshpoints_x.withIndex()) {
                canvas.drawCircle(point_x, meshpoints_y[index], 1f, p)
            }
        }
        canvas.drawBitmap(bitmapImage, 0f, 0f, p)
        checkerBinding.frameCheck.setImageBitmap(bitmapImage)
    }
}