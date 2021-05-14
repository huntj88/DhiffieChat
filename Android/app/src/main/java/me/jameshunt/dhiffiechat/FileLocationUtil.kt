package me.jameshunt.dhiffiechat

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

class FileLocationUtil(context: Context) {
    private val cacheDir: File = context.cacheDir
    private val fileProviderDir: File = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "DhiffieChat"
    )

    fun outgoingEncryptedFile(): File {
        val file = File(cacheDir, "send")

        // Create the storage directory if it does not exist
        if (!file.exists() && !file.mkdirs()) {
            Log.d("DhiffieChat", "failed to create directory")
        }

        return File(file, "encrypted")
    }

    fun incomingDecryptedFile(): File {
        val file = File(cacheDir, "receive")

        // Create the storage directory if it does not exist
        if (!file.exists() && !file.mkdirs()) {
            Log.d("DhiffieChat", "failed to create directory")
        }

        return File(file, "decrypted")
    }

    fun getInputFile(): File {
        // Create the storage directory if it does not exist
        if (!fileProviderDir.exists() && !fileProviderDir.mkdirs()) {
            Log.d("DhiffieChat", "failed to create directory")
        }

        return File(fileProviderDir.path, "temp")
    }
}