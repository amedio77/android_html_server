package com.lf.remoteeditor

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.lf.remoteeditor.ui.theme.RemoteEditorTheme
import com.lf.remoteeditor.ui.ServerControlScreen

class MainActivity : ComponentActivity() {
    private var hasAllFilesAccess = false
    
    private val manageExternalStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Check if we have all files access permission after returning from settings
        hasAllFilesAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Not needed for older versions
        }
    }
    
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            RemoteEditorTheme {
                // Separate regular permissions from special permissions
                val regularPermissions = listOf(
                    android.Manifest.permission.INTERNET,
                    android.Manifest.permission.ACCESS_WIFI_STATE,
                    android.Manifest.permission.ACCESS_NETWORK_STATE,
                    android.Manifest.permission.WAKE_LOCK
                ) + when {
                    // Android 13+ (API 33+)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                        listOf(
                            android.Manifest.permission.READ_MEDIA_IMAGES,
                            android.Manifest.permission.READ_MEDIA_VIDEO,
                            android.Manifest.permission.READ_MEDIA_AUDIO
                        )
                    }
                    // Android 10 and below (API 29-)
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q -> {
                        listOf(
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                    }
                    // Android 11-12 (API 30-32) - handled separately as special permission
                    else -> {
                        listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
                
                val permissionsState = rememberMultiplePermissionsState(
                    permissions = regularPermissions
                )
                
                // Check if we need special storage permission for Android 11-12
                val needsSpecialStoragePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && 
                                                  Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                
                // Update hasAllFilesAccess state
                LaunchedEffect(Unit) {
                    hasAllFilesAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Environment.isExternalStorageManager()
                    } else {
                        true
                    }
                }
                
                val allPermissionsGranted = permissionsState.allPermissionsGranted && 
                                          (!needsSpecialStoragePermission || hasAllFilesAccess)
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (allPermissionsGranted) {
                        ServerControlScreen()
                    } else {
                        PermissionRequestScreen(
                            regularPermissionsGranted = permissionsState.allPermissionsGranted,
                            specialPermissionGranted = hasAllFilesAccess,
                            needsSpecialPermission = needsSpecialStoragePermission,
                            onRequestRegularPermissions = {
                                permissionsState.launchMultiplePermissionRequest()
                            },
                            onRequestSpecialPermission = {
                                requestManageExternalStoragePermission()
                            }
                        )
                    }
                }
            }
        }
    }
    
    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                manageExternalStorageLauncher.launch(intent)
            } catch (e: Exception) {
                // Fallback to general manage all files access
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageExternalStorageLauncher.launch(intent)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Update permission state when returning from settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasAllFilesAccess = Environment.isExternalStorageManager()
        }
    }
}

@Composable
fun PermissionRequestScreen(
    regularPermissionsGranted: Boolean,
    specialPermissionGranted: Boolean,
    needsSpecialPermission: Boolean,
    onRequestRegularPermissions: () -> Unit,
    onRequestSpecialPermission: () -> Unit
) {
    val androidVersion = Build.VERSION.SDK_INT
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "권한 설정",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Remote HTML Editor가 정상 작동하려면 다음 권한이 필요합니다:",
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (regularPermissionsGranted) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (regularPermissionsGranted) "✅" else "❌",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "기본 권한",
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "• 인터넷 접근\n• 네트워크 상태 확인\n• 화면 깨우기",
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                if (!regularPermissionsGranted) {
                    Button(
                        onClick = onRequestRegularPermissions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text("기본 권한 허용")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Special storage permission for Android 11-12
        if (needsSpecialPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (specialPermissionGranted) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (specialPermissionGranted) "✅" else "❌",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "파일 접근 권한",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "모든 파일에 대한 액세스 권한이 필요합니다.\n(Android ${androidVersion})",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    if (!specialPermissionGranted) {
                        Button(
                            onClick = onRequestSpecialPermission,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text("파일 접근 권한 설정")
                        }
                        
                        Text(
                            text = "※ 설정 화면에서 '모든 파일에 대한 액세스 허용'을 활성화해주세요.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // File permissions info for Android 13+
        if (androidVersion >= Build.VERSION_CODES.TIRAMISU) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ℹ️",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Android 13+ 파일 접근",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "미디어 파일 접근 권한을 사용합니다.\n앱별 디렉토리와 공유 미디어에 접근 가능합니다.",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (regularPermissionsGranted && (!needsSpecialPermission || specialPermissionGranted)) {
            Text(
                text = "🎉 모든 권한이 설정되었습니다!\n서버가 곧 시작됩니다...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Text(
                text = "모든 권한 설정 완료 후 서버가 자동으로 시작됩니다.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}