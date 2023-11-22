package com.holydev.biocheck

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

fun appSettingOpen(context: Context) {
    Toast.makeText(
        context,
        "Go to Setting and Enable All Permission",
        Toast.LENGTH_LONG
    ).show()

    val settingIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    settingIntent.data = Uri.parse("package:${context.packageName}")
    context.startActivity(settingIntent)
}

fun warningPermissionDialog(context: Context, listener: DialogInterface.OnClickListener) {
    MaterialAlertDialogBuilder(context)
        .setMessage("All Permission are Required for this app")
        .setCancelable(false)
        .setPositiveButton("Ok", listener)
        .create()
        .show()
}

fun assetFilePath(context: Context, assetName: String): String? {
    val file = File(context.filesDir, assetName)
    try {
        val `is` = context.assets.open(assetName)
        try {
            val os: OutputStream = FileOutputStream(file)
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while (`is`.read(buffer).also { read = it } != -1) {
                os.write(buffer, 0, read)
            }
            os.flush()
        } catch (e: Exception) {
            Log.e("pytorchandroid", "Error process asset 1 $assetName to file path")
        }
        return file.absolutePath
    } catch (e: IOException) {
        Log.e("pytorchandroid", "Error process asset 2$assetName to file path")
    }
    return null
}