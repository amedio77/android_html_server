# Remote HTML Editor - 기술 구현 세부사항

## 🏗️ 아키텍처 개요

Remote HTML Editor는 MVVM (Model-View-ViewModel) 아키텍처 패턴을 기반으로 하며, Android의 Jetpack Compose와 Kotlin 코루틴을 활용한 현대적인 Android 애플리케이션입니다.

### 전체 시스템 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Browser   │◄──►│  Ktor Server    │◄──►│ Android File    │
│  (Frontend UI)  │    │ (HTTP Service)  │    │    System       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Android Application                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   UI Layer  │  │ ViewModel   │  │  Repository │             │
│  │ (Compose)   │◄─│   Layer     │◄─│    Layer    │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
```

## 📱 Android 앱 아키텍처

### 1. UI Layer (Presentation)

#### MainActivity.kt
앱의 진입점이며 권한 관리와 기본 UI 컨테이너를 제공합니다.

```kotlin
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?)
}
```

**주요 기능:**
- **권한 관리**: 동적 권한 요청 시스템
- **버전별 권한 처리**: Android 버전에 따른 적절한 권한 요청
- **자동 서버 시작 알림**: 권한 허용 후 서버 자동 시작 안내

**권한 매트릭스:**
```kotlin
val permissions = listOf(
    android.Manifest.permission.INTERNET,                    // 네트워크 접근
    android.Manifest.permission.ACCESS_WIFI_STATE,           // WiFi 상태
    android.Manifest.permission.ACCESS_NETWORK_STATE,        // 네트워크 상태
    android.Manifest.permission.WAKE_LOCK                    // 화면 깨우기
) + when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> 
        listOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO)
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> 
        listOf(MANAGE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE)
    else -> 
        listOf(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE)
}
```

#### ServerControlScreen.kt
서버 상태 표시 및 제어를 위한 메인 UI 화면입니다.

```kotlin
@Composable
fun ServerControlScreen(serverViewModel: ServerViewModel = viewModel())
```

**UI 컴포넌트:**
- **서버 상태 카드**: 실행 상태, IP 주소, 포트 정보
- **QR 코드 디스플레이**: 빠른 접속을 위한 QR 코드
- **서버 제어 버튼**: 시작/중지 토글 기능
- **네트워크 정보**: WiFi 연결 상태 및 IP 주소

### 2. ViewModel Layer

#### ServerViewModel.kt
서버 상태 관리 및 비즈니스 로직의 핵심 컴포넌트입니다.

```kotlin
class ServerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _serverState = MutableStateFlow(ServerState())
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()
    
    init {
        // 자동 서버 시작
        startServer()
    }
}
```

**상태 관리:**
```kotlin
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
```

**핵심 기능:**
- **자동 시작**: ViewModel 초기화 시 서버 자동 시작
- **상태 추적**: 서버 실행 상태 실시간 모니터링
- **에러 처리**: 서버 시작 실패 시 상세 오류 정보 제공
- **리소스 관리**: ViewModel 소멸 시 서버 자동 정리

**저장소 경로 관리:**
```kotlin
private fun getWorkDirectory(): String {
    val context = getApplication<Application>()
    return try {
        // 앱별 외부 저장소 사용 (권한 불필요)
        val appExternalDir = context.getExternalFilesDir(null)
        if (appExternalDir != null) {
            File(appExternalDir, "work").absolutePath
        } else {
            // 내부 저장소 폴백
            File(context.filesDir, "work").absolutePath
        }
    } catch (e: Exception) {
        File(context.filesDir, "work").absolutePath
    }
}
```

### 3. Utility Layer

#### NetworkUtils.kt
네트워크 관련 유틸리티 함수들을 제공합니다.

```kotlin
object NetworkUtils {
    fun getDeviceIpAddress(context: Context): String? {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        
        if (!wifiManager.isWifiEnabled) return null
        
        val wifiInfo = wifiManager.connectionInfo
        val ipInt = wifiInfo.ipAddress
        
        return String.format(
            "%d.%d.%d.%d",
            (ipInt and 0xff),
            (ipInt shr 8 and 0xff),
            (ipInt shr 16 and 0xff),
            (ipInt shr 24 and 0xff)
        )
    }
}
```

#### QRCodeGenerator.kt
QR 코드 생성을 위한 유틸리티입니다.

```kotlin
object QRCodeGenerator {
    fun generateQRCode(text: String, size: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
        
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, 
                    if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
```

## 🌐 웹 서버 아키텍처

### Ktor 서버 구성

#### WebServer.kt
Ktor 기반의 임베디드 웹 서버 구현체입니다.

```kotlin
class WebServer(
    private val port: Int = 8080,
    private val rootDirectory: File,
    private val authEnabled: Boolean = false,
    private val authUsername: String? = null,
    private val authPassword: String? = null
) {
    private var server: NettyApplicationEngine? = null
}
```

**서버 설정 단계:**
1. **보안 설정**: HTTP 기본 인증 (선택적)
2. **직렬화 설정**: JSON 콘텐츠 협상
3. **CORS 설정**: 교차 출처 요청 허용
4. **라우팅 설정**: API 및 정적 파일 라우트

**CORS 설정:**
```kotlin
private fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost() // 개발용 - 운영환경에서는 특정 호스트로 제한
    }
}
```

#### 라우팅 구조

```
/                           # 웹 에디터 인터페이스
├── static/                 # 정적 리소스
│   ├── editor.css
│   ├── editor.js
│   ├── codemirror.css
│   └── codemirror.js
├── work/                   # 직접 파일 접근
│   └── {filePath...}
└── api/                    # REST API
    ├── files              # 파일 목록
    ├── file               # 파일 CRUD
    ├── file/new           # 새 파일 생성
    ├── file/move          # 파일 이동
    ├── search             # 파일 검색
    └── upload             # 파일 업로드
```

### FileRoutes.kt - API 구현

#### 파일 목록 API
```kotlin
get("/files") {
    val path = call.request.queryParameters["path"] ?: ""
    val targetDir = if (path.isEmpty()) rootDirectory else File(rootDirectory, path)
    
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
```

#### 다중 파일 업로드 구현
```kotlin
post("/upload") {
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
                            // 보안: 파일명 정화
                            val sanitizedFileName = fileName
                                .replace("..", "")
                                .replace("/", "_")
                                .replace("\\", "_")
                            
                            val file = File(rootDirectory, sanitizedFileName)
                            
                            // 중복 파일 처리
                            val finalFile = handleDuplicateFile(file)
                            
                            finalFile.outputStream().use { output ->
                                part.streamProvider().use { input ->
                                    input.copyTo(output)
                                }
                            }
                            
                            uploadedFiles.add(finalFile.name)
                        } catch (e: Exception) {
                            failedFiles.add(fileName)
                        }
                    }
                }
            }
            part.dispose()
        }
    }
}
```

## 🎨 프론트엔드 웹 인터페이스

### HTML 구조
```html
<div id="app">
    <div class="sidebar">
        <div class="sidebar-header">
            <h3>파일 탐색기</h3>
            <button id="new-file-btn">새 파일</button>
            <button id="upload-btn">파일 업로드</button>
            <input type="file" id="file-input" multiple style="display: none;">
        </div>
        <div id="file-list" class="file-list"></div>
    </div>
    <div class="editor-container">
        <div class="editor-header">
            <span id="current-file">선택된 파일 없음</span>
            <div class="editor-actions">
                <button id="save-btn">저장</button>
                <button id="delete-btn">삭제</button>
            </div>
        </div>
        <textarea id="code-editor"></textarea>
    </div>
</div>
```

### CodeMirror 설정
```javascript
editor = CodeMirror.fromTextArea(document.getElementById('code-editor'), {
    lineNumbers: true,
    theme: 'monokai',
    mode: 'htmlmixed',
    autoCloseTags: true,
    autoCloseBrackets: true,
    lineWrapping: true
});

// 자동 저장 기능
let saveTimeout;
editor.on('change', function() {
    if (currentFile) {
        clearTimeout(saveTimeout);
        saveTimeout = setTimeout(saveFile, 2000);
    }
});
```

### 파일 모드 자동 감지
```javascript
function setEditorMode(filePath) {
    const ext = filePath.split('.').pop().toLowerCase();
    let mode = 'htmlmixed';
    
    switch(ext) {
        case 'css': mode = 'css'; break;
        case 'js': mode = 'javascript'; break;
        case 'json': mode = 'javascript'; break;
        case 'xml': mode = 'xml'; break;
        case 'py': mode = 'python'; break;
        case 'java': mode = 'text/x-java'; break;
        case 'kt': mode = 'text/x-kotlin'; break;
    }
    
    editor.setOption('mode', mode);
}
```

## 🔒 보안 구현

### 파일 시스템 보안

#### 경로 탐색 방지
```kotlin
// 파일 접근 시 보안 검사
if (!file.canonicalPath.startsWith(rootDirectory.canonicalPath)) {
    call.respond(HttpStatusCode.Forbidden, "Access denied")
    return@get
}
```

#### 앱별 저장소 사용
```kotlin
// 권한이 필요 없는 앱별 외부 저장소 사용
val appExternalDir = context.getExternalFilesDir(null)
val workDir = File(appExternalDir, "work")
```

### 네트워크 보안

#### HTTPS 준비 (선택적)
```kotlin
// 운영환경에서는 HTTPS 사용 권장
private fun configureSecurity() {
    if (useHttps) {
        install(HttpsRedirect) {
            sslPort = 8443
            permanentRedirect = true
        }
    }
}
```

#### 호스트 제한
```kotlin
// 운영환경에서는 특정 호스트만 허용
install(CORS) {
    allowHost("192.168.1.0/24") // 로컬 네트워크만
    allowCredentials = false
}
```

## 📊 성능 최적화

### 메모리 관리

#### 코루틴 스코프 관리
```kotlin
class ServerViewModel(application: Application) : AndroidViewModel(application) {
    
    fun startServer() {
        viewModelScope.launch {
            // I/O 작업은 Dispatchers.IO에서 실행
            withContext(Dispatchers.IO) {
                // 파일 시스템 작업
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        webServer?.stop() // 리소스 정리
    }
}
```

#### 파일 스트리밍
```kotlin
// 대용량 파일 처리 시 스트리밍 사용
finalFile.outputStream().use { output ->
    part.streamProvider().use { input ->
        input.copyTo(output) // 메모리 효율적인 복사
    }
}
```

### 네트워크 최적화

#### 압축 지원
```kotlin
install(Compression) {
    gzip {
        priority = 1.0
    }
    deflate {
        priority = 10.0
        minimumSize(1024)
    }
}
```

#### 캐싱 헤더
```kotlin
// 정적 리소스에 대한 캐싱 설정
call.response.caching = CachingOptions(
    cacheControl = CacheControl.MaxAge(maxAgeSeconds = 3600)
)
```

## 🧪 테스트 전략

### 단위 테스트
```kotlin
@Test
fun `test server start with valid configuration`() = runTest {
    val mockApplication = mockk<Application>()
    val viewModel = ServerViewModel(mockApplication)
    
    // 테스트 구현
    assertThat(viewModel.serverState.value.isRunning).isTrue()
}
```

### 통합 테스트
```kotlin
@Test
fun `test file upload API`() = testApplication {
    application {
        configureRouting()
    }
    
    client.post("/api/upload") {
        setBody(MultiPartFormDataContent(
            formData {
                append("files", "test content", Headers.build {
                    append(HttpHeaders.ContentDisposition, 
                           "filename=\"test.txt\"")
                })
            }
        ))
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}
```

### UI 테스트
```kotlin
@Test
fun testServerControlScreen() {
    composeTestRule.setContent {
        ServerControlScreen()
    }
    
    composeTestRule
        .onNodeWithText("서버 시작")
        .performClick()
    
    composeTestRule
        .onNodeWithText("실행 중")
        .assertIsDisplayed()
}
```

## 📈 모니터링 및 로깅

### 구조화된 로깅
```kotlin
private fun logServerEvent(event: String, details: Map<String, Any>) {
    val logMessage = buildString {
        append("ServerViewModel: $event")
        details.forEach { (key, value) ->
            append(" | $key: $value")
        }
    }
    println(logMessage)
}

// 사용 예시
logServerEvent("Server started", mapOf(
    "port" to serverPort.value,
    "ip" to ipAddress,
    "rootDir" to rootDirectory.value
))
```

### 성능 메트릭
```kotlin
class PerformanceMonitor {
    fun measureServerStartTime(): Long {
        val startTime = System.currentTimeMillis()
        // 서버 시작 로직
        val endTime = System.currentTimeMillis()
        return endTime - startTime
    }
    
    fun trackFileOperations(operation: String, fileSize: Long) {
        println("FileOperation: $operation | Size: ${fileSize}B | " +
                "Time: ${System.currentTimeMillis()}")
    }
}
```

## 🔄 배포 및 CI/CD

### Gradle 빌드 최적화
```kotlin
// build.gradle.kts
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // APK 최적화
            isShrinkResources = true
            
            // 서명 설정
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    // 빌드 변형
    flavorDimensions += "environment"
    productFlavors {
        create("development") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        create("production") {
            dimension = "environment"
        }
    }
}
```

### ProGuard 규칙
```proguard
# Ktor 서버 유지
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Serialization 유지
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * {
    *;
}

# QR 코드 라이브러리 유지
-keep class com.google.zxing.** { *; }
```

---

이 문서는 Remote HTML Editor의 핵심 기술 구현 사항들을 상세히 다룹니다. 각 컴포넌트의 역할과 상호작용을 이해하는 데 도움이 되며, 코드 수정이나 확장 시 참고할 수 있는 기술적 근거를 제공합니다.