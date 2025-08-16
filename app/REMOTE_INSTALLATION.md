# Remote HTML Editor - 원격 설치 가이드

이 가이드는 Remote HTML Editor 앱을 다른 Android 디바이스에 원격으로 설치하는 방법을 설명합니다.

## 🎯 설치 방법 개요

### 방법 1: GitHub에서 APK 다운로드 (권장) ⭐
가장 간단한 방법으로, GitHub Release에서 APK를 직접 다운로드하여 설치

### 방법 2: WiFi ADB를 통한 원격 설치
개발자 모드를 활용하여 네트워크를 통해 직접 설치

### 방법 3: 웹 링크를 통한 배포
임시 웹 서버를 통해 APK 배포

---

## 📱 방법 1: GitHub APK 다운로드

### 대상 디바이스에서 설치

1. **개발자 옵션 활성화**
   ```
   설정 > 휴대전화 정보 > 빌드 번호 7회 연속 탭
   ```

2. **알 수 없는 소스 허용**
   ```
   설정 > 보안 > 알 수 없는 소스 허용
   또는
   설정 > 앱 및 알림 > 특별 앱 액세스 > 알 수 없는 앱 설치
   ```

3. **APK 다운로드 및 설치**
   - GitHub 저장소 방문: https://github.com/amedio77/android_html_server
   - Releases 섹션에서 최신 버전 선택
   - `app-debug.apk` 파일 다운로드
   - 다운로드된 파일 탭하여 설치

4. **앱 실행**
   - Remote HTML Editor 앱 실행
   - 권한 허용
   - 자동으로 서버 시작됨

---

## 🌐 방법 2: WiFi ADB를 통한 원격 설치

### 사전 요구사항
- 개발용 PC와 대상 Android 디바이스가 같은 WiFi 네트워크에 연결
- 대상 디바이스에서 개발자 옵션 및 USB 디버깅 활성화

### 1단계: 대상 디바이스 설정

**개발자 옵션 활성화:**
```
설정 > 휴대전화 정보 > 빌드 번호 7회 연속 탭
```

**USB 디버깅 활성화:**
```
설정 > 개발자 옵션 > USB 디버깅 활성화
```

**무선 디버깅 활성화 (Android 11+):**
```
설정 > 개발자 옵션 > 무선 디버깅 활성화
```

### 2단계: WiFi ADB 연결

#### 방법 A: USB로 초기 연결 후 WiFi 전환

1. **USB로 디바이스 연결**
   ```bash
   adb devices
   ```

2. **TCP/IP 모드로 전환**
   ```bash
   adb tcpip 5555
   ```

3. **디바이스 IP 주소 확인**
   ```bash
   adb shell ip route | grep wlan0
   ```
   또는 디바이스에서: 설정 > WiFi > 현재 네트워크 > 고급

4. **WiFi로 연결**
   ```bash
   # [디바이스IP]를 실제 IP로 변경
   adb connect 192.168.1.100:5555
   ```

#### 방법 B: 무선 디버깅 (Android 11+)

1. **페어링 코드 생성**
   ```
   설정 > 개발자 옵션 > 무선 디버깅 > 페어링 코드로 디바이스 페어링
   ```

2. **PC에서 페어링**
   ```bash
   adb pair 192.168.1.100:6666
   # 디바이스에 표시된 페어링 코드 입력
   ```

3. **연결**
   ```bash
   adb connect 192.168.1.100:5555
   ```

### 3단계: APK 설치

```bash
# 연결 확인
adb devices

# APK 설치
adb install app/build/outputs/apk/debug/app-debug.apk

# 앱 실행
adb shell monkey -p com.lf.remoteeditor -c android.intent.category.LAUNCHER 1

# 로그 확인 (선택)
adb logcat -d -s System.out | grep ServerViewModel
```

### 4단계: 연결 해제

```bash
# 특정 디바이스 연결 해제
adb disconnect 192.168.1.100:5555

# 모든 연결 해제
adb disconnect
```

---

## 🌍 방법 3: 웹 서버를 통한 배포

개발 PC에서 간단한 웹 서버를 구축하여 APK 배포

### Python을 이용한 간단 서버

1. **APK가 있는 디렉토리로 이동**
   ```bash
   cd "C:\Users\PC\AndroidStudioProjects\MyApplication\app\build\outputs\apk\debug"
   ```

2. **Python 웹 서버 시작**
   ```bash
   # Python 3
   python -m http.server 8000
   
   # Python 2
   python -m SimpleHTTPServer 8000
   ```

3. **대상 디바이스에서 접속**
   - 브라우저에서 `http://[PC_IP]:8000` 접속
   - `app-debug.apk` 파일 클릭하여 다운로드
   - 다운로드된 APK 설치

### Node.js를 이용한 서버 (선택)

```bash
npx http-server . -p 8000 -c-1
```

---

## 🔧 문제 해결

### ADB 연결 문제

**"device not found" 오류:**
```bash
# ADB 서버 재시작
adb kill-server
adb start-server

# 디바이스 재연결
adb connect [디바이스IP]:5555
```

**"connection refused" 오류:**
- 방화벽 설정 확인
- 디바이스와 PC가 같은 네트워크인지 확인
- 디바이스에서 무선 디버깅이 활성화되었는지 확인

### APK 설치 실패

**"App not installed" 오류:**
- 개발자 옵션에서 "알 수 없는 소스 허용" 확인
- 기존 앱이 설치되어 있다면 제거 후 재설치
- 저장공간 확인

**권한 오류:**
```bash
# 기존 앱 제거
adb uninstall com.lf.remoteeditor

# 강제 설치
adb install -r app-debug.apk
```

### 네트워크 연결 확인

**PC와 디바이스 연결 테스트:**
```bash
# PC에서 디바이스로 ping
ping [디바이스IP]

# 포트 연결 테스트
telnet [디바이스IP] 5555
```

---

## 📋 배포 체크리스트

### 설치 전 확인사항
- [ ] 대상 디바이스에서 개발자 옵션 활성화
- [ ] USB 디버깅 활성화
- [ ] 알 수 없는 소스 허용
- [ ] WiFi 네트워크 연결 확인

### WiFi ADB 사용시
- [ ] PC와 디바이스가 같은 네트워크에 연결
- [ ] 방화벽 설정 확인
- [ ] ADB 도구 설치 확인

### 설치 후 확인사항
- [ ] 앱이 정상적으로 설치됨
- [ ] 권한 허용 완료
- [ ] 서버 자동 시작 확인
- [ ] 웹 브라우저에서 접속 가능

---

## 🚀 자동화 스크립트

### Windows 배치 파일

```batch
@echo off
echo Remote HTML Editor 설치 스크립트
echo ================================

set /p DEVICE_IP="디바이스 IP 주소를 입력하세요: "

echo 디바이스에 연결 중...
adb connect %DEVICE_IP%:5555

echo APK 설치 중...
adb install app\build\outputs\apk\debug\app-debug.apk

echo 앱 실행 중...
adb shell monkey -p com.lf.remoteeditor -c android.intent.category.LAUNCHER 1

echo 설치 완료!
pause
```

### Linux/Mac 셸 스크립트

```bash
#!/bin/bash
echo "Remote HTML Editor 설치 스크립트"
echo "================================"

read -p "디바이스 IP 주소를 입력하세요: " DEVICE_IP

echo "디바이스에 연결 중..."
adb connect $DEVICE_IP:5555

echo "APK 설치 중..."
adb install app/build/outputs/apk/debug/app-debug.apk

echo "앱 실행 중..."
adb shell monkey -p com.lf.remoteeditor -c android.intent.category.LAUNCHER 1

echo "설치 완료!"
```

---

## 📞 지원

설치 중 문제가 발생하면:
1. 이 가이드의 문제 해결 섹션 확인
2. GitHub Issues에 문제 신고
3. 로그 파일과 함께 상세한 오류 내용 제공

**유용한 디버깅 명령어:**
```bash
# 연결된 디바이스 확인
adb devices

# 설치된 앱 확인
adb shell pm list packages | grep remoteeditor

# 앱 로그 확인
adb logcat -d | grep RemoteEditor
```

---

이 가이드를 통해 Remote HTML Editor를 쉽게 원격 설치할 수 있습니다! 🎯