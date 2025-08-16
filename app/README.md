# Remote HTML Editor

원격 HTML 편집기 - 모바일 디바이스를 웹 서버로 변환하여 파일 관리 및 편집을 제공하는 Android 애플리케이션

## 📱 애플리케이션 개요

Remote HTML Editor는 Android 디바이스에서 웹 서버를 실행하여 브라우저를 통해 파일을 관리하고 편집할 수 있는 애플리케이션입니다. CodeMirror 기반의 강력한 웹 에디터를 제공하며, 실시간 파일 업로드, 다운로드, 편집 기능을 지원합니다.

## ✨ 주요 기능

### 🚀 자동 서버 시작
- 앱 실행 시 웹 서버가 자동으로 시작됩니다
- 별도의 설정 없이 즉시 사용 가능합니다

### 🌐 웹 기반 파일 편집기
- CodeMirror 기반의 고급 코드 에디터
- 구문 강조 (HTML, CSS, JavaScript)
- 자동 완성 및 괄호 매칭
- 2초 자동 저장 기능

### 📁 파일 관리 시스템
- 파일 생성, 편집, 삭제
- 디렉토리 구조 탐색
- 다중 파일 업로드 지원
- 파일 검색 기능

### 🔗 직접 파일 액세스
- URL을 통한 직접 파일 접근 (`http://IP:8080/work/filename`)
- 디렉토리 목록 보기
- 정적 파일 서빙 (이미지, CSS, JS 등)

### 📊 실시간 서버 상태
- QR 코드 생성으로 쉬운 URL 공유
- 네트워크 상태 모니터링
- 서버 시작/중지 제어

## 🏗️ 기술 아키텍처

### Frontend (Android)
- **Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Language**: Kotlin
- **UI Library**: Material Design 3

### Backend (Web Server)
- **Framework**: Ktor (Kotlin)
- **Engine**: Netty
- **Features**: 
  - RESTful API
  - CORS 지원
  - Multipart 파일 업로드
  - 정적 파일 서빙

### 주요 컴포넌트

#### 1. ServerViewModel
서버 상태 관리 및 제어를 담당하는 핵심 컴포넌트
```kotlin
class ServerViewModel(application: Application) : AndroidViewModel(application)
```
- 자동 서버 시작 로직
- 서버 상태 관리 (시작/중지/로딩)
- 네트워크 정보 관리
- QR 코드 생성

#### 2. WebServer
Ktor 기반 웹 서버 구현
```kotlin
class WebServer(
    private val port: Int = 8080,
    private val rootDirectory: File,
    private val authEnabled: Boolean = false
)
```
- HTTP 서버 구동
- 라우팅 설정
- CORS 및 보안 설정
- 정적 파일 서빙

#### 3. FileRoutes
파일 관리 API 엔드포인트 구현
```kotlin
fun Route.fileRoutes(rootDirectory: File)
```
- 파일 CRUD 작업
- 다중 파일 업로드
- 파일 검색
- 디렉토리 관리

## 📂 프로젝트 구조

```
app/src/main/java/com/lf/remoteeditor/
├── MainActivity.kt              # 메인 액티비티 및 권한 관리
├── server/
│   ├── WebServer.kt            # Ktor 웹 서버 구현
│   └── FileRoutes.kt           # 파일 관리 API 라우팅
├── ui/
│   ├── ServerControlScreen.kt  # 서버 제어 UI
│   └── theme/                  # Material Design 테마
├── utils/
│   ├── NetworkUtils.kt         # 네트워크 유틸리티
│   └── QRCodeGenerator.kt      # QR 코드 생성
└── viewmodel/
    └── ServerViewModel.kt      # 서버 상태 관리
```

## 🔧 설정 및 설치

### 시스템 요구사항
- Android 6.0 (API 23) 이상
- 네트워크 연결 (WiFi 권장)
- 파일 저장소 접근 권한

### 빌드 요구사항
- Android Studio Arctic Fox 이상
- Kotlin 1.9.22
- Android Gradle Plugin 8.12.0
- Gradle 8.0 이상

### 빌드 명령어
```bash
# 프로젝트 빌드
./gradlew assembleDebug

# APK 설치
adb install app/build/outputs/apk/debug/app-debug.apk

# 앱 실행
adb shell monkey -p com.lf.remoteeditor -c android.intent.category.LAUNCHER 1
```

## 🚀 사용 방법

### 1. 앱 실행 및 권한 허용
1. 앱을 설치하고 실행합니다
2. 필요한 권한들을 허용합니다:
   - 인터넷 접근
   - WiFi 상태 확인
   - 파일 읽기/쓰기

### 2. 자동 서버 시작
- 권한 허용 후 서버가 자동으로 시작됩니다
- 화면에 표시되는 IP 주소와 포트를 확인합니다

### 3. 웹 브라우저 접속
- 브라우저에서 `http://[디바이스IP]:8080` 접속
- QR 코드 스캔으로 빠른 접속 가능

### 4. 파일 관리
- **새 파일 생성**: "새 파일" 버튼 클릭
- **파일 편집**: 파일 목록에서 파일 선택
- **파일 업로드**: "파일 업로드" 버튼으로 다중 파일 업로드
- **직접 접근**: `http://[IP]:8080/work/filename` 형태로 직접 파일 접근

## 📡 API 문서

### 파일 관리 API

#### GET /api/files
파일 목록 조회
```json
[
  {
    "name": "index.html",
    "path": "index.html",
    "isDirectory": false,
    "size": 1024,
    "lastModified": 1692144000000
  }
]
```

#### GET /api/file?path={filePath}
파일 내용 조회
```json
{
  "path": "index.html",
  "content": "<!DOCTYPE html>..."
}
```

#### POST /api/file
파일 저장
```json
{
  "path": "index.html",
  "content": "<!DOCTYPE html>..."
}
```

#### DELETE /api/file?path={filePath}
파일 삭제

#### POST /api/upload
다중 파일 업로드 (multipart/form-data)

### 정적 파일 서빙

#### GET /work/{filePath}
직접 파일 접근
- 파일이 존재하면 파일 내용 반환
- 디렉토리면 목록 페이지 표시
- 보안 검사를 통한 안전한 접근

## 🔒 보안 기능

### 파일 시스템 보안
- 앱별 외부 저장소 사용 (`/Android/data/com.lf.remoteeditor/files/work`)
- 경로 탐색 공격 방지
- 파일 업로드 시 파일명 정화

### 네트워크 보안
- 로컬 네트워크 전용 접근
- CORS 설정을 통한 교차 출처 요청 제어
- 선택적 HTTP 기본 인증 지원

## 🛠️ 개발 정보

### 주요 의존성
```kotlin
dependencies {
    // Kotlin & Android
    implementation "androidx.core:core-ktx:1.12.0"
    implementation "androidx.activity:activity-compose:1.8.2"
    
    // Jetpack Compose
    implementation platform("androidx.compose:compose-bom:2023.10.01")
    implementation "androidx.compose.ui:ui"
    implementation "androidx.compose.material3:material3"
    
    // Ktor Server
    implementation "io.ktor:ktor-server-core:2.3.5"
    implementation "io.ktor:ktor-server-netty:2.3.5"
    implementation "io.ktor:ktor-server-content-negotiation:2.3.5"
    implementation "io.ktor:ktor-serialization-kotlinx-json:2.3.5"
    
    // QR Code
    implementation "com.google.zxing:core:3.5.2"
    implementation "com.journeyapps:zxing-android-embedded:4.3.0"
    
    // Permissions
    implementation "com.google.accompanist:accompanist-permissions:0.32.0"
}
```

### 빌드 설정
- **Target SDK**: 34 (Android 14)
- **Min SDK**: 24 (Android 7.0)
- **Compile SDK**: 34
- **Java Version**: 17

## 🔄 업데이트 히스토리

### v1.2.0 (최신)
- ✅ 자동 서버 시작 기능 추가
- ✅ 다중 파일 업로드 지원
- ✅ 직접 파일 URL 접근 기능
- ✅ 디렉토리 목록 페이지 개선

### v1.1.0
- ✅ 파일 업로드 기능 추가
- ✅ 보안 강화 (경로 탐색 방지)
- ✅ 에러 처리 개선

### v1.0.0
- ✅ 기본 웹 서버 기능
- ✅ 파일 편집기 구현
- ✅ QR 코드 생성

## 🤝 기여하기

1. 이 저장소를 포크합니다
2. 기능 브랜치를 생성합니다 (`git checkout -b feature/새기능`)
3. 변경사항을 커밋합니다 (`git commit -am '새 기능 추가'`)
4. 브랜치에 푸시합니다 (`git push origin feature/새기능`)
5. Pull Request를 생성합니다

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 📞 지원 및 문의

문제가 발생하거나 제안사항이 있으시면 GitHub Issues를 통해 문의해 주세요.

---

**Remote HTML Editor** - 모바일 디바이스를 강력한 웹 서버로 변환하는 혁신적인 솔루션