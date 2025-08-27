# ⭐ 동화공방
AI 기반 맞춤형 동화 생성 서비스 프로젝트로 사용된 레포지토리입니다.
<br><br>

## 📑 목차

- [서비스 소개](#-서비스-소개)
- [주요 기능](#-주요-기능)
- [기술 스택](#️-기술-스택)
- [실행 가이드](#️-실행-가이드)
- [환경 변수 예시](#-환경-변수-예시)
- [팀 소개](#-팀-소개)

<br>

## 📖 서비스 소개
**동화공방**은 사용자가 입력한 키워드를 바탕으로 AI가 창의적인 동화를 실시간으로 생성해주는 웹 서비스입니다.  
아이의 이름, 역할, 등장인물, 교훈, 장소 등을 설정하면, 나만의 특별한 동화를 생성하고 저장할 수 있습니다.
<br><br>
![image](https://i.postimg.cc/jjv8ZPzC/Chat-GPT-Image-2025-7-28-04-07-03.png)
<br><br>

---
<br>

## 🚀 주요 기능

| 기능 | 설명                                                |
|------|---------------------------------------------------|
| 🎨 동화 생성 | AI에 키워드(이름, 등장인물, 교훈, 장소 등)를 입력하면 동화를 생성하고 저장합니다. |
| 📚 동화 조회 | 생성된 동화를 목록에서 조회하고 다시 감상할 수 있습니다.                  |
| 🖼️ 표지 이미지 | 생성된 동화에 어울리는 AI 표지 이미지를 함께 제공합니다.                 |
| ⭐ 동화 즐겨찾기 | 마음에 드는 동화를 즐겨찾기에 추가하여 나만의 동화책처럼 관리할 수 있습니다.       |
| 🌍 동화 갤러리 | 다른 사용자가 공개한 동화들만 모아 감상할 수 있는 공간입니다.               |
| 👍 동화 좋아요 | 공개 동화에 좋아요를 눌러 감상 평가를 남길 수 있습니다.                  |
| 💬 동화 댓글 | 공개 동화에 대한 감상이나 의견을 댓글로 남기고 다른 사용자와 소통할 수 있습니다.    |
| ✍️ 아이 생각 기록 | 동화를 읽은 뒤, 아이의 느낀 점이나 생각을 부모가 기록하여 보관할 수 있습니다.     |
| 👤 OAuth 로그인 | 네이버 소셜 로그인을 통해 간편하게 이용할 수 있습니다.                   |

<br><br>

## 🛠️ 기술 스택

- **Frontend**: Next.js, React, TypeScript, Tailwind CSS
- **Backend**: Spring Boot, Spring Data JPA, SpringDoc(Open API)
- **AI 연동**: Google Gemini API, Hugging Face Inference API, Stable Diffusion XL
- **인증**: Spring Security, OAuth2, JWT
- **DB**: PostgreSQL
- **클라우드**: Google Cloud Storage
- **배포**: Docker, Render, Vercel

<br><br>
## ⚙️ 실행 가이드
로컬 환경에서 프로젝트를 실행하기 위한 전체 가이드입니다.
<br><br>

### 1. 저장소 클론
```bash
git clone [레포지토리 URL]
cd [프로젝트 폴더]
```
### 2. 백엔드 실행
```bash
cd backend
./gradlew bootRun
```

### 3. 프론트엔드 실행
```bash
cd frontend
npm install
npm run dev
```

<br><br>

## 🔒 환경 변수 예시


### ✅ .env 파일 예시 (루트 디렉토리 또는 백엔드 폴더에 위치)

```env
# 네이버 OAuth2
LOCAL_NAVER_CLIENT_ID=your_naver_client_id
LOCAL_NAVER_CLIENT_SECRET=your_naver_client_secret
LOCAL_NAVER_REDIRECT_URI=http://localhost:8080/login/oauth2/code/naver

# Gemini API
GEMINI_API_KEY=your_gemini_api_key
GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent

# JWT 설정
JWT_SECRET=your_jwt_secret_key

# Hugging Face API
HUGGING_FACE_URL=https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-xl-base-1.0
HUGGING_FACE_TOKEN=your_hugging_face_token

# Google Cloud Storage
GCS_LOCATION=your_gcs_service_account_key.json
GCS_PROJECT_ID=your_gcp_project_id
GCS_BUCKET=your_gcs_bucket_name
```
<br><br>


## 📌 팀 소개

| 이름 | 역할            | 담당 내용                                                                        | GitHub |
|------|---------------|------------------------------------------------------------------------------|--------|
| 김영건 | 프론트엔드, 백엔드 개발 | - 프론트엔드 페이지 구조 설계 및 라우팅 구성<br>- 전역 레이아웃 및 공통 UI 컴포넌트 구현<br>- 아이 생각 기록 기능 구현<br>- 동화 댓글 기능 구현      | [@johnbosco0414](https://github.com/johnbosco0414) |
| 김유석 | 프론트엔드, 백엔드 개발 | - 키워드 조회 및 검증 기능 구현<br>- 동화 갤러리 조회 기능 구현<br>- 동화 갤러리 페이지 UI/UX 구현                               | [@YouSeok518](https://github.com/YouSeok518) |
| 이용호 | 프론트엔드, 백엔드 개발 | - 동화 즐겨찾기 기능 구현<br>- 동화 좋아요 기능 구현 <br>- 프론트엔드/백엔드 서버 배포<br> - GCS 이미지 업로드 기능 구현<br> | [@yongho9064](https://github.com/yongho9064 ) |
| 임정민 | 프론트엔드, 백엔드 개발 | - 외부 AI API 연동<br>- 동화 생성/조회/삭제 기능 구현<br>- 동화 이미지 생성 기능 구현<br>- 동화 즐겨찾기 및 생각기록 UI/UX 구현           | [@simount3](https://github.com/simount3) |
| 오현배 | 프론트엔드, 백엔드 개발 | - OAuth 로그인 및 로그아웃 기능 구현<br>- JWT 인증 기능 구현<br>- 로그인 페이지 UI/UX 구현                                  | [@shihan00321](https://github.com/shihan00321) |