package com.rudra.everything.core.backup

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.UUID

class GoogleDriveBackupClient {
    suspend fun uploadBackup(
        accessToken: String,
        fileName: String,
        encryptedBackup: String,
    ): DriveUploadResult {
        val folderId = findBackupFolder(accessToken) ?: createBackupFolder(accessToken)
        val fileId = createBackupFile(
            accessToken = accessToken,
            folderId = folderId,
            fileName = fileName,
            encryptedBackup = encryptedBackup,
        )
        return DriveUploadResult(fileId = fileId, fileName = fileName)
    }

    private fun findBackupFolder(accessToken: String): String? {
        val query = "mimeType = '$DRIVE_FOLDER_MIME_TYPE' and name = '$BACKUP_FOLDER_NAME' and trashed = false"
        val fields = "files(id,name)"
        val url = URL(
            "$DRIVE_FILES_URL?q=${query.urlEncoded()}&fields=${fields.urlEncoded()}&pageSize=1",
        )
        val response = url.openDriveConnection(accessToken, "GET").readJson()
        val files = response.optJSONArray("files") ?: JSONArray()
        return files.optJSONObject(0)?.optString("id")?.takeIf(String::isNotBlank)
    }

    private fun createBackupFolder(accessToken: String): String {
        val metadata = JSONObject()
            .put("name", BACKUP_FOLDER_NAME)
            .put("mimeType", DRIVE_FOLDER_MIME_TYPE)
        val response = URL(DRIVE_FILES_URL)
            .openDriveConnection(accessToken, "POST", "application/json; charset=utf-8")
            .writeJson(metadata)
            .readJson()
        return response.getString("id")
    }

    private fun createBackupFile(
        accessToken: String,
        folderId: String,
        fileName: String,
        encryptedBackup: String,
    ): String {
        val boundary = "everything-${UUID.randomUUID()}"
        val metadata = JSONObject()
            .put("name", fileName)
            .put("mimeType", BACKUP_MIME_TYPE)
            .put("parents", JSONArray().put(folderId))

        val connection = URL("$DRIVE_UPLOAD_URL?uploadType=multipart&fields=id")
            .openDriveConnection(accessToken, "POST", "multipart/related; boundary=$boundary")
        connection.doOutput = true
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.append("--").append(boundary).append("\r\n")
            writer.append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            writer.append(metadata.toString()).append("\r\n")
            writer.append("--").append(boundary).append("\r\n")
            writer.append("Content-Type: ").append(BACKUP_MIME_TYPE).append("\r\n\r\n")
            writer.append(encryptedBackup).append("\r\n")
            writer.append("--").append(boundary).append("--")
        }
        return connection.readJson().getString("id")
    }

    private fun URL.openDriveConnection(
        accessToken: String,
        method: String,
        contentType: String? = null,
    ): HttpURLConnection {
        return (openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            contentType?.let { setRequestProperty("Content-Type", it) }
        }
    }

    private fun HttpURLConnection.writeJson(json: JSONObject): HttpURLConnection {
        doOutput = true
        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
            writer.write(json.toString())
        }
        return this
    }

    private fun HttpURLConnection.readJson(): JSONObject {
        val code = responseCode
        val body = if (code in 200..299) {
            inputStream.bufferedReader().use { it.readText() }
        } else {
            errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        if (code !in 200..299) {
            error("Drive request failed ($code): ${body.ifBlank { responseMessage }}")
        }
        return JSONObject(body)
    }

    private fun String.urlEncoded(): String = URLEncoder.encode(this, "UTF-8")

    private companion object {
        const val BACKUP_FOLDER_NAME = "Everything Backups"
        const val BACKUP_MIME_TYPE = "application/vnd.everything.backup+json"
        const val DRIVE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
    }
}

data class DriveUploadResult(
    val fileId: String,
    val fileName: String,
)
