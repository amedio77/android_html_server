# Android Studio 설정 가이드

## 프로젝트 열기 체크리스트

### 1. 프로젝트 Import
- File → Open → `C:\Users\러닝팩토리\StudioProjects\android_html_server` 선택

### 2. Gradle Sync
- 자동으로 시작되지 않으면: File → Sync Project with Gradle Files

### 3. SDK 확인 (Tools → SDK Manager)
- [ ] Android SDK Platform 34
- [ ] Android SDK Build-Tools 34.0.0
- [ ] Android Emulator
- [ ] Android SDK Platform-Tools

### 4. JDK 설정 확인
- File → Project Structure → SDK Location
- JDK Location: `C:\Program Files\java\jdk-21` (자동 감지됨)

## 앱 실행 방법

### 에뮬레이터 실행
1. AVD Manager 열기 (Tools → AVD Manager)
2. 에뮬레이터 생성 또는 기존 것 실행
3. Run 'app' (Shift + F10)

### 실제 기기 실행
1. USB 디버깅 활성화
2. USB로 연결
3. Run 'app' (Shift + F10)
4. 기기 선택 다이얼로그에서 연결된 기기 선택

## 자주 발생하는 문제

### Gradle Sync 실패
```
File → Invalidate Caches and Restart
```

### 에뮬레이터가 느릴 때
```
1. AVD Manager → 에뮬레이터 Edit
2. Graphics: Hardware - GLES 2.0 선택
3. RAM 늘리기 (최소 2048 MB)
```

### Build 실패
```bash
# Terminal에서 실행
./gradlew clean
./gradlew --stop
# 그 다음 Android Studio에서 다시 Sync
```

### 기기가 인식되지 않을 때
```
1. adb 재시작
   - Terminal: adb kill-server && adb start-server
2. USB 케이블 변경 (데이터 전송 가능한 케이블 사용)
3. USB 디버깅 재활성화
```

## 앱 실행 후

앱이 실행되면:
1. 서버가 자동으로 시작됨
2. QR 코드가 표시됨
3. 같은 네트워크의 다른 기기에서 QR 코드를 스캔하여 접속

## 디버깅

### Logcat 확인
- View → Tool Windows → Logcat
- 필터: `package:com.lf.remoteeditor`

### 브레이크포인트
- 코드 라인 번호 왼쪽 클릭하여 브레이크포인트 설정
- Debug 'app' (Shift + F9)로 디버그 모드 실행

## 빌드 변형

### Debug 빌드 (개발용)
- Build Variant: debug (기본값)
- 디버깅 가능, 서명 불필요

### Release 빌드 (배포용)
- Build Variant: release
- Build → Generate Signed Bundle/APK
- 서명 키 필요

## 단축키

- **Run**: Shift + F10
- **Debug**: Shift + F9
- **Stop**: Ctrl + F2
- **Sync Gradle**: Ctrl + Shift + O
- **Build**: Ctrl + F9
- **Clean**: 없음 (메뉴 사용)