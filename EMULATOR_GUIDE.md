# Android 에뮬레이터 설정 가이드

## 🚀 빠른 설정 (5분)

### 1. AVD Manager 열기
- **Tools → AVD Manager** 클릭
- 또는 툴바의 📱 아이콘 클릭

### 2. 에뮬레이터 생성

#### Step 1: Create Virtual Device
- "Create Virtual Device..." 버튼 클릭

#### Step 2: 하드웨어 선택
추천 기기:
- **Pixel 6** (균형잡힌 성능)
- **Pixel 4** (가벼운 옵션)
- **Pixel 7 Pro** (대화면 테스트)

선택 후 "Next" 클릭

#### Step 3: 시스템 이미지 선택
**권장 설정:**
- **API Level**: 34 (Android 14)
- **ABI**: x86_64 (Intel) 또는 arm64-v8a (M1/M2 Mac)
- **Target**: Google APIs (Google Play 포함)

⚠️ 첫 설치시 "Download" 클릭 필요 (약 1-2GB)

#### Step 4: AVD 설정
- **AVD Name**: Pixel_6_API_34 (또는 원하는 이름)
- **Startup orientation**: Portrait
- **Graphics**: Hardware - GLES 2.0 (권장)

#### Step 5: Advanced Settings (선택사항)
"Show Advanced Settings" 클릭:
- **RAM**: 2048 MB (최소) / 4096 MB (권장)
- **VM heap**: 256 MB
- **Internal Storage**: 2048 MB
- **SD card**: 512 MB

"Finish" 클릭

## 🎮 에뮬레이터 실행

### 방법 1: AVD Manager에서
1. AVD Manager 열기
2. 생성된 에뮬레이터 우측 ▶️ 클릭

### 방법 2: Run 앱에서
1. Run 'app' (Shift + F10)
2. 에뮬레이터 자동 실행

## ⚡ 성능 최적화

### Windows에서 HAXM 설치 (Intel CPU)
1. SDK Manager → SDK Tools
2. "Intel x86 Emulator Accelerator (HAXM)" 체크
3. Apply → 설치

### 에뮬레이터 속도 개선
1. AVD Manager → 에뮬레이터 Edit (✏️)
2. 설정 변경:
   - Graphics: **Hardware - GLES 2.0**
   - Cold Boot: **Quick boot**
   - Multi-Core CPU: 4개 이상

### 메모리 부족시
```
# Android Studio 메모리 증가
Help → Edit Custom VM Options
-Xmx4096m (4GB로 설정)
```

## 🔧 문제 해결

### "에뮬레이터가 시작되지 않음"
```bash
# Terminal에서
cd C:\Users\러닝팩토리\AppData\Local\Android\Sdk\emulator
emulator -list-avds
emulator @Pixel_6_API_34
```

### "HAXM is not installed"
1. BIOS에서 Intel VT-x 활성화
2. Windows Features에서 Hyper-V 비활성화
3. HAXM 재설치

### "에뮬레이터가 너무 느림"
1. Snapshot 사용:
   - AVD Edit → Advanced → Snapshot: Enabled
2. GPU 가속:
   - Graphics: Hardware - GLES 2.0
3. RAM 증가:
   - Memory: 4096 MB

### "검은 화면만 보임"
1. 에뮬레이터 Cold Boot:
   - AVD Manager → ▼ → Cold Boot Now
2. Graphics 설정 변경:
   - Software - GLES 2.0으로 변경

## 📱 에뮬레이터 단축키

| 기능 | 단축키 |
|------|--------|
| 홈 화면 | Home |
| 뒤로 가기 | Esc |
| 멀티태스킹 | Ctrl + Shift + M |
| 회전 | Ctrl + ← / → |
| 스크린샷 | Ctrl + S |
| 확대/축소 | Ctrl + ↑ / ↓ |
| 전원 | Ctrl + P |

## 🚀 앱 설치 및 실행

### Android Studio에서
1. 에뮬레이터 실행
2. Run 'app' (Shift + F10)
3. 자동 설치 및 실행

### Terminal에서
```bash
# APK 설치
adb install app/build/outputs/apk/debug/app-debug.apk

# 앱 실행
adb shell am start -n com.lf.remoteeditor/.MainActivity

# 로그 확인
adb logcat | grep RemoteEditor
```

## 💡 유용한 팁

### 여러 에뮬레이터 동시 실행
- 각각 다른 API 레벨로 테스트 가능
- 네트워크 통신 테스트 가능

### Snapshot 저장
- 에뮬레이터 상태 저장
- 다음 실행시 빠른 시작

### 네트워크 설정
- Extended Controls (⋮) → Settings → Proxy
- HTTP Proxy 설정 가능

## 📋 체크리스트

- [ ] AVD Manager에서 에뮬레이터 생성
- [ ] API 34 시스템 이미지 다운로드
- [ ] Hardware Graphics 활성화
- [ ] RAM 2GB 이상 할당
- [ ] 에뮬레이터 정상 부팅 확인
- [ ] 앱 설치 및 실행 테스트