package com.lf.remoteeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lf.remoteeditor.viewmodel.ServerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerControlScreen(
    viewModel: ServerViewModel = viewModel()
) {
    val serverState by viewModel.serverState.collectAsState()
    val serverInfo by viewModel.serverInfo.collectAsState()
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Server Icon",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Remote HTML Editor",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "웹 브라우저에서 HTML 파일을 편집하세요",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Server Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (serverState.isRunning) Color(0xFF4CAF50).copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (serverState.isRunning) Icons.Default.CheckCircle
                        else Icons.Default.Cancel,
                        contentDescription = "Status",
                        tint = if (serverState.isRunning) Color(0xFF4CAF50) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = if (serverState.isRunning) "서버 실행 중" else "서버 중지됨",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (serverState.isRunning && serverInfo != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Divider()
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Server Info
                    InfoRow("IP 주소", serverInfo!!.ipAddress)
                    InfoRow("포트", serverInfo!!.port.toString())
                    InfoRow("접속 URL", serverInfo!!.accessUrl)
                    
                    if (serverInfo!!.qrCodeBitmap != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "QR 코드로 접속",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                // QR 코드는 나중에 구현
                            }
                        }
                    }
                }
                
                serverState.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        if (serverState.isRunning) {
                            viewModel.stopServer()
                        } else {
                            viewModel.startServer()
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !serverState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (serverState.isRunning) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            ) {
                if (serverState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (serverState.isRunning) Icons.Default.Stop
                        else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (serverState.isRunning) "서버 중지" else "서버 시작"
                    )
                }
            }
            
            Button(
                onClick = { viewModel.refreshServerInfo() },
                modifier = Modifier.weight(1f),
                enabled = serverState.isRunning && !serverState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("새로고침")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "서버 설정",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = viewModel.serverPort.value.toString(),
                    onValueChange = { 
                        it.toIntOrNull()?.let { port ->
                            viewModel.updateServerPort(port)
                        }
                    },
                    label = { Text("포트 번호") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !serverState.isRunning,
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = viewModel.rootDirectory.value,
                    onValueChange = { viewModel.updateRootDirectory(it) },
                    label = { Text("루트 디렉토리") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !serverState.isRunning,
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = viewModel.authEnabled.value,
                        onCheckedChange = { viewModel.updateAuthEnabled(it) },
                        enabled = !serverState.isRunning
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("인증 사용")
                }
                
                if (viewModel.authEnabled.value) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = viewModel.authUsername.value,
                        onValueChange = { viewModel.updateAuthUsername(it) },
                        label = { Text("사용자명") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !serverState.isRunning,
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = viewModel.authPassword.value,
                        onValueChange = { viewModel.updateAuthPassword(it) },
                        label = { Text("비밀번호") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !serverState.isRunning,
                        singleLine = true
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}