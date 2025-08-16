# Remote HTML Editor v1.0.0 - Initial Release

🎉 **첫 번째 공식 릴리즈!** 

Android 디바이스를 웹 서버로 변환하여 브라우저를 통해 파일을 편집하고 관리할 수 있는 혁신적인 앱입니다.

## ✨ 주요 기능

### 🚀 자동 서버 시작
- 앱 실행 시 웹 서버가 자동으로 시작됩니다
- 별도의 설정 없이 즉시 사용 가능

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

## 🏗️ 기술 스택

- **Frontend**: Jetpack Compose (Material Design 3)
- **Backend**: Ktor Web Server (Kotlin)
- **Architecture**: MVVM Pattern
- **Language**: Kotlin 1.9.22
- **Target SDK**: Android 14 (API 34)
- **Min SDK**: Android 7.0 (API 24)

## 📱 설치 방법

### 방법 1: APK 직접 설치 (권장)
1. 아래 Assets에서 `app-debug.apk` 다운로드
2. Android 디바이스에서 개발자 옵션 활성화
3. "알 수 없는 소스" 설치 허용
4. APK 파일 실행하여 설치

### 방법 2: 원격 설치 (개발자용)
- 상세 가이드: [REMOTE_INSTALLATION.md](https://github.com/amedio77/android_html_server/blob/master/app/REMOTE_INSTALLATION.md)

## 🚀 빠른 시작

1. **앱 설치 및 실행**
2. **권한 허용** (인터넷, 파일 읽기/쓰기)
3. **자동 서버 시작** 확인
4. **브라우저 접속**: `http://[디바이스IP]:8080`
5. **파일 편집 시작**!

## 📚 문서

- 📖 [메인 문서 (README)](https://github.com/amedio77/android_html_server/blob/master/app/README.md)
- 🔧 [설치 가이드](https://github.com/amedio77/android_html_server/blob/master/app/SETUP_GUIDE.md)
- 📡 [API 레퍼런스](https://github.com/amedio77/android_html_server/blob/master/app/API_REFERENCE.md)
- 🛠️ [기술 세부사항](https://github.com/amedio77/android_html_server/blob/master/app/TECHNICAL_DETAILS.md)
- 🌐 [원격 설치 가이드](https://github.com/amedio77/android_html_server/blob/master/app/REMOTE_INSTALLATION.md)

## 🔒 보안 기능

- 앱별 외부 저장소 사용 (권한 최소화)
- 경로 탐색 공격 방지
- CORS 설정을 통한 안전한 웹 접근
- 파일 업로드 시 보안 검증

## 📋 시스템 요구사항

- **Android**: 7.0 (API 24) 이상
- **RAM**: 2GB 이상 권장
- **저장공간**: 100MB 이상
- **네트워크**: WiFi 연결 필수

## 🛠️ 빌드 정보

- **빌드 일시**: 2024-08-16
- **APK 크기**: ~22MB
- **서명**: Debug 서명 (개발용)

## 🔄 다음 버전 계획

- [ ] HTTPS 지원
- [ ] 사용자 인증 시스템
- [ ] 플러그인 시스템
- [ ] 테마 커스터마이징
- [ ] 백업/복원 기능

## 🤝 기여하기

1. 이 저장소를 포크합니다
2. 기능 브랜치를 생성합니다 (`git checkout -b feature/새기능`)
3. 변경사항을 커밋합니다 (`git commit -am '새 기능 추가'`)
4. 브랜치에 푸시합니다 (`git push origin feature/새기능`)
5. Pull Request를 생성합니다

## 📄 라이선스

MIT License - 자유롭게 사용, 수정, 배포 가능

## 📞 지원

- 🐛 **버그 신고**: [GitHub Issues](https://github.com/amedio77/android_html_server/issues)
- 💡 **기능 제안**: [GitHub Discussions](https://github.com/amedio77/android_html_server/discussions)
- 📧 **이메일 문의**: GitHub 프로필 참조

---

**Remote HTML Editor** - 모바일 디바이스를 강력한 웹 서버로 변환하는 혁신적인 솔루션 🚀