# Remote HTML Editor - 설치 및 설정 가이드

## 🚀 빠른 시작

### 1단계: APK 다운로드 및 설치
1. 최신 APK 파일을 다운로드합니다
2. Android 기기에서 "알 수 없는 소스" 설치를 허용합니다
3. APK를 설치합니다

### 2단계: 앱 실행 및 권한 허용
1. Remote HTML Editor 앱을 실행합니다
2. 다음 권한들을 허용합니다:
   - 📡 인터넷 접근
   - 📶 WiFi 상태 확인
   - 📁 파일 읽기/쓰기

### 3단계: 서버 접속
1. 앱이 자동으로 서버를 시작합니다
2. 화면에 표시되는 IP 주소를 확인합니다 (예: `192.168.0.3:8080`)
3. 같은 네트워크의 브라우저에서 해당 주소로 접속합니다
4. 또는 QR 코드를 스캔하여 빠르게 접속할 수 있습니다

## 🛠️ 개발 환경 설정

### 시스템 요구사항

#### Android 개발 환경
- **Android Studio**: Arctic Fox (2020.3.1) 이상
- **JDK**: OpenJDK 17 이상
- **Android SDK**: API 34 (Android 14)
- **Gradle**: 8.0 이상

#### 대상 디바이스
- **Android 버전**: 7.0 (API 24) 이상
- **RAM**: 2GB 이상 권장
- **저장공간**: 100MB 이상
- **네트워크**: WiFi 연결 필수

### 프로젝트 클론 및 설정

```bash
# 1. 프로젝트 클론
git clone [레포지토리 URL]
cd MyApplication

# 2. Android Studio에서 프로젝트 열기
# File > Open > 프로젝트 폴더 선택

# 3. Gradle 동기화 대기
# Android Studio가 자동으로 의존성을 다운로드합니다
```

### 환경 설정 파일

#### local.properties
```properties
# Android SDK 경로 설정
sdk.dir=C\:\\Users\\[사용자명]\\AppData\\Local\\Android\\Sdk

# NDK 경로 (선택사항)
ndk.dir=C\:\\Users\\[사용자명]\\AppData\\Local\\Android\\Sdk\\ndk\\[버전]
```

#### gradle.properties
```properties
# JVM 메모리 설정
org.gradle.jvmargs=-Xmx6g -Dfile.encoding=UTF-8

# 병렬 빌드 활성화
org.gradle.parallel=true

# Android X 사용
android.useAndroidX=true

# JDK 경로 (필요시)
org.gradle.java.home=C\:\\Program Files\\Eclipse Adoptium\\jdk-17.0.16.8-hotspot
```

## 🔧 빌드 및 배포

### 개발 빌드

```bash
# Debug APK 빌드
./gradlew assembleDebug

# Release APK 빌드 (서명 필요)
./gradlew assembleRelease

# 테스트 실행
./gradlew test

# 전체 빌드 (테스트 포함)
./gradlew build
```

### 빌드 산출물
```
app/build/outputs/apk/
├── debug/
│   └── app-debug.apk          # 개발용 APK
└── release/
    └── app-release.apk        # 배포용 APK (서명 필요)
```

### ADB를 통한 설치 및 테스트

```bash
# 연결된 디바이스 확인
adb devices

# APK 설치
adb install app/build/outputs/apk/debug/app-debug.apk

# 앱 실행
adb shell monkey -p com.lf.remoteeditor -c android.intent.category.LAUNCHER 1

# 로그 확인
adb logcat -d -s System.out

# 앱 제거
adb uninstall com.lf.remoteeditor
```

## 📱 디바이스별 설정

### Samsung Galaxy 시리즈
1. **설정 > 앱 > 특별 접근** 에서 "모든 파일 액세스" 권한 허용
2. **개발자 옵션**에서 USB 디버깅 활성화
3. **Knox 보안**: 일부 기능이 제한될 수 있음

### Google Pixel 시리즈
1. **설정 > 개인정보 보호 > 권한 관리자**에서 권한 확인
2. **파일 및 미디어** 권한을 "모든 파일 관리" 모드로 설정

### Xiaomi (MIUI)
1. **보안** 앱에서 "자동 시작 관리" 허용
2. **배터리 최적화** 예외 설정
3. **MIUI 최적화** 비활성화 (개발자 옵션)

### OnePlus (OxygenOS)
1. **배터리 최적화** 예외 설정
2. **백그라운드 앱 새로고침** 허용

## 🔧 고급 설정

### 서버 포트 변경
```kotlin
// ServerViewModel.kt에서 기본 포트 수정
val serverPort = mutableStateOf(8080) // 원하는 포트로 변경
```

### 인증 활성화
```kotlin
// ServerViewModel.kt에서 인증 설정
val authEnabled = mutableStateOf(true)
val authUsername = mutableStateOf("admin")
val authPassword = mutableStateOf("password123")
```

### 파일 크기 제한 변경
```kotlin
// FileRoutes.kt에서 파일 크기 제한 수정
if (file.length() > 10 * 1024 * 1024) { // 10MB → 원하는 크기로 변경
```

### 커스텀 테마 적용
```kotlin
// ui/theme/Theme.kt에서 색상 커스터마이징
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,           // 원하는 색상으로 변경
    secondary = PurpleGrey80,
    tertiary = Pink80
)
```

## 🐛 문제 해결

### 일반적인 문제

#### 1. 앱이 시작되지 않음
```bash
# 로그 확인
adb logcat -d | grep -i "error\|exception"

# 권한 상태 확인
adb shell dumpsys package com.lf.remoteeditor | grep permission
```

**해결방법:**
- 모든 권한이 허용되었는지 확인
- 앱 데이터 삭제 후 재시작
- 디바이스 재부팅

#### 2. 서버에 접속할 수 없음
```bash
# 네트워크 연결 확인
adb shell ping 8.8.8.8

# 포트 사용 여부 확인
adb shell netstat -an | grep 8080
```

**해결방법:**
- WiFi 연결 상태 확인
- 방화벽 설정 검토
- 다른 포트로 변경 시도

#### 3. 파일 업로드 실패
**해결방법:**
- 저장공간 여유 용량 확인
- 파일 권한 설정 검토
- 앱별 저장소 권한 확인

#### 4. 웹 에디터가 로드되지 않음
**해결방법:**
- 브라우저 캐시 삭제
- 다른 브라우저로 시도
- 네트워크 연결 상태 확인

### 빌드 오류 해결

#### Gradle 동기화 실패
```bash
# Gradle Wrapper 새로고침
./gradlew wrapper --gradle-version 8.0

# 캐시 정리
./gradlew clean
rm -rf .gradle/
```

#### 의존성 충돌
```bash
# 의존성 트리 확인
./gradlew app:dependencies

# 특정 의존성 강제 사용
implementation('io.ktor:ktor-server-core:2.3.5') {
    force = true
}
```

#### 메모리 부족 오류
```properties
# gradle.properties에서 메모리 증가
org.gradle.jvmargs=-Xmx8g -XX:MaxMetaspaceSize=512m
```

## 📋 검증 체크리스트

### 개발 환경 준비
- [ ] Android Studio 설치 완료
- [ ] JDK 17 설치 및 설정
- [ ] Android SDK 다운로드
- [ ] 프로젝트 클론 및 동기화

### 빌드 검증
- [ ] `./gradlew assembleDebug` 성공
- [ ] APK 파일 생성 확인
- [ ] 테스트 실행 성공

### 디바이스 테스트
- [ ] APK 설치 성공
- [ ] 모든 권한 허용
- [ ] 서버 자동 시작 확인
- [ ] 웹 브라우저 접속 성공
- [ ] 파일 업로드/편집 테스트

### 네트워크 테스트
- [ ] 로컬 네트워크 접속 확인
- [ ] QR 코드 스캔 기능 테스트
- [ ] 다중 디바이스 동시 접속 테스트

## 📞 추가 지원

### 문서 리소스
- [Android 개발자 가이드](https://developer.android.com/guide)
- [Kotlin 공식 문서](https://kotlinlang.org/docs/)
- [Ktor 프레임워크 문서](https://ktor.io/docs/)
- [Jetpack Compose 가이드](https://developer.android.com/jetpack/compose)

### 커뮤니티 지원
- GitHub Issues를 통한 버그 신고
- 기능 제안 및 개선 요청
- 개발자 커뮤니티 참여

---

이 가이드를 따라하시면 Remote HTML Editor를 성공적으로 설치하고 사용할 수 있습니다. 추가 질문이나 문제가 있으시면 GitHub Issues를 통해 문의해 주세요.