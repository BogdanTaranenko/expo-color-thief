package expo.modules.colorthief

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class ExpoColorThiefModule : Module() {
    override fun definition() = ModuleDefinition {
        Name("ExpoColorThief")

        AsyncFunction("getColor") { imageUri: String, quality: Int, ignoreWhite: Boolean, promise: Promise ->
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val bitmap = loadBitmap(imageUri)
                    if (bitmap == null) {
                        withContext(Dispatchers.Main) { promise.resolve(null) }
                        return@launch
                    }

                    val color = ColorThief.getColor(bitmap, quality, ignoreWhite)
                    withContext(Dispatchers.Main) {
                        if (color != null) {
                            promise.resolve(colorToMap(color))
                        } else {
                            promise.resolve(null)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { promise.resolve(null) }
                }
            }
        }

        AsyncFunction("getPalette") { imageUri: String, colorCount: Int, quality: Int, ignoreWhite: Boolean, promise: Promise ->
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val bitmap = loadBitmap(imageUri)
                    if (bitmap == null) {
                        withContext(Dispatchers.Main) { promise.resolve(null) }
                        return@launch
                    }

                    val palette = ColorThief.getPalette(bitmap, colorCount, quality, ignoreWhite)
                    withContext(Dispatchers.Main) {
                        if (palette != null) {
                            val colors = palette.map { colorToMap(it) }
                            promise.resolve(colors)
                        } else {
                            promise.resolve(null)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { promise.resolve(null) }
                }
            }
        }
    }

    private fun colorToMap(color: IntArray): Map<String, Any> {
        val hex = String.format("#%02x%02x%02x", color[0], color[1], color[2])
        return mapOf(
            "rgb" to mapOf(
                "r" to color[0],
                "g" to color[1],
                "b" to color[2]
            ),
            "hex" to hex
        )
    }

    private fun loadBitmap(uri: String): Bitmap? {
        return try {
            when {
                uri.startsWith("http://") || uri.startsWith("https://") -> {
                    val url = URL(uri)
                    BitmapFactory.decodeStream(url.openStream())
                }
                uri.startsWith("file://") -> {
                    val path = uri.removePrefix("file://")
                    BitmapFactory.decodeFile(path)
                }
                uri.startsWith("content://") -> {
                    val context = appContext.reactContext ?: return null
                    val inputStream = context.contentResolver.openInputStream(Uri.parse(uri))
                    BitmapFactory.decodeStream(inputStream)
                }
                uri.startsWith("data:image") -> {
                    val base64 = uri.substringAfter(",")
                    val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                else -> {
                    // Try as file path
                    if (File(uri).exists()) {
                        BitmapFactory.decodeFile(uri)
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}