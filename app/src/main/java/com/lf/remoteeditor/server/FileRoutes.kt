package com.lf.remoteeditor.server

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class FileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)

@Serializable
data class FileContent(
    val path: String,
    val content: String
)

@Serializable
data class SaveFileRequest(
    val path: String,
    val content: String
)

@Serializable
data class CreateFileRequest(
    val path: String,
    val content: String = "",
    val isDirectory: Boolean = false
)

@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String? = null
)

fun Route.fileRoutes(rootDirectory: File) {
    
    route("/api") {
        
        // Get list of files
        get("/files") {
            val path = call.request.queryParameters["path"] ?: ""
            val targetDir = if (path.isEmpty()) {
                rootDirectory
            } else {
                File(rootDirectory, path)
            }
            
            if (!targetDir.exists() || !targetDir.isDirectory) {
                call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Directory not found"))
                return@get
            }
            
            val files = withContext(Dispatchers.IO) {
                targetDir.listFiles()?.map { file ->
                    FileInfo(
                        name = file.name,
                        path = file.relativeTo(rootDirectory).path.replace('\\', '/'),
                        isDirectory = file.isDirectory,
                        size = if (file.isFile) file.length() else 0,
                        lastModified = file.lastModified()
                    )
                }?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
            }
            
            call.respond(files)
        }
        
        // Get file content
        get("/file") {
            val filePath = call.request.queryParameters["path"] 
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Path parameter required"))
            
            val file = File(rootDirectory, filePath)
            
            if (!file.exists()) {
                call.respond(HttpStatusCode.NotFound, ApiResponse(false, "File not found"))
                return@get
            }
            
            if (!file.isFile) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Path is not a file"))
                return@get
            }
            
            // Check if file is too large (limit to 10MB)
            if (file.length() > 10 * 1024 * 1024) {
                call.respond(HttpStatusCode.PayloadTooLarge, ApiResponse(false, "File too large"))
                return@get
            }
            
            val content = withContext(Dispatchers.IO) {
                file.readText()
            }
            
            call.respond(FileContent(
                path = file.relativeTo(rootDirectory).path.replace('\\', '/'),
                content = content
            ))
        }
        
        // Save file
        post("/file") {
            val request = call.receive<SaveFileRequest>()
            
            val file = File(rootDirectory, request.path)
            
            // Ensure parent directory exists
            file.parentFile?.mkdirs()
            
            withContext(Dispatchers.IO) {
                file.writeText(request.content)
            }
            
            call.respond(ApiResponse(true, "File saved successfully"))
        }
        
        // Create new file or directory
        post("/file/new") {
            try {
                val request = call.receive<CreateFileRequest>()
                
                val file = File(rootDirectory, request.path)
                
                if (file.exists()) {
                    call.respond(HttpStatusCode.Conflict, ApiResponse(false, "File already exists"))
                    return@post
                }
                
                withContext(Dispatchers.IO) {
                    try {
                        if (request.isDirectory) {
                            val created = file.mkdirs()
                            if (!created && !file.exists()) {
                                throw Exception("Failed to create directory: ${file.absolutePath}")
                            }
                        } else {
                            // Ensure parent directory exists
                            file.parentFile?.let { parent ->
                                if (!parent.exists()) {
                                    val parentCreated = parent.mkdirs()
                                    if (!parentCreated) {
                                        throw Exception("Failed to create parent directory: ${parent.absolutePath}")
                                    }
                                }
                            }
                            
                            // Create the file
                            if (!file.createNewFile()) {
                                throw Exception("Failed to create file: ${file.absolutePath}")
                            }
                            
                            // Write content if provided
                            if (request.content.isNotEmpty()) {
                                file.writeText(request.content)
                            }
                        }
                    } catch (e: Exception) {
                        throw Exception("IO Error: ${e.message}")
                    }
                }
                
                call.respond(ApiResponse(true, "Created successfully"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, 
                    ApiResponse(false, "Failed to create: ${e.message}"))
            }
        }
        
        // Delete file
        delete("/file") {
            val filePath = call.request.queryParameters["path"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Path parameter required"))
            
            val file = File(rootDirectory, filePath)
            
            if (!file.exists()) {
                call.respond(HttpStatusCode.NotFound, ApiResponse(false, "File not found"))
                return@delete
            }
            
            val deleted = withContext(Dispatchers.IO) {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
            
            if (deleted) {
                call.respond(ApiResponse(true, "Deleted successfully"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse(false, "Failed to delete"))
            }
        }
        
        // Rename/Move file
        put("/file/move") {
            val oldPath = call.request.queryParameters["from"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "From parameter required"))
            val newPath = call.request.queryParameters["to"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "To parameter required"))
            
            val oldFile = File(rootDirectory, oldPath)
            val newFile = File(rootDirectory, newPath)
            
            if (!oldFile.exists()) {
                call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Source file not found"))
                return@put
            }
            
            if (newFile.exists()) {
                call.respond(HttpStatusCode.Conflict, ApiResponse(false, "Destination already exists"))
                return@put
            }
            
            // Ensure parent directory exists
            newFile.parentFile?.mkdirs()
            
            val success = withContext(Dispatchers.IO) {
                oldFile.renameTo(newFile)
            }
            
            if (success) {
                call.respond(ApiResponse(true, "Moved successfully"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse(false, "Failed to move file"))
            }
        }
        
        // Search files
        get("/search") {
            val query = call.request.queryParameters["q"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Query parameter required"))
            
            val results = mutableListOf<FileInfo>()
            
            withContext(Dispatchers.IO) {
                searchFiles(rootDirectory, query.lowercase(), results, rootDirectory)
            }
            
            call.respond(results.take(100)) // Limit results to 100
        }
        
        // Upload multiple files
        post("/upload") {
            try {
                val multipart = call.receiveMultipart()
                val uploadedFiles = mutableListOf<String>()
                val failedFiles = mutableListOf<String>()
                
                withContext(Dispatchers.IO) {
                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                val fileName = part.originalFileName
                                if (fileName != null && fileName.isNotEmpty()) {
                                    try {
                                        // Security check: prevent path traversal
                                        val sanitizedFileName = fileName.replace("..", "").replace("/", "_").replace("\\", "_")
                                        val file = File(rootDirectory, sanitizedFileName)
                                        
                                        // Ensure we don't overwrite without confirmation
                                        val finalFile = if (file.exists()) {
                                            val nameWithoutExt = sanitizedFileName.substringBeforeLast(".")
                                            val ext = if (sanitizedFileName.contains(".")) ".${sanitizedFileName.substringAfterLast(".")}" else ""
                                            var counter = 1
                                            var newFile: File
                                            do {
                                                newFile = File(rootDirectory, "${nameWithoutExt}_$counter$ext")
                                                counter++
                                            } while (newFile.exists())
                                            newFile
                                        } else {
                                            file
                                        }
                                        
                                        finalFile.outputStream().use { output ->
                                            part.streamProvider().use { input ->
                                                input.copyTo(output)
                                            }
                                        }
                                        
                                        uploadedFiles.add(finalFile.name)
                                    } catch (e: Exception) {
                                        failedFiles.add(fileName)
                                        println("Failed to upload $fileName: ${e.message}")
                                    }
                                }
                            }
                            else -> {
                                // Ignore other part types
                            }
                        }
                        part.dispose()
                    }
                }
                
                val message = buildString {
                    if (uploadedFiles.isNotEmpty()) {
                        append("Uploaded: ${uploadedFiles.joinToString(", ")}")
                    }
                    if (failedFiles.isNotEmpty()) {
                        if (uploadedFiles.isNotEmpty()) append(". ")
                        append("Failed: ${failedFiles.joinToString(", ")}")
                    }
                }
                
                if (failedFiles.isEmpty()) {
                    call.respond(ApiResponse(true, message.ifEmpty { "No files uploaded" }))
                } else {
                    call.respond(HttpStatusCode.PartialContent, ApiResponse(false, message))
                }
                
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, 
                    ApiResponse(false, "Upload failed: ${e.message}"))
            }
        }
    }
}

private fun searchFiles(directory: File, query: String, results: MutableList<FileInfo>, rootDirectory: File) {
    directory.listFiles()?.forEach { file ->
        if (file.name.lowercase().contains(query)) {
            results.add(
                FileInfo(
                    name = file.name,
                    path = file.relativeTo(rootDirectory).path.replace('\\', '/'),
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0,
                    lastModified = file.lastModified()
                )
            )
        }
        
        if (file.isDirectory && results.size < 100) {
            searchFiles(file, query, results, rootDirectory)
        }
    }
}