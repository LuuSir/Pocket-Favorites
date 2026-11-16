package com.example.resouretree.data.transfer

import com.example.resouretree.R
import com.example.resouretree.localization.AppText
import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.resouretree.domain.model.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.ensureActive

class DocumentStore(private val resolver: ContentResolver, private val mediaDirectory: File? = null) {
    /** Keep one durable private copy. No dependency on a temporary picker grant after saving. */
    suspend fun importMedia(uri: Uri, type: ContentType): ResourceContent = withContext(Dispatchers.IO) {
        require(type != ContentType.TEXT) { AppText.get(R.string.choose_media_type) }
        val mime = resolver.getType(uri) ?: if (type == ContentType.FILE) "application/octet-stream" else ""
        require(validMime(type, mime)) { AppText.get(R.string.media_type_mismatch) }
        val directory = requireNotNull(mediaDirectory) { AppText.get(R.string.media_storage_missing) }
        check(directory.isDirectory || directory.mkdirs()) { AppText.get(R.string.media_directory_failed) }
        val file = File(directory, UUID.randomUUID().toString())
        try {
            requireNotNull(resolver.openInputStream(uri)) { AppText.get(R.string.selected_read_failed) }.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                }
            }
            ResourceContent(type, path = "media/${file.name}", mimeType = mime)
        } catch (e: Exception) { file.delete(); throw e }
    }
    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = requireNotNull(resolver.openInputStream(uri)) { AppText.get(R.string.selected_open_failed) }
        stream.use {
            val buffer = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            while (true) {
                val size = it.read(chunk)
                if (size == -1) break
                require(buffer.size() + size <= TreeJson.MAX_BYTES) { AppText.get(R.string.import_too_large) }
                buffer.write(chunk, 0, size)
            }
            buffer.toString("UTF-8")
        }
    }
    suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        requireNotNull(resolver.openOutputStream(uri, "wt")) { AppText.get(R.string.destination_write_failed) }
            .bufferedWriter(Charsets.UTF_8).use { it.write(text) }
    }
}
