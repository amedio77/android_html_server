# Remote HTML Editor - API 레퍼런스

## 📡 API 개요

Remote HTML Editor는 RESTful API를 통해 파일 관리 기능을 제공합니다. 모든 API는 `/api` 경로 하위에서 제공되며, JSON 형태의 요청/응답을 사용합니다.

### 기본 URL
```
http://[디바이스IP]:8080/api
```

### 공통 응답 형식
```typescript
interface ApiResponse {
    success: boolean;
    message?: string;
}
```

## 🗂️ 파일 관리 API

### 1. 파일 목록 조회

#### `GET /api/files`

특정 디렉토리의 파일 목록을 조회합니다.

**요청 매개변수**
- `path` (선택): 조회할 디렉토리 경로 (기본값: 루트)

**요청 예시**
```http
GET /api/files
GET /api/files?path=subfolder
```

**응답**
```typescript
interface FileInfo {
    name: string;           // 파일명
    path: string;           // 상대 경로
    isDirectory: boolean;   // 디렉토리 여부
    size: number;          // 파일 크기 (바이트)
    lastModified: number;  // 최종 수정 시간 (Unix timestamp)
}

type Response = FileInfo[];
```

**응답 예시**
```json
[
    {
        "name": "index.html",
        "path": "index.html",
        "isDirectory": false,
        "size": 2048,
        "lastModified": 1692144000000
    },
    {
        "name": "assets",
        "path": "assets",
        "isDirectory": true,
        "size": 0,
        "lastModified": 1692144000000
    }
]
```

### 2. 파일 내용 조회

#### `GET /api/file`

특정 파일의 내용을 조회합니다.

**요청 매개변수**
- `path` (필수): 파일 경로

**요청 예시**
```http
GET /api/file?path=index.html
```

**응답**
```typescript
interface FileContent {
    path: string;    // 파일 경로
    content: string; // 파일 내용
}
```

**응답 예시**
```json
{
    "path": "index.html",
    "content": "<!DOCTYPE html>\n<html>\n<head>\n    <title>My Page</title>\n</head>\n<body>\n    <h1>Hello World</h1>\n</body>\n</html>"
}
```

**오류 응답**
- `400`: 경로 매개변수 누락
- `404`: 파일이 존재하지 않음
- `413`: 파일이 너무 큼 (10MB 제한)

### 3. 파일 저장

#### `POST /api/file`

파일을 생성하거나 기존 파일을 수정합니다.

**요청 본문**
```typescript
interface SaveFileRequest {
    path: string;    // 파일 경로
    content: string; // 파일 내용
}
```

**요청 예시**
```http
POST /api/file
Content-Type: application/json

{
    "path": "newfile.html",
    "content": "<!DOCTYPE html><html><body><h1>New File</h1></body></html>"
}
```

**응답**
```json
{
    "success": true,
    "message": "File saved successfully"
}
```

### 4. 새 파일/디렉토리 생성

#### `POST /api/file/new`

새 파일이나 디렉토리를 생성합니다.

**요청 본문**
```typescript
interface CreateFileRequest {
    path: string;           // 파일/디렉토리 경로
    content?: string;       // 파일 내용 (파일인 경우)
    isDirectory?: boolean;  // 디렉토리 여부 (기본값: false)
}
```

**요청 예시**
```http
POST /api/file/new
Content-Type: application/json

{
    "path": "assets/style.css",
    "content": "/* CSS styles */"
}
```

**응답**
```json
{
    "success": true,
    "message": "Created successfully"
}
```

**오류 응답**
- `409`: 파일이 이미 존재함
- `500`: 생성 실패

### 5. 파일 삭제

#### `DELETE /api/file`

파일이나 디렉토리를 삭제합니다.

**요청 매개변수**
- `path` (필수): 삭제할 파일/디렉토리 경로

**요청 예시**
```http
DELETE /api/file?path=oldfile.txt
```

**응답**
```json
{
    "success": true,
    "message": "Deleted successfully"
}
```

**오류 응답**
- `400`: 경로 매개변수 누락
- `404`: 파일이 존재하지 않음
- `500`: 삭제 실패

### 6. 파일 이동/이름 변경

#### `PUT /api/file/move`

파일이나 디렉토리를 이동하거나 이름을 변경합니다.

**요청 매개변수**
- `from` (필수): 원본 경로
- `to` (필수): 대상 경로

**요청 예시**
```http
PUT /api/file/move?from=oldname.txt&to=newname.txt
```

**응답**
```json
{
    "success": true,
    "message": "Moved successfully"
}
```

**오류 응답**
- `400`: 매개변수 누락
- `404`: 원본 파일이 존재하지 않음
- `409`: 대상 파일이 이미 존재함
- `500`: 이동 실패

### 7. 파일 검색

#### `GET /api/search`

파일명으로 파일을 검색합니다.

**요청 매개변수**
- `q` (필수): 검색 쿼리

**요청 예시**
```http
GET /api/search?q=index
```

**응답**
```typescript
type Response = FileInfo[]; // 최대 100개 결과
```

### 8. 다중 파일 업로드

#### `POST /api/upload`

여러 파일을 동시에 업로드합니다.

**요청 형식**
- Content-Type: `multipart/form-data`
- 파라미터: `files` (다중 파일)

**JavaScript 예시**
```javascript
const formData = new FormData();
for (let file of files) {
    formData.append('files', file);
}

fetch('/api/upload', {
    method: 'POST',
    body: formData
}).then(response => response.json());
```

**응답**
```typescript
interface UploadResponse {
    success: boolean;
    message: string; // "Uploaded: file1.txt, file2.txt. Failed: file3.txt"
}
```

**특징**
- 동일한 파일명이 있으면 자동으로 번호 추가 (`file_1.txt`)
- 경로 탐색 방지를 위한 파일명 정화
- 부분 실패 시 `206 Partial Content` 응답

## 🌐 정적 파일 서빙

### 직접 파일 접근

#### `GET /work/{filePath}`

파일 시스템에 직접 접근하여 파일을 제공합니다.

**URL 패턴**
```
http://[IP]:8080/work/filename.html
http://[IP]:8080/work/assets/style.css
http://[IP]:8080/work/images/logo.png
```

**기능**
- 파일 존재 시: 파일 내용 반환 (적절한 Content-Type)
- 디렉토리 접근 시: HTML 디렉토리 목록 표시
- 보안 검사: 루트 디렉토리 밖 접근 차단

**지원 파일 형식**
- HTML/CSS/JavaScript
- 이미지 (PNG, JPG, GIF, SVG, ICO)
- JSON, XML, 텍스트 파일
- 기타 바이너리 파일

### 웹 에디터 인터페이스

#### `GET /`

웹 기반 파일 편집기 인터페이스를 제공합니다.

**기능**
- CodeMirror 기반 코드 에디터
- 파일 브라우저
- 실시간 편집 및 자동 저장
- 파일 업로드 인터페이스

#### `GET /static/{resource}`

에디터 정적 자원을 제공합니다.

**리소스**
- `editor.css`: 에디터 스타일
- `editor.js`: 에디터 JavaScript
- `codemirror.css`: CodeMirror 스타일
- `codemirror.js`: CodeMirror 라이브러리

## 🔧 오류 처리

### HTTP 상태 코드

| 코드 | 의미 | 설명 |
|------|------|------|
| 200 | OK | 성공 |
| 206 | Partial Content | 부분 성공 (파일 업로드) |
| 400 | Bad Request | 잘못된 요청 |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 충돌 (파일 이미 존재) |
| 413 | Payload Too Large | 파일 크기 초과 |
| 500 | Internal Server Error | 서버 오류 |

### 오류 응답 형식

```typescript
interface ErrorResponse {
    success: false;
    message: string; // 오류 설명
}
```

### 일반적인 오류 상황

1. **파일이 존재하지 않음**
```json
{
    "success": false,
    "message": "File not found"
}
```

2. **권한 없음**
```json
{
    "success": false,
    "message": "Access denied"
}
```

3. **파일 크기 초과**
```json
{
    "success": false,
    "message": "File too large"
}
```

## 🔒 보안 고려사항

### 경로 탐색 방지
- 모든 파일 경로는 루트 디렉토리 내부로 제한
- `..` 경로 요소 제거
- 절대 경로 검증

### 파일 업로드 보안
- 파일명에서 경로 구분자 제거
- 동일 파일명 충돌 방지
- 파일 크기 제한 (10MB)

### 네트워크 보안
- 로컬 네트워크 전용 접근
- CORS 헤더 설정
- 선택적 HTTP 기본 인증

## 📋 사용 예시

### JavaScript 클라이언트 예제

```javascript
class RemoteFileAPI {
    constructor(baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    async getFiles(path = '') {
        const response = await fetch(`${this.baseUrl}/api/files?path=${path}`);
        return await response.json();
    }
    
    async getFile(path) {
        const response = await fetch(`${this.baseUrl}/api/file?path=${encodeURIComponent(path)}`);
        return await response.json();
    }
    
    async saveFile(path, content) {
        const response = await fetch(`${this.baseUrl}/api/file`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ path, content })
        });
        return await response.json();
    }
    
    async uploadFiles(files) {
        const formData = new FormData();
        for (let file of files) {
            formData.append('files', file);
        }
        
        const response = await fetch(`${this.baseUrl}/api/upload`, {
            method: 'POST',
            body: formData
        });
        return await response.json();
    }
}

// 사용 예시
const api = new RemoteFileAPI('http://192.168.0.3:8080');

// 파일 목록 조회
const files = await api.getFiles();

// 파일 편집
const fileContent = await api.getFile('index.html');
await api.saveFile('index.html', fileContent.content + '\n<!-- Updated -->');
```

---

이 API 문서는 Remote HTML Editor의 모든 백엔드 기능을 다룹니다. 추가 질문이나 개선 사항이 있으면 GitHub Issues를 통해 문의해 주세요.