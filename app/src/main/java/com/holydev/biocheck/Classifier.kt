package com.holydev.biocheck

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.util.Arrays
import java.util.stream.IntStream
import kotlin.math.exp


class Classifier(modelPath: String?) {
    var model: Module

    init {
        model = Module.load(modelPath)
    }

    // приведение размера картинки и конвертация ее в тензор
    //    public Tensor preprocess(Bitmap bitmap, int size){
    //        bitmap = Bitmap.createScaledBitmap(bitmap, size, size,false);
    //        return TensorImageUtils.bitmapToFloat32Tensor(bitmap, this.mean, this.std);
    //    }
    fun preprocess(bitmap: Bitmap): Tensor {
        // Здесь не совсем правильное преобразование!!
        var bitmap = bitmap
        var k = 0.0
        if (bitmap.width < bitmap.height) {
            k = 1.0 * IMG_SIZE / bitmap.width
            val newW = IMG_SIZE
            val newH = (k * bitmap.height).toInt()
            bitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, false)
            bitmap = Bitmap.createBitmap(
                bitmap,
                0,
                (bitmap.height - IMG_SIZE) / 2,
                IMG_SIZE,
                IMG_SIZE
            )
        } else {
            k = 1.0 * IMG_SIZE / bitmap.height
            val newW = (k * bitmap.width).toInt()
            val newH = IMG_SIZE
            bitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, false)
            bitmap = Bitmap.createBitmap(
                bitmap,
                bitmap.width - IMG_SIZE,
                0,
                IMG_SIZE,
                IMG_SIZE
            )
        }
        return TensorImageUtils.bitmapToFloat32Tensor(bitmap, mean, std)
    }

    // найти номер максимального элемента в массиве
    fun argMax(inputs: FloatArray): Int {
        var maxIndex = -1
        var maxvalue = 0.0f
        for (i in inputs.indices) {
            if (inputs[i] > maxvalue) {
                maxIndex = i
                maxvalue = inputs[i]
            }
        }
        return maxIndex
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun softmax(input: Double, neuronValues: DoubleArray): Double {
        val total = Arrays.stream(neuronValues).map { a: Double ->
            exp(
                a
            )
        }.sum()
        return exp(input) / total
    }

    // использование НС
    @RequiresApi(Build.VERSION_CODES.N)
    fun predict(bitmap: Bitmap): DoubleArray {
        val tensor: Tensor = preprocess(bitmap)
        val inputs: IValue = IValue.from(tensor)
        val outputs: Tensor = model.forward(inputs).toTensor()
        val scores: FloatArray = outputs.dataAsFloatArray
        val scores_ = IntStream.range(0, scores.size).mapToDouble { i: Int ->
            scores[i].toDouble()
        }.toArray()
        val result = DoubleArray(scores.size)
        for (i in scores.indices) result[i] = softmax(scores[i].toDouble(), scores_)
        return result
    }

    companion object {
        const val IMG_SIZE = 200
        val IMAGENET_CLASSES = arrayOf("NoAttack", "Attack")
        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}