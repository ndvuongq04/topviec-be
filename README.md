# TopViec Backend

Backend của TopViec là REST API xây dựng bằng Spring Boot. Ứng dụng xử lý xác thực, phân quyền, quản lý hồ sơ ứng viên, công ty, tin tuyển dụng, ứng tuyển, phỏng vấn, dịch vụ tuyển dụng, thanh toán và kiểm duyệt nội dung.

## Tech Stack

| Component      | Version / Library                                   |
| -------------- | --------------------------------------------------- |
| Java           | 21                                                  |
| Spring Boot    | `4.0.3`                                             |
| Gradle Wrapper | `9.3.1`                                             |
| Database       | MySQL 8.0                                           |
| ORM            | Spring Data JPA / Hibernate                         |
| Security       | Spring Security, OAuth2 Resource Server, JWT        |
| Cache/session  | Redis 7, Spring Data Redis, Spring Cache            |
| Validation     | Spring Validation                                   |
| Mail/template  | Spring Mail, Thymeleaf                              |
| File storage   | Local filesystem, served through `/api/v1/files/**` |
| Payment        | VNPAY                                               |
| Env loader     | `dotenv-java`                                       |
| Container      | Docker, Docker Compose                              |

## System Requirements

| Tool           | Minimum          | Recommended                     |
| -------------- | ---------------- | ------------------------------- |
| JDK            | 21               | Temurin 21                      |
| Gradle         | Wrapper included | Use `gradlew.bat` / `./gradlew` |
| Docker         | 24.x             | Latest stable                   |
| Docker Compose | 2.x              | Latest stable                   |
| MySQL          | 8.0              | Docker Compose service          |
| Redis          | 7.x              | Docker Compose service          |

## Local Setup

### Option 1: Run with Docker Compose

Docker Compose hiện chạy `mysql`, `redis` và `backend`. Frontend chạy riêng trong `topviec-fe`.

Lưu ý: service `backend` trong `docker-compose.yml` đang chạy với `SPRING_PROFILE=prod`, còn prod profile dùng `ddl-auto=validate`. Vì vậy database cần có schema sẵn. Nếu muốn Hibernate tự tạo/cập nhật schema khi phát triển local, dùng Option 2 với `SPRING_PROFILE=dev`.

```powershell
cd topviec-be
Copy-Item .env.docker.example .env
Copy-Item .env.docker.example .env.docker
```

Cập nhật tối thiểu các biến trong `.env` và `.env.docker`:

```env
BACKEND_ENV_FILE=.env.docker
DB_ROOT_PASSWORD=your_root_password
DB_USERNAME=topviec_user
DB_PASSWORD=your_db_password
REDIS_PASSWORD=your_redis_password
JWT_SECRET=your_very_long_jwt_secret_key_min_32_chars
MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=your_gmail_app_password
APP_BASE_URL=http://localhost:5173
STORAGE_BASE_URL=http://localhost:8080/api/v1/files
VNPAY_TMN_CODE=your_vnpay_code
VNPAY_HASH_SECRET=your_vnpay_secret
```

Chạy stack:

```powershell
docker compose up -d
```

URL sau khi chạy:

- Backend API: `http://localhost:8080/api/v1`
- MySQL: `localhost:3307`
- Redis: `localhost:6379`

### Option 2: Run Manually for Development

Chạy database/cache bằng Docker:

```powershell
cd topviec-be
Copy-Item .env.docker.example .env
docker compose up -d mysql redis
```

Tạo hoặc cập nhật `topviec-be/.env`:

```env
SPRING_PROFILE=dev
DB_URL=jdbc:mysql://localhost:3307/topviec_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=topviec_user
DB_PASSWORD=your_db_password

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

JWT_SECRET=your_very_long_jwt_secret_key_min_32_chars
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=your_gmail_app_password

APP_BASE_URL=http://localhost:5173
APP_VERIFY_EMAIL_URL=http://localhost:5173/verify-email/callback
APP_RESET_PASSWORD_URL=http://localhost:5173/reset-password
APP_CONFIRM_INTERVIEW_URL=http://localhost:5173/interview-confirm-update
APP_SLOT_SELECTION_URL=http://localhost:5173/interview-select-slot
APP_TALENT_POOL_INVITE_URL=http://localhost:5173/talent-pool-invite
CORS_ALLOWED_ORIGINS=http://localhost:5173

STORAGE_BASE_URL=http://localhost:8080/api/v1/files

VNPAY_TMN_CODE=your_vnpay_code
VNPAY_HASH_SECRET=your_vnpay_secret
VNPAY_RETURN_URL=http://localhost:8080/api/v1/payment/vnpay/return
VNPAY_FRONTEND_RETURN_URL=http://localhost:5173/payment/result
```

Chạy backend:

```powershell
.\gradlew.bat bootRun
```

## Environment Configuration

Backend đọc profile từ `SPRING_PROFILE`. Cấu hình chính nằm ở:

- `src/main/resources/application.yaml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`
- `.env.example`
- `.env.docker.example`

| Variable                       | Required | Meaning                                   |
| ------------------------------ | -------: | ----------------------------------------- |
| `SPRING_PROFILE`               |      Yes | `dev` hoặc `prod`                         |
| `DB_URL`                       |      Yes | JDBC URL tới MySQL                        |
| `DB_USERNAME`                  |      Yes | Database username                         |
| `DB_PASSWORD`                  |      Yes | Database password                         |
| `REDIS_HOST`                   |      Yes | Redis host                                |
| `REDIS_PORT`                   |      Yes | Redis port                                |
| `REDIS_PASSWORD`               | Optional | Redis password                            |
| `JWT_SECRET`                   |      Yes | Secret ký JWT, tối thiểu 32 ký tự         |
| `JWT_ACCESS_TOKEN_EXPIRATION`  | Optional | Access token TTL, default `900000` ms     |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Optional | Refresh token TTL, default `604800000` ms |
| `MAIL_USERNAME`                |      Yes | SMTP username                             |
| `MAIL_PASSWORD`                |      Yes | SMTP app password                         |
| `APP_BASE_URL`                 |      Yes | Frontend base URL                         |
| `APP_VERIFY_EMAIL_URL`         |      Yes | Email verification callback               |
| `APP_RESET_PASSWORD_URL`       |      Yes | Reset password URL                        |
| `APP_CONFIRM_INTERVIEW_URL`    |      Yes | Interview update confirmation URL         |
| `APP_SLOT_SELECTION_URL`       |      Yes | Interview slot selection URL              |
| `APP_TALENT_POOL_INVITE_URL`   |      Yes | Talent Pool invite URL                    |
| `CORS_ALLOWED_ORIGINS`         | Optional | Allowed frontend origins                  |
| `UPLOAD_DIR`                   | Optional | Upload directory, default `uploads`       |
| `STORAGE_BASE_URL`             |      Yes | Public file base URL                      |
| `VNPAY_TMN_CODE`               |      Yes | VNPAY merchant code                       |
| `VNPAY_HASH_SECRET`            |      Yes | VNPAY signing secret                      |
| `VNPAY_PAY_URL`                | Optional | VNPAY payment endpoint                    |
| `VNPAY_API_URL`                | Optional | VNPAY transaction API                     |
| `VNPAY_RETURN_URL`             |      Yes | Backend payment return URL                |
| `VNPAY_FRONTEND_RETURN_URL`    |      Yes | Frontend payment result URL               |

## Folder Structure

```text
topviec-be/
├── src/main/java/com/topviec/topviec_be/
│   ├── annotation/
│   ├── aspect/
│   ├── config/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── enums/
│   ├── exception/
│   ├── repository/
│   ├── scheduler/
│   ├── security/
│   ├── service/
│   ├── specification/
│   └── util/
├── src/main/resources/
│   ├── application.yaml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── templates/
├── Dockerfile
├── docker-compose.yml
├── build.gradle
└── gradle/wrapper/
```

Package convention:

- `controller`: REST endpoint.
- `service` và `service/impl`: nghiệp vụ chính.
- `repository`: Spring Data JPA repository.
- `entity`: mapping bảng database.
- `dto`: request/response DTO.
- `security`: JWT, user details, permission evaluator.
- `config`: security, CORS, Redis, storage, Jackson, data seed.
- `scheduler`: background jobs.
- `aspect`: audit/business logging aspect.

## API Documentation

Hiện tại dự án không sử dụng Swagger UI làm tài liệu API chính. Có thể tham khảo source controller trong `src/main/java/com/topviec/topviec_be/controller`.

Base URL local:

```text
http://localhost:8080/api/v1
```

| Group                     | Prefix                                                         |
| ------------------------- | -------------------------------------------------------------- |
| Auth                      | `/auth`                                                        |
| Public jobs               | `/job-postings`                                                |
| Public companies          | `/companies`                                                   |
| Master data               | `/industries`, `/levels`, `/locations`, `/skills`              |
| Candidate profile         | `/candidate/profile`                                           |
| Candidate applications    | `/applications`                                                |
| Candidate CV              | `/cvs`                                                         |
| Candidate saved jobs      | `/saved-jobs`                                                  |
| Candidate reports         | `/candidate/reports`                                           |
| Candidate company follows | `/candidate/companies`                                         |
| Interview schedules       | `/interview-schedules`                                         |
| Talent Pool invite        | `/talent-pool-invite`                                          |
| Employer dashboard        | `/employer/dashboard`                                          |
| Employer company/profile  | `/employer/company`, `/employer/profile`                       |
| Employer jobs             | `/employer/job-postings`                                       |
| Employer assignment       | `/employer/job-post-assignments`                               |
| Employer applications     | `/employer/applications`                                       |
| Employer interviews       | `/employer/interviews`                                         |
| Employer team and roles   | `/employer/member`, `/employer/roles`                          |
| Employer services/orders  | `/employer/services`, `/employer/packages`, `/employer/orders` |
| Employer Talent Pool      | `/employer/talent-pool`                                        |
| Employer reports/logs     | `/employer/complaints`, `/employer/logs`                       |
| Admin                     | `/admin/**`                                                    |
| Uploads                   | `/files/upload`, `/files/**`                                   |
| Payment                   | `/payment/vnpay/**`                                            |

Auth flow:

1. Login bằng `POST /auth/login`.
2. Client lưu access token và gửi header `Authorization: Bearer <token>`.
3. Refresh token được xử lý qua cookie HttpOnly và endpoint `POST /auth/refresh`.

## Database

Database chính là MySQL.

Dev profile:

```yaml
spring.jpa.hibernate.ddl-auto: update
```

Prod profile:

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

Repo chưa dùng Flyway hoặc Liquibase. Migration schema hiện phụ thuộc vào Hibernate trong môi trường dev và schema đã chuẩn bị sẵn ở production.

Các nhóm bảng chính:

| Group              | Main tables                                                                                        |
| ------------------ | -------------------------------------------------------------------------------------------------- |
| Users/auth         | `users`, `auth_accounts`, `user_sessions`, `admin_users`                                           |
| Candidate          | `candidate_profiles`, `cvs`                                                                        |
| Company/employer   | `companies`, `company_members`, `role_defaults`, `permission_change_logs`                          |
| Jobs               | `job_postings`, `job_post_locations`, `job_post_skills`, `job_post_assignments`                    |
| Applications       | `applications`, CV snapshot JSON                                                                   |
| Interviews         | `interview_rounds`, `interviews`, `interview_slots`, `interview_results`                           |
| Services/orders    | `service_packages`, `services`, `addon_services`, `orders`, `order_items`, `company_subscriptions` |
| Moderation/reports | `complaints`, `complaint_appeals`, `complaint_evidence`, `violation_logs`                          |
| Logs               | `audit_logs`, `business_event_logs`                                                                |

Seed data hiện có trong `DataInitializer`:

| Role                | Email                   | Password |
| ------------------- | ----------------------- | -------- |
| `super_admin`       | `superadmin@topviec.vn` | `123456` |
| `content_moderator` | `moderator@topviec.vn`  | `123456` |
| `support_admin`     | `support@topviec.vn`    | `123456` |
| `finance_admin`     | `finance@topviec.vn`    | `123456` |

## File Storage

File upload được lưu local qua `FileStorageService`.

```text
uploads/
├── files/cvs/user_{userId}/
└── images/
    ├── avatars/user_{userId}/
    ├── company-logos/company_{companyId}/
    ├── company-covers/company_{companyId}/
    └── business-licenses/company_{companyId}/
```

Upload endpoint:

```text
POST /api/v1/files/upload
```

Form data:

- `file`: file upload.
- `type`: `CV`, `AVATAR`, `COMPANY_LOGO`, `COMPANY_COVER`, `BUSINESS_LICENSE`.

Public file URL:

```text
GET /api/v1/files/**
```

## Common Commands

```powershell
cd topviec-be

# Run backend locally
.\gradlew.bat bootRun

# Build production JAR
.\gradlew.bat bootJar -x test

# Full Gradle build
.\gradlew.bat build

# Run tests
.\gradlew.bat test
```

Lưu ý: task `test` đang bị disable trong `build.gradle`.

Docker:

```powershell
cd topviec-be

# Start all compose services
docker compose up -d

# Start only database and Redis
docker compose up -d mysql redis

# View backend logs
docker compose logs -f backend

# Stop services
docker compose down

# Rebuild and restart
docker compose up -d --build
```

Cảnh báo: lệnh dưới đây sẽ xóa database, Redis data và uploads volume.

```powershell
docker compose down -v
```

## Docker Compose Services

| Service   |        Port | Description               |
| --------- | ----------: | ------------------------- |
| `mysql`   | `3307:3306` | MySQL 8.0 database        |
| `redis`   | `6379:6379` | Redis 7 cache/token store |
| `backend` | `8080:8080` | Spring Boot API           |

## CI/CD

Workflow: `.github/workflows/deploy-be.yml`

- Trigger: push vào `main` hoặc `develop`.
- Setup Java 21.
- Build JAR bằng `./gradlew bootJar -x test`.
- Build và push Docker image:
  - `${DOCKER_USERNAME}/topviec-be:latest`
  - `${DOCKER_USERNAME}/topviec-be:${github.sha}`
- SSH vào EC2 và chạy `docker compose up -d backend`.
