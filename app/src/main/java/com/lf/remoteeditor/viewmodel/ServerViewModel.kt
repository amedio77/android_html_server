package com.lf.remoteeditor.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.os.Environment
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lf.remoteeditor.server.WebServer
import com.lf.remoteeditor.utils.NetworkUtils
import com.lf.remoteeditor.utils.QRCodeGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ServerState(
    val isRunning: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ServerInfo(
    val ipAddress: String,
    val port: Int,
    val accessUrl: String,
    val qrCodeBitmap: Bitmap? = null
)

class ServerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _serverState = MutableStateFlow(ServerState())
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()
    
    private val _serverInfo = MutableStateFlow<ServerInfo?>(null)
    val serverInfo: StateFlow<ServerInfo?> = _serverInfo.asStateFlow()
    
    // Get proper external storage directory
    private fun getWorkDirectory(): String {
        val context = getApplication<Application>()
        
        return try {
            // Use app-specific external storage (no special permissions needed)
            val appExternalDir = context.getExternalFilesDir(null)
            if (appExternalDir != null) {
                val workDir = File(appExternalDir, "work")
                println("Using app-specific external directory: ${workDir.absolutePath}")
                workDir.absolutePath
            } else {
                // Fallback to internal storage if external is not available
                val workDir = File(context.filesDir, "work")
                println("Fallback to internal storage: ${workDir.absolutePath}")
                workDir.absolutePath
            }
        } catch (e: Exception) {
            // Last fallback to internal storage
            val workDir = File(context.filesDir, "work")
            println("Exception fallback to internal: ${workDir.absolutePath}")
            workDir.absolutePath
        }
    }
    
    val serverPort = mutableStateOf(8080)
    val rootDirectory = mutableStateOf(getWorkDirectory())
    val authEnabled = mutableStateOf(false)
    val authUsername = mutableStateOf("admin")
    val authPassword = mutableStateOf("password")
    
    private var webServer: WebServer? = null
    
    init {
        // Auto-start server when ViewModel is created
        println("ServerViewModel: Initializing and auto-starting server...")
        startServer()
    }
    
    fun startServer() {
        viewModelScope.launch {
            try {
                println("ServerViewModel: Starting server...")
                _serverState.value = ServerState(isLoading = true)
                
                val context = getApplication<Application>()
                val ipAddress = NetworkUtils.getDeviceIpAddress(context)
                println("ServerViewModel: IP Address = $ipAddress")
                
                if (ipAddress == null) {
                    println("ServerViewModel: No IP address available")
                    _serverState.value = ServerState(
                        isRunning = false,
                        isLoading = false,
                        error = "WiFi에 연결되어 있지 않습니다"
                    )
                    return@launch
                }
                
                val rootDir = File(rootDirectory.value)
                println("ServerViewModel: Root directory = ${rootDir.absolutePath}")
                
                if (!rootDir.exists()) {
                    println("ServerViewModel: Creating root directory")
                    val created = rootDir.mkdirs()
                    println("ServerViewModel: Directory created = $created")
                }
                
                println("ServerViewModel: Creating WebServer instance")
                webServer = WebServer(
                    port = serverPort.value,
                    rootDirectory = rootDir,
                    authEnabled = authEnabled.value,
                    authUsername = if (authEnabled.value) authUsername.value else null,
                    authPassword = if (authEnabled.value) authPassword.value else null
                )
                
                println("ServerViewModel: Starting WebServer on port ${serverPort.value}")
                webServer?.start()
                println("ServerViewModel: WebServer started successfully")
                
                val accessUrl = "http://$ipAddress:${serverPort.value}"
                println("ServerViewModel: Access URL = $accessUrl")
                
                val qrCodeBitmap = try {
                    QRCodeGenerator.generateQRCode(accessUrl)
                } catch (e: Exception) {
                    println("ServerViewModel: QR Code generation failed: ${e.message}")
                    null
                }
                
                _serverInfo.value = ServerInfo(
                    ipAddress = ipAddress,
                    port = serverPort.value,
                    accessUrl = accessUrl,
                    qrCodeBitmap = qrCodeBitmap
                )
                
                _serverState.value = ServerState(
                    isRunning = true,
                    isLoading = false
                )
                
                println("ServerViewModel: Server started successfully!")
                
            } catch (e: Exception) {
                println("ServerViewModel: Server start failed: ${e.message}")
                e.printStackTrace()
                _serverState.value = ServerState(
                    isRunning = false,
                    isLoading = false,
                    error = "서버 시작 실패: ${e.message}"
                )
            }
        }
    }
    
    fun stopServer() {
        viewModelScope.launch {
            try {
                _serverState.value = _serverState.value.copy(isLoading = true)
                
                webServer?.stop()
                webServer = null
                
                _serverInfo.value = null
                _serverState.value = ServerState(
                    isRunning = false,
                    isLoading = false
                )
                
            } catch (e: Exception) {
                _serverState.value = ServerState(
                    isRunning = false,
                    isLoading = false,
                    error = "서버 중지 실패: ${e.message}"
                )
            }
        }
    }
    
    fun refreshServerInfo() {
        viewModelScope.launch {
            if (_serverState.value.isRunning) {
                val context = getApplication<Application>()
                val ipAddress = NetworkUtils.getDeviceIpAddress(context)
                
                if (ipAddress != null) {
                    val accessUrl = "http://$ipAddress:${serverPort.value}"
                    val qrCodeBitmap = try {
                        QRCodeGenerator.generateQRCode(accessUrl)
                    } catch (e: Exception) {
                        null
                    }
                    
                    _serverInfo.value = ServerInfo(
                        ipAddress = ipAddress,
                        port = serverPort.value,
                        accessUrl = accessUrl,
                        qrCodeBitmap = qrCodeBitmap
                    )
                }
            }
        }
    }
    
    fun updateServerPort(port: Int) {
        if (port in 1024..65535) {
            serverPort.value = port
        }
    }
    
    fun updateRootDirectory(path: String) {
        rootDirectory.value = path
    }
    
    fun updateAuthEnabled(enabled: Boolean) {
        authEnabled.value = enabled
    }
    
    fun updateAuthUsername(username: String) {
        authUsername.value = username
    }
    
    fun updateAuthPassword(password: String) {
        authPassword.value = password
    }
    
    override fun onCleared() {
        super.onCleared()
        webServer?.stop()
    }
}