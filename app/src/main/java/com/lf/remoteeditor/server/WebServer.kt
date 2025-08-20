package com.lf.remoteeditor.server

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import io.ktor.server.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class WebServer(
    private val port: Int = 8080,
    private val rootDirectory: File,
    private val authEnabled: Boolean = false,
    private val authUsername: String? = null,
    private val authPassword: String? = null
) {
    private var server: NettyApplicationEngine? = null
    
    fun start() {
        server = embeddedServer(Netty, port = port) {
            configureSecurity()
            configureSerialization()
            configureCORS()
            configureRouting()
        }
        server?.start(wait = false)
    }
    
    fun stop() {
        server?.stop(1000, 5000)
        server = null
    }
    
    private fun Application.configureSecurity() {
        if (authEnabled && authUsername != null && authPassword != null) {
            install(Authentication) {
                basic("auth-basic") {
                    realm = "Access to the HTML editor"
                    validate { credentials ->
                        if (credentials.name == authUsername && credentials.password == authPassword) {
                            UserIdPrincipal(credentials.name)
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }
    
    private fun Application.configureSerialization() {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
    
    private fun Application.configureCORS() {
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            anyHost()
        }
    }
    
    private fun Application.configureRouting() {
        routing {
            if (authEnabled) {
                authenticate("auth-basic") {
                    createRoutes()
                }
            } else {
                createRoutes()
            }
        }
    }
    
    private fun Route.createRoutes(): Route {
        // Serve the web editor interface
        get("/") {
            call.respondText(getEditorHtml(), ContentType.Text.Html)
        }
        
        // Serve static resources (CSS, JS)
        get("/static/{file}") {
            val fileName = call.parameters["file"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            when (fileName) {
                "editor.css" -> call.respondText(getEditorCSS(), ContentType.Text.CSS)
                "editor.js" -> call.respondText(getEditorJS(), ContentType.Text.JavaScript)
                "codemirror.css" -> call.respondText(getCodeMirrorCSS(), ContentType.Text.CSS)
                "codemirror.js" -> call.respondText(getCodeMirrorJS(), ContentType.Text.JavaScript)
                else -> call.respond(HttpStatusCode.NotFound)
            }
        }
        
        // Serve files directly from work directory
        get("/work/{...}") {
            val filePath = call.request.path().removePrefix("/work/")
            
            if (filePath.isEmpty()) {
                // Show directory listing for /work/
                showDirectoryListing(call, rootDirectory, "")
                return@get
            }
            
            val file = File(rootDirectory, filePath)
            
            if (!file.exists()) {
                call.respond(HttpStatusCode.NotFound, "File not found: $filePath")
                return@get
            }
            
            // Security check: ensure file is within rootDirectory
            if (!file.canonicalPath.startsWith(rootDirectory.canonicalPath)) {
                call.respond(HttpStatusCode.Forbidden, "Access denied")
                return@get
            }
            
            if (file.isDirectory) {
                showDirectoryListing(call, file, filePath)
            } else {
                serveStaticFile(call, file)
            }
        }
        
        // File API routes
        fileRoutes(rootDirectory)
        
        return this
    }
    
    private fun getEditorHtml(): String = """
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Remote HTML Editor</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/codemirror.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/theme/monokai.min.css">
    <link rel="stylesheet" href="/static/editor.css">
</head>
<body>
    <div id="app">
        <div class="sidebar">
            <div class="sidebar-header">
                <h3>파일 탐색기</h3>
                <button id="new-file-btn" class="btn btn-primary">새 파일</button>
                <button id="upload-btn" class="btn btn-secondary">파일 업로드</button>
                <button id="upload-folder-btn" class="btn btn-secondary">폴더 업로드</button>
                <input type="file" id="file-input" multiple style="display: none;" accept="*/*">
                <input type="file" id="folder-input" webkitdirectory directory multiple style="display: none;">
            </div>
            <div id="file-list" class="file-list"></div>
        </div>
        <div class="editor-container">
            <div class="editor-header">
                <span id="current-file">선택된 파일 없음</span>
                <div class="editor-actions">
                    <button id="save-btn" class="btn btn-success" disabled>저장</button>
                    <button id="delete-btn" class="btn btn-danger" disabled>삭제</button>
                </div>
            </div>
            <textarea id="code-editor"></textarea>
        </div>
    </div>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/codemirror.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/mode/xml/xml.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/mode/javascript/javascript.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/mode/css/css.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/mode/htmlmixed/htmlmixed.min.js"></script>
    <script src="/static/editor.js"></script>
</body>
</html>
    """.trimIndent()
    
    private fun getEditorCSS(): String = """
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    background: #1e1e1e;
    color: #d4d4d4;
    height: 100vh;
    overflow: hidden;
}

#app {
    display: flex;
    height: 100vh;
}

.sidebar {
    width: 280px;
    background: #252526;
    border-right: 1px solid #3e3e42;
    display: flex;
    flex-direction: column;
}

.sidebar-header {
    padding: 15px;
    border-bottom: 1px solid #3e3e42;
}

.sidebar-header h3 {
    margin-bottom: 10px;
    font-size: 14px;
    text-transform: uppercase;
    color: #cccccc;
}

.file-list {
    flex: 1;
    overflow-y: auto;
    padding: 10px;
}

.file-item {
    padding: 8px 12px;
    cursor: pointer;
    border-radius: 4px;
    margin-bottom: 2px;
    transition: background 0.2s;
}

.file-item:hover {
    background: #2a2d2e;
}

.file-item.active {
    background: #094771;
}

.editor-container {
    flex: 1;
    display: flex;
    flex-direction: column;
}

.editor-header {
    padding: 10px 20px;
    background: #2d2d30;
    border-bottom: 1px solid #3e3e42;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

#current-file {
    font-size: 14px;
    color: #cccccc;
}

.editor-actions {
    display: flex;
    gap: 10px;
}

.btn {
    padding: 6px 16px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 13px;
    transition: opacity 0.2s;
}

.btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.btn-primary {
    background: #0e639c;
    color: white;
    margin-bottom: 8px;
    width: 100%;
}

.btn-primary:hover:not(:disabled) {
    background: #1177bb;
}

.btn-secondary {
    background: #5c5c5c;
    color: white;
    margin-bottom: 8px;
    width: 100%;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.btn-secondary:hover:not(:disabled) {
    background: #6c6c6c;
}

.btn-success {
    background: #16825d;
    color: white;
}

.btn-success:hover:not(:disabled) {
    background: #1e9e6e;
}

.btn-danger {
    background: #a1260d;
    color: white;
}

.btn-danger:hover:not(:disabled) {
    background: #c53030;
}

.CodeMirror {
    flex: 1;
    font-size: 14px;
    line-height: 1.5;
}

.modal {
    display: none;
    position: fixed;
    z-index: 1000;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
}

.modal-content {
    background: #2d2d30;
    margin: 15% auto;
    padding: 20px;
    border: 1px solid #3e3e42;
    width: 400px;
    border-radius: 8px;
}

.modal-header {
    margin-bottom: 20px;
}

.modal-header h2 {
    color: #cccccc;
    font-size: 18px;
}

.modal-body input {
    width: 100%;
    padding: 8px;
    background: #1e1e1e;
    border: 1px solid #3e3e42;
    color: #d4d4d4;
    border-radius: 4px;
    margin-bottom: 20px;
}

.modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}
    """.trimIndent()
    
    private fun getEditorJS(): String = """
let editor;
let currentFile = null;

document.addEventListener('DOMContentLoaded', function() {
    // Initialize CodeMirror
    editor = CodeMirror.fromTextArea(document.getElementById('code-editor'), {
        lineNumbers: true,
        theme: 'monokai',
        mode: 'htmlmixed',
        autoCloseTags: true,
        autoCloseBrackets: true,
        lineWrapping: true
    });
    
    // Load file list
    loadFileList();
    
    // Check webkitdirectory support
    checkFolderUploadSupport();
    
    // Event listeners
    document.getElementById('save-btn').addEventListener('click', saveFile);
    document.getElementById('delete-btn').addEventListener('click', deleteFile);
    document.getElementById('new-file-btn').addEventListener('click', newFile);
    document.getElementById('upload-btn').addEventListener('click', function() {
        document.getElementById('file-input').click();
    });
    document.getElementById('upload-folder-btn').addEventListener('click', function() {
        document.getElementById('folder-input').click();
    });
    document.getElementById('file-input').addEventListener('change', uploadFiles);
    document.getElementById('folder-input').addEventListener('change', uploadFolder);
    
    // Auto-save functionality
    let saveTimeout;
    editor.on('change', function() {
        if (currentFile) {
            clearTimeout(saveTimeout);
            saveTimeout = setTimeout(saveFile, 2000); // Auto-save after 2 seconds
        }
    });
});

function checkFolderUploadSupport() {
    const folderInput = document.getElementById('folder-input');
    const folderBtn = document.getElementById('upload-folder-btn');
    
    // Check if webkitdirectory is supported
    if ('webkitdirectory' in folderInput) {
        console.log('Folder upload is supported');
        folderBtn.style.display = 'block';
    } else {
        console.log('Folder upload is not supported on this browser');
        folderBtn.style.display = 'none';
        // You could show a message to the user here
    }
    
    // Force visibility for testing (remove this in production)
    folderBtn.style.display = 'block';
    folderBtn.style.opacity = 'webkitdirectory' in folderInput ? '1' : '0.5';
    
    if (!('webkitdirectory' in folderInput)) {
        folderBtn.title = '이 브라우저에서는 폴더 업로드를 지원하지 않습니다';
    }
}

async function loadFileList() {
    try {
        const response = await fetch('/api/files');
        const files = await response.json();
        displayFileList(files);
    } catch (error) {
        console.error('Failed to load file list:', error);
    }
}

function displayFileList(files) {
    const fileList = document.getElementById('file-list');
    fileList.innerHTML = '';
    
    files.forEach(file => {
        const fileItem = document.createElement('div');
        fileItem.className = 'file-item';
        fileItem.textContent = file.name;
        fileItem.onclick = () => loadFile(file.path);
        fileList.appendChild(fileItem);
    });
}

async function loadFile(filePath) {
    try {
        const response = await fetch(`/api/file?path=` + encodeURIComponent(filePath));
        const data = await response.json();
        
        currentFile = filePath;
        editor.setValue(data.content);
        
        // Update UI
        document.getElementById('current-file').textContent = filePath;
        document.getElementById('save-btn').disabled = false;
        document.getElementById('delete-btn').disabled = false;
        
        // Update active file highlight
        document.querySelectorAll('.file-item').forEach(item => {
            item.classList.remove('active');
            if (item.textContent === filePath.split('/').pop()) {
                item.classList.add('active');
            }
        });
        
        // Set editor mode based on file extension
        const ext = filePath.split('.').pop().toLowerCase();
        let mode = 'htmlmixed';
        if (ext === 'css') mode = 'css';
        else if (ext === 'js') mode = 'javascript';
        editor.setOption('mode', mode);
        
    } catch (error) {
        alert('파일을 불러오는데 실패했습니다: ' + error.message);
    }
}

async function saveFile() {
    if (!currentFile) return;
    
    try {
        const response = await fetch('/api/file', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                path: currentFile,
                content: editor.getValue()
            })
        });
        
        if (response.ok) {
            console.log('File saved successfully');
        } else {
            throw new Error('Failed to save file');
        }
    } catch (error) {
        alert('파일 저장 실패: ' + error.message);
    }
}

async function deleteFile() {
    if (!currentFile) return;
    
    if (!confirm('정말로 이 파일을 삭제하시겠습니까?')) return;
    
    try {
        const response = await fetch('/api/file?path=' + encodeURIComponent(currentFile), {
            method: 'DELETE'
        });
        
        if (response.ok) {
            currentFile = null;
            editor.setValue('');
            document.getElementById('current-file').textContent = '선택된 파일 없음';
            document.getElementById('save-btn').disabled = true;
            document.getElementById('delete-btn').disabled = true;
            loadFileList();
        } else {
            throw new Error('Failed to delete file');
        }
    } catch (error) {
        alert('파일 삭제 실패: ' + error.message);
    }
}

async function newFile() {
    const fileName = prompt('새 파일 이름을 입력하세요 (예: index.html):');
    if (!fileName) return;
    
    try {
        const response = await fetch('/api/file', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                path: fileName,
                content: ''
            })
        });
        
        if (response.ok) {
            loadFileList();
            loadFile(fileName);
        } else {
            throw new Error('Failed to create file');
        }
    } catch (error) {
        alert('파일 생성 실패: ' + error.message);
    }
}

async function uploadFiles(event) {
    const files = event.target.files;
    if (!files || files.length === 0) return;
    
    const formData = new FormData();
    for (let i = 0; i < files.length; i++) {
        formData.append('files', files[i]);
    }
    
    try {
        const response = await fetch('/api/upload', {
            method: 'POST',
            body: formData
        });
        
        const result = await response.json();
        
        if (response.ok) {
            alert('업로드 완료: ' + result.message);
            loadFileList(); // Refresh file list
        } else {
            alert('업로드 실패: ' + result.message);
        }
    } catch (error) {
        alert('업로드 오류: ' + error.message);
    } finally {
        // Clear the file input so the same file can be selected again
        event.target.value = '';
    }
}

async function uploadFolder(event) {
    const files = event.target.files;
    if (!files || files.length === 0) {
        alert('폴더가 선택되지 않았습니다.');
        return;
    }
    
    // Check if webkitdirectory is supported
    const folderInput = document.getElementById('folder-input');
    if (!('webkitdirectory' in folderInput)) {
        alert('이 브라우저에서는 폴더 업로드를 지원하지 않습니다. 개별 파일을 업로드해주세요.');
        return;
    }
    
    console.log('폴더 업로드 시작: ' + files.length + '개 파일');
    
    const formData = new FormData();
    for (let i = 0; i < files.length; i++) {
        const file = files[i];
        // webkitRelativePath contains the folder structure
        const relativePath = file.webkitRelativePath || file.name;
        console.log('파일 ' + (i+1) + ': ' + relativePath);
        formData.append('files', file);
        formData.append('paths', relativePath);
    }
    
    try {
        const response = await fetch('/api/upload/folder', {
            method: 'POST',
            body: formData
        });
        
        const result = await response.json();
        
        if (response.ok) {
            alert('폴더 업로드 완료: ' + result.message);
            loadFileList(); // Refresh file list
        } else {
            alert('폴더 업로드 실패: ' + result.message);
        }
    } catch (error) {
        alert('폴더 업로드 오류: ' + error.message);
        console.error('Upload error:', error);
    } finally {
        // Clear the file input so the same folder can be selected again
        event.target.value = '';
    }
}
    """.trimIndent()
    
    private fun getCodeMirrorCSS(): String = ""
    private fun getCodeMirrorJS(): String = ""
    
    // Static file serving functions
    private suspend fun serveStaticFile(call: ApplicationCall, file: File) {
        val contentType = when (file.extension.lowercase()) {
            "html", "htm" -> ContentType.Text.Html
            "css" -> ContentType.Text.CSS
            "js" -> ContentType.Text.JavaScript
            "json" -> ContentType.Application.Json
            "xml" -> ContentType.Text.Xml
            "txt" -> ContentType.Text.Plain
            "png" -> ContentType.Image.PNG
            "jpg", "jpeg" -> ContentType.Image.JPEG
            "gif" -> ContentType.Image.GIF
            "svg" -> ContentType.Image.SVG
            "ico" -> ContentType.parse("image/x-icon")
            else -> ContentType.Application.OctetStream
        }
        
        try {
            val content = withContext(Dispatchers.IO) {
                file.readBytes()
            }
            call.respondBytes(content, contentType)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Error serving file: ${e.message}")
        }
    }
    
    private suspend fun showDirectoryListing(call: ApplicationCall, directory: File, relativePath: String) {
        try {
            val files = withContext(Dispatchers.IO) {
                directory.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
            }
            
            val html = buildString {
                append("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Directory: /$relativePath</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 40px; }
                        .header { border-bottom: 1px solid #ccc; padding-bottom: 10px; margin-bottom: 20px; }
                        .file-list { list-style: none; padding: 0; }
                        .file-item { padding: 8px 0; border-bottom: 1px solid #eee; }
                        .file-item a { text-decoration: none; color: #333; }
                        .file-item a:hover { color: #0066cc; }
                        .directory { font-weight: bold; }
                        .size { color: #666; float: right; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>Directory: /work/$relativePath</h1>
                """)
                
                if (relativePath.isNotEmpty()) {
                    val parentPath = if (relativePath.contains("/")) {
                        relativePath.substringBeforeLast("/")
                    } else {
                        ""
                    }
                    append("""<p><a href="/work/$parentPath">⬆ Parent Directory</a></p>""")
                }
                
                append("""
                    </div>
                    <ul class="file-list">
                """)
                
                files.forEach { file ->
                    val fileName = file.name
                    val fileUrl = "/work/$relativePath${if (relativePath.isEmpty()) "" else "/"}$fileName"
                    val sizeInfo = if (file.isFile) {
                        val size = file.length()
                        when {
                            size < 1024 -> "${size}B"
                            size < 1024 * 1024 -> "${size / 1024}KB"
                            else -> "${size / (1024 * 1024)}MB"
                        }
                    } else ""
                    
                    append("""
                        <li class="file-item">
                            <a href="$fileUrl" class="${if (file.isDirectory) "directory" else ""}">
                                ${if (file.isDirectory) "📁" else "📄"} $fileName
                            </a>
                            <span class="size">$sizeInfo</span>
                        </li>
                    """)
                }
                
                append("""
                    </ul>
                </body>
                </html>
                """)
            }
            
            call.respondText(html, ContentType.Text.Html)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Error listing directory: ${e.message}")
        }
    }
}