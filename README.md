<div align="center">

# 🏄 SURF Backend

### TAVE Makers 커뮤니티 플랫폼

**AI 에이전트 기반 개발 자동화를 적용한 Spring Boot 백엔드**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white)](https://aws.amazon.com/)

<br/>

[📖 API 문서](https://tavesurf.site/swagger-ui.html) · [🐛 이슈 리포트](https://github.com/Tave-Makers/SURF-BE/issues) · [📋 프로젝트 보드](https://github.com/Tave-Makers/SURF-BE/projects)

<br/>

<table>
<tr>
<td align="center" width="50%">
<img src="./docs/login.png" alt="로그인 화면" width="280"/>
</td>
<td align="center" width="50%">
<img src="./docs/home.png" alt="홈 화면" width="280"/>
</td>
</tr>
</table>

</div>

---

## ✨ Highlights

<table>
<tr>
<td width="50%">

### 🤖 AI-Powered Development

- **Claude Code** 멀티 에이전트 QA 자동화
- **Gemini CLI** 서브 에이전트로 코드 분석
- 토큰 **97.5% 절감**, API 비용 **95% 절감**
- 수동 QA 대비 **95% 시간 단축**

</td>
<td width="50%">

### 🚀 Production Ready

- **79개** API 엔드포인트
- **354개** Java 파일, 도메인 중심 설계
- FCM 실시간 푸시 알림
- Kakao OAuth2 소셜 로그인

</td>
</tr>
</table>

---

## 🛠 Tech Stack

<table>
<tr>
<td align="center" width="96">
<img src="https://techstack-generator.vercel.app/java-icon.svg" alt="Java" width="48" height="48" />
<br>Java 17
</td>
<td align="center" width="96">
<img src="https://techstack-generator.vercel.app/restapi-icon.svg" alt="REST API" width="48" height="48" />
<br>REST API
</td>
<td align="center" width="96">
<img src="https://techstack-generator.vercel.app/mysql-icon.svg" alt="MySQL" width="48" height="48" />
<br>MySQL
</td>
<td align="center" width="96">
<img src="https://techstack-generator.vercel.app/aws-icon.svg" alt="AWS" width="48" height="48" />
<br>AWS
</td>
<td align="center" width="96">
<img src="https://techstack-generator.vercel.app/github-icon.svg" alt="GitHub" width="48" height="48" />
<br>GitHub
</td>
</tr>
</table>

| 영역 | 기술 |
|:---:|:---|
| **Backend** | Spring Boot 3.3, Spring Security 6, Spring Data JPA, QueryDSL |
| **Database** | MySQL 8.0, Redis |
| **Auth** | JWT, OAuth2 (Kakao) |
| **Cloud** | AWS EC2, RDS, S3, Firebase FCM |
| **DevOps** | GitHub Actions, Docker |
| **AI/Automation** | Claude Code, Gemini CLI |

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client (React)                           │
└─────────────────────────────┬───────────────────────────────────┘
                              │ HTTPS
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     AWS Application Load Balancer               │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Spring Boot API                          │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐   │
│  │Controller │→ │  Service  │→ │Repository │→ │  Entity   │   │
│  └───────────┘  └───────────┘  └───────────┘  └───────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Security Filter Chain (JWT + OAuth2)                    │   │
│  └─────────────────────────────────────────────────────────┘   │
└───────┬─────────────────┬─────────────────┬─────────────────────┘
        │                 │                 │
        ▼                 ▼                 ▼
   ┌─────────┐      ┌─────────┐      ┌─────────┐
   │  MySQL  │      │  Redis  │      │   S3    │
   │  (RDS)  │      │ (Cache) │      │ (Files) │
   └─────────┘      └─────────┘      └─────────┘
```

---

## 🤖 AI Agent System

> **Claude Code + Gemini CLI 멀티 에이전트 아키텍처**

<div align="center">
<img src="./docs/QA 아키텍처.png" alt="QA 멀티 에이전트 아키텍처" width="800"/>

*Claude Code 멀티 에이전트 + Gemini CLI 서브 에이전트 기반 QA 자동화 시스템*
</div>

### 성과 지표

| 지표 | Before | After | 개선 |
|:---:|:---:|:---:|:---:|
| QA 소요 시간 | 3시간 | 8분 | **95% ↓** |
| 메인 컨텍스트 | 200K 토큰 | 5K 토큰 | **97.5% ↓** |
| API 커버리지 | 수동 테스트 | 79개 자동화 | **100%** |
| 테스트 성공률 | - | 87.8% | - |

---

## 📦 Core Features

<details>
<summary><b>🔐 인증/인가</b></summary>

- Kakao OAuth2 소셜 로그인
- JWT Access/Refresh Token
- Redis 기반 토큰 관리
- 역할 기반 접근 제어 (ADMIN, MANAGER, MEMBER)

</details>

<details>
<summary><b>📝 게시판 시스템</b></summary>

- 게시글 CRUD + 이미지 업로드 (S3)
- 댓글/대댓글 (무한 depth)
- 좋아요, 스크랩
- 게시글 검색 (QueryDSL)

</details>

<details>
<summary><b>🔔 실시간 알림</b></summary>

- Firebase Cloud Messaging (FCM)
- 댓글/좋아요/공지사항 알림
- 딥링크 지원
- Device Token 관리

</details>

<details>
<summary><b>📅 일정 관리</b></summary>

- 일정 CRUD
- 캘린더 월별 조회
- 게시글-일정 매핑
- 일정 카테고리

</details>

<details>
<summary><b>🏆 게이미피케이션</b></summary>

- 활동 점수 시스템 (50+ 활동 유형)
- 배지 시스템
- 활동 기록 관리

</details>

---

## 👨‍💻 My Contributions

> **@GOOHAESEUNG** 의 주요 개발 내역

### 🤖 AI/자동화 시스템

| 기능 | 설명 | PR |
|------|------|:---:|
| **QA 자동화** | Claude Code 멀티 에이전트 기반 79개 API 자동 테스트 | - |
| **API 명세서 자동화** | GitHub Actions + Gemini API 파이프라인 | #245 |

### 🔔 알림 시스템 (전담)

| 기능 | 설명 | PR |
|------|------|:---:|
| FCM 푸시 알림 | Firebase Cloud Messaging 연동 | #185 |
| 공지사항 알림 | 활동 유저 단체 알림 발송 | #213 |
| 인터랙션 알림 | 댓글, 좋아요, 대댓글 실시간 알림 | #184 |
| 알림 조회 | 알림 목록 조회 및 읽음 처리 | #173 |
| 알림 엔티티 설계 | 알림 시스템 DB 설계 | #168 |

### 📅 일정 관리 (전담)

| 기능 | 설명 | PR |
|------|------|:---:|
| 일정 CRUD | 생성, 조회, 수정, 삭제 | #144, #146 |
| 캘린더 조회 | 월별 일정 조회 API | #132 |
| 게시글 매핑 | 게시글-일정 연동 기능 | #152, #166 |
| 일정 카테고리 | 일정 분류 체계 구현 | #143 |

### 🔧 기타 기능

| 기능 | 설명 | PR |
|------|------|:---:|
| 게시글 검색 | QueryDSL 기반 검색 기능 | #135 |
| 로그 시스템 | Logback 설정, AOP 이벤트 로깅 | #106, #200 |
| 인증 토큰 통일 | 쿠키 기반 토큰 관리 리팩토링 | #198 |

---

## 📊 Project Stats

<div align="center">

| 📁 Files | 📝 Lines | 🎯 Domains | 🔌 APIs |
|:---:|:---:|:---:|:---:|
| 354 | 14,155 | 15 | 79 |

</div>

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- MySQL 8.0+
- Redis
- Firebase Admin SDK

### Installation

```bash
# 1. Clone
git clone https://github.com/Tave-Makers/SURF-BE.git
cd SURF-BE

# 2. 환경변수 설정
cp .env.example .env
# .env 파일 수정

# 3. Build & Run
./gradlew build
./gradlew bootRun
```

### Environment Variables

```env
# Database
DB_URL=jdbc:mysql://localhost:3306/surf
DB_USERNAME=root
DB_PASSWORD=password

# JWT
JWT_SECRET=your-secret-key
JWT_EXPIRATION=3600000

# Kakao OAuth
KAKAO_CLIENT_ID=your-client-id
KAKAO_CLIENT_SECRET=your-client-secret

# AWS
AWS_S3_ACCESS_KEY=your-access-key
AWS_S3_SECRET_KEY=your-secret-key
AWS_BUCKET_NAME=your-bucket

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
```

---

## 📚 Documentation

| 문서 | 설명 |
|------|------|
| [API 문서 (Swagger)](https://tavesurf.site/swagger-ui.html) | REST API 명세 |
| [QA 자동화 포트폴리오](./docs/qa/portfolio-qa-automation.md) | AI 에이전트 기반 QA 시스템 |
| [QA 테스트 리포트](./docs/qa/2026-01-23-report.md) | 최신 테스트 결과 |

---

## 🏛 Project Structure

```
src/main/java/com/tavemakers/surf/
├── domain/
│   ├── member/          # 회원 관리
│   ├── post/            # 게시글
│   ├── comment/         # 댓글
│   ├── notification/    # 알림 (FCM)
│   ├── score/           # 활동 점수
│   ├── badge/           # 배지
│   ├── board/           # 게시판
│   ├── scrap/           # 스크랩
│   ├── letter/          # 쪽지
│   ├── home/            # 홈 화면
│   ├── feedback/        # 피드백
│   ├── login/           # 소셜 로그인
│   └── activity/        # 활동 기록
└── global/
    ├── config/          # 설정
    ├── jwt/             # JWT 인증
    ├── common/          # 공통 유틸
    └── logging/         # 로깅
```

---

<div align="center">

**Made with ❤️ by TAVE Makers**

[![Hits](https://hits.seeyoufarm.com/api/count/incr/badge.svg?url=https%3A%2F%2Fgithub.com%2FTave-Makers%2FSURF-BE&count_bg=%2379C83D&title_bg=%23555555&icon=&icon_color=%23E7E7E7&title=hits&edge_flat=false)](https://hits.seeyoufarm.com)

</div>
