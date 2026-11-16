package app.penny.data.io

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads and writes user-chosen documents via the Storage Access Framework (no storage permission needed). */
class DocumentIo(private val context: Context) {

    suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw java.io.IOException("Couldn't open the file for writing")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
    }

    suspend fun read(uri: Uri, maxBytes: Int = MAX_BACKUP_BYTES): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw java.io.IOException("Couldn't open the file")
        stream.use { input ->
            val bytes = input.readNBytesCompat(maxBytes + 1)
            if (bytes.size > maxBytes) throw java.io.IOException("This file is too large to be a Penny backup")
            String(bytes, Charsets.UTF_8)
        }
    }

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8 * 1024)
        var total = 0
        while (total < limit) {
            val n = read(buf, 0, minOf(buf.size, limit - total))
            if (n < 0) break
            out.write(buf, 0, n)
            total += n
        }
        return out.toByteArray()
    }

    companion object {
        const val MAX_BACKUP_BYTES = 20 * 1024 * 1024
    }
}
