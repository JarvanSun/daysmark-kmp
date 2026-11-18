package com.example.daysmark.android.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shares a rendered card: saves it to the app's cache, then opens the system share sheet. The
 * screen renders the card; this only stores and hands it on, and says whether that worked.
 */
internal class CardSharing(private val context: Context) {
    /** `false` when the image couldn't be saved or no app can receive it (logged). */
    suspend fun share(card: Bitmap, title: String): Boolean {
        val file =
            try {
                save(card)
            } catch (failure: IOException) {
                Log.w(TAG, "Couldn't save the card to share", failure)
                return false
            }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                // The share sheet's preview thumbnail and title come from the clip data.
                clipData = ClipData.newRawUri(title, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        return try {
            context.startActivity(Intent.createChooser(send, title))
            true
        } catch (failure: ActivityNotFoundException) {
            Log.w(TAG, "No app can receive the card", failure)
            false
        }
    }

    private suspend fun save(card: Bitmap): File =
        withContext(Dispatchers.IO) {
            val folder = File(context.cacheDir, "shared").apply { mkdirs() }
            File(folder, "daysmark-card.png").apply {
                outputStream().use { stream ->
                    if (!card.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)) {
                        throw IOException("Couldn't encode the card")
                    }
                }
            }
        }

    private companion object {
        const val TAG = "DaysmarkShare"
        const val PNG_QUALITY = 100
    }
}
