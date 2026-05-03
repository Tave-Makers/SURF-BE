# 로컬 환경 세팅 가이드

## 사전 설치

| 항목 | 버전 |
|------|------|
| JDK | 17 |
| MySQL | 8.x |
| Redis | 7.x |

---

## 1. 저장소 클론

```bash
git clone {repository-url}
cd SURF-BE
git checkout dev
```

---

## 2. MySQL 설정

```sql
CREATE DATABASE SURF2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 3. Redis 설정

로컬에서는 비밀번호 없이 기본 포트(6379)로 실행

```bash
# macOS (Homebrew)
brew install redis
brew services start redis

# 실행 확인
redis-cli ping  # PONG 응답 확인
```

---

## 4. .env 파일 구성

프로젝트 루트에 `.env` 파일 생성 (기존 팀원에게 전달받은 값 사용)

```properties
# Database
DB_URL=jdbc:mysql://localhost:3306/SURF2?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD={MySQL 비밀번호}

# Kakao OAuth
KAKAO_CLIENT_ID={카카오 앱 REST API 키}
KAKAO_CLIENT_SECRET={카카오 앱 Client Secret}
KAKAO_REDIRECT_URI=http://localhost:8080/login/oauth2/code/kakao
KAKAO_TOKEN_URI=https://kauth.kakao.com/oauth/token
KAKAO_USER_INFO=https://kapi.kakao.com/v2/user/me
KAKAO_AUTHORIZE_URL=https://kauth.kakao.com/oauth/authorize
KAKAO_ADMIN_KEY={카카오 앱 Admin 키}

# JWT
JWT_SECRET={임의의 문자열}
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=86400000

# Feedback
FEEDBACK_HASH_SECRET={임의의 Base64 문자열}

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# AWS S3
AWS_S3_IAM_ACCESS_KEY={IAM 액세스 키}
AWS_S3_IAM_SECRET_KEY={IAM 시크릿 키}
AWS_REGION=ap-northeast-2
AWS_BUCKET_NAME=tavesurf-dev03

# Gmail SMTP (쪽지 발송용)
MAIL_USERNAME={Gmail 주소}
MAIL_PASSWORD={Gmail 앱 비밀번호}
```

---

## 5. Firebase 설정

`src/main/resources/firebase/` 경로에 Firebase Admin SDK JSON 파일 위치

```
src/main/resources/firebase/tave-surf-dev-firebase-adminsdk.json
```

> 파일은 기존 팀원에게 별도 전달받아야 합니다. `.gitignore`에 포함되어 있어 저장소에 없습니다.

---

## 6. 카카오 개발 앱 설정

로컬 개발 시 카카오 개발자 콘솔에서 redirect URI를 추가해야 합니다.

1. [카카오 개발자 콘솔](https://developers.kakao.com) 접속
2. 앱 선택 → **앱 설정 > 플랫폼**
3. Web 플랫폼에 `http://localhost:8080` 추가
4. **카카오 로그인 > Redirect URI**에 아래 추가
   ```
   http://localhost:8080/login/oauth2/code/kakao
   ```

---

## 7. 빌드 및 실행

```bash
./gradlew bootRun
```

실행 후 Swagger: `http://localhost:8080/swagger-ui.html`

---

## 자주 겪는 오류

| 오류 | 원인 | 해결 |
|------|------|------|
| `Redis connection refused` | Redis 미실행 | `brew services start redis` |
| `Access denied for user 'root'` | DB 비밀번호 불일치 | `.env`의 `DB_PASSWORD` 확인 |
| `Kakao OAuth redirect_uri mismatch` | 카카오 콘솔에 URI 미등록 | 6번 항목 참고 |
| `Firebase credentials` 오류 | SDK JSON 파일 없음 | 5번 항목 참고 |
| `JWT signature invalid` | `JWT_SECRET` 값 불일치 | `.env` 재확인 |
