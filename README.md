## 쇼핑몰 백엔드 과제 안내서

이 저장소는 **백엔드 입문자**를 위한 쇼핑몰 백엔드 과제 템플릿입니다.  
과제는 **3단계**로 구성되어 있으며, 단계별로 요구사항이 점점 어려워집니다.

---

## 0. 공통 규칙

- **기술 스택**
  - **Java 17+**
  - **Spring Boot** (Spring Web, Spring Data JPA, Spring Security)
  - DB: **PostgreSQL**, **Redis**
- **API 공통 규칙**
  - 모든 API는 **`/api/v1/**` 형태의 엔드포인트 사용
  - 모든 API 응답은 **공통 Response 객체(`Response<T>`)** 를 사용해야 합니다.
  - `Response<T>` 의 JSON 구조는 다음과 같습니다.

    ```json
    {
      "status": "SUCCESS 또는 ERROR 등 상태 문자열",
      "data": {
        "...": "실제 비즈니스 응답 페이로드(제네릭 T)"
      }
    }
    ```

- **Soft Delete 정책**
  - 실제 DB 삭제(DELETE 쿼리)를 날리지 않고, 예를 들어 아래와 같은 필드를 두고 논리 삭제 처리합니다.
    - `deletedAt` (nullable `LocalDateTime`)
    - 또는 `isDeleted` (boolean)
  - **조회 시에는 Soft Delete 된 데이터는 보이지 않도록 처리**해야 합니다.
- **에러 응답 규칙 (예시)**
  - Validation, 인증/인가, 비즈니스 에러 등은 아래와 같이 응답하는 것을 권장합니다.

    ```json
    {
      "status": "ERROR",
      "data": {
        "code": "VALIDATION_ERROR",
        "message": "필수 값이 비어 있습니다."
      }
    }
    ```

- **레이어드 아키텍처 권장**
  - `controller` / `service` / `repository` / `domain` 등으로 패키지를 분리해서 구현해보세요.

- **실행 방법 문서화**
  - 이 프로젝트를 실행하기 위한 최소한의 설정과 명령어를 `README.md` 최하단에 간단히 정리해주세요.

---

## 1단계: 기본 기능 (상품 CRUD + 회원/로그인 + Validation + 페이지네이션)

1단계의 목표는 **기본적인 REST API 설계와 JPA 기반 CRUD 구현, Validation, 페이지네이션, 로그인**을 경험하는 것입니다.

### 1-1. 도메인 설계

- **상품 (`Product`)**
  - **필수 필드 예시**
    - `id` (Long, PK)
    - `status` (enum: `PENDING`, `APPROVED` — 기본값 `PENDING`, 관리자 승인 전까지는 `APPROVED` 가 될 수 없음)
    - `name` (String, 필수, 길이 제한)
    - `price` (Long 또는 Integer, 0 이상)
    - `stock` (Integer, 0 이상)
    - `description` (String, 옵션)
    - `createdAt`, `updatedAt`
    - `deletedAt` 또는 `isDeleted` (Soft Delete 용)

- **회원 (`User`)**
  - **필수 필드 예시**
    - `id` (Long, PK)
    - `email` (String, unique, 필수)
    - `password` (String, 필수, **암호화 저장 필수**)
    - `name` (String)
    - `role` (enum: `USER`, `ADMIN`)  → 2단계에서 사용
    - `createdAt`, `updatedAt`

필드는 예시이며, 필요에 따라 자유롭게 확장해도 됩니다. 다만 **Soft Delete 필드와 Role 필드는 꼭 포함**해주세요.

### 1-2. 상품 관련 API 요구사항

#### 상품 등록
- **HTTP Method / Path**
  - `POST /api/v1/products`
- **요청 (JSON)**
  - `name` (String, 2~50자, 필수)
  - `price` (Number, 필수, 0 이상)
  - `stock` (Number, 필수, 0 이상)
  - `description` (String, 옵션)
- **응답**
  - 타입: `Response<ProductResponse>`
  - `ProductResponse`
    - `id`
    - `status` (등록 시점에는 항상 `PENDING`)
    - `name`
    - `price`
    - `stock`
    - `description`
    - `createdAt`, `updatedAt`
- **요구사항**
  - Bean Validation 사용 (예: `@NotBlank`, `@Min`, `@Size` 등)
  - 생성 직후 `status` 는 반드시 `PENDING`
  - `APPROVED` 되기 전까지는 일반 사용자 상품 목록/단건 조회에 노출되지 않음

#### 상품 목록 조회 (페이지네이션 + 검색)
- **HTTP Method / Path**
  - `GET /api/v1/products`
- **Query Parameter**
  - `page` (Integer, 기본값 0) - 페이지 번호 (0부터 시작)
  - `size` (Integer, 기본값 10) - 페이지당 항목 수
  - `minPrice` (Long, 옵션) - 최소 가격 필터
  - `maxPrice` (Long, 옵션) - 최대 가격 필터
  - `name` (String, 옵션) - 상품명 부분 검색 (LIKE 검색)
- **요청 예시**
  ```
  GET /api/v1/products?page=0&size=10&minPrice=1000&maxPrice=50000&name=노트북
  ```
- **응답**
  - 타입: `Response<ProductPageResponse>`
  - `ProductPageResponse`
    - `content`: `ProductSummary[]`
    - `totalElements`: 전체 개수
    - `totalPages`: 전체 페이지 수
    - `page`: 현재 페이지
    - `size`: 페이지 크기
  - `ProductSummary`
    - `id`
    - `name`
    - `price`
    - `stock`
    - `status`
- **요구사항**
  - Soft Delete 된 상품은 조회되면 안 됨
  - 일반 사용자 조회 시 `status = APPROVED` 인 상품만 조회
  - `minPrice`와 `maxPrice`를 함께 사용하면 가격 범위 검색 가능
  - `name` 파라미터는 부분 일치 검색 (대소문자 구분 없음 권장)

#### 상품 단건 조회
- **HTTP Method / Path**
  - `GET /api/v1/products/{id}`
- **응답**
  - 타입: `Response<ProductDetailResponse>`
  - `ProductDetailResponse`
    - `id`, `status`, `name`, `price`, `stock`, `description`, `createdAt`, `updatedAt`
- **요구사항**
  - Soft Delete 된 상품은 조회 시 404 또는 적절한 에러 응답
  - 일반 사용자는 `status = APPROVED` 인 상품만 조회 가능 (`ADMIN` 의 조회 범위는 2단계에서 정의)

#### 상품 수정
- **HTTP Method / Path**
  - `PUT /api/v1/products/{id}`
- **요청 (JSON)**
  - `name` (String, 2~50자, 필수)
  - `price` (Number, 필수, 0 이상)
  - `stock` (Number, 필수, 0 이상)
  - `description` (String, 옵션)
- **응답**
  - 타입: `Response<ProductResponse>`
- **요구사항**
  - 위 필드들을 업데이트할 수 있어야 함
  - Validation 적용
  - Soft Delete 된 상품은 수정 불가 → 에러 응답

#### 상품 삭제 (Soft Delete)
- **HTTP Method / Path**
  - `DELETE /api/v1/products/{id}`
- **응답**
  - 타입: `Response<Void>` 또는 비어 있는 `data` 를 가진 `Response<Object>` 등 프로젝트에서 명확히 정의
- **요구사항**
  - 실제 삭제 대신 Soft Delete 필드(`deletedAt` 또는 `isDeleted`)를 변경
  - 이후 목록/단건 조회에서 보이지 않아야 함

### 1-3. 회원 가입 / 로그인 / 인증

#### 회원 가입
- **HTTP Method / Path**
  - `POST /api/v1/auth/signup`
- **요청 (JSON)**
  - `email` (String, 이메일 형식, 필수)
  - `password` (String, 8~12자, 특수문자/숫자/대문자 포함, 필수)
  - `name` (String, 2~12자 필수)
- **응답**
  - 타입: `Response<UserResponse>`
  - `UserResponse`
    - `id`, `email`, `name`, `role`, `createdAt`, `updatedAt`
- **요구사항**
  - `email` 은 unique
  - 비밀번호는 **반드시 암호화** 해서 저장

#### 로그인
- **HTTP Method / Path**
  - `POST /api/v1/auth/login`
- **요청 (JSON)**
  - `email` (String, 이메일 형식, 필수)
  - `password` (String, 필수)
- **응답**
  - 타입: `Response<LoginResponse>`
  - `LoginResponse`
    - `accessToken`
    - (선택) `refreshToken` — 3단계에서 확장 가능
- **요구사항**
  - `email`, `password` 로 인증
  - 성공 시 Access Token 발급
  - 이후 인증이 필요한 API 호출 시 헤더 사용  
    - `Authorization: Bearer <access_token>`

---

## 2단계: Role 기반 접근 제어 + 주문 도메인 + 상태 머신

2단계의 목표는 **Role 기반 권한 제어, 자기 데이터만 조회하기, enum 기반 상태 머신**을 경험하는 것입니다.

### 2-1. Role 기반 접근 제어

- **Role 정의**
  - `USER`: 일반 사용자
  - `ADMIN`: 관리자

- **접근 제어 정책 예시**
  - **`ADMIN`**
    - 상품 생성 / 수정 / 삭제 / 승인 가능
  - **`USER`**
    - 승인 완료(`APPROVED`) 상품 조회만 가능 (등록/수정/삭제/승인 불가)

### 2-2. 주문 도메인 설계

- **주문 (`Order`)**
  - **필드 예시**
    - `id`
    - `user` (주문한 사용자)
    - `status` (enum: `CREATED`, `PAID`, `CANCELLED`, `COMPLETED` 등)
    - `totalPrice`
    - `createdAt`, `updatedAt`

- **주문상품 (`OrderItem`)**
  - **필드 예시**
    - `id`
    - `order` (Order)
    - `product` (Product)
    - `price` (주문 시점의 상품 가격 스냅샷)
    - `quantity`

### 2-3. 간단한 enum 기반 상태 머신

- **상태 전이 규칙 예시**
  - `CREATED` → `PAID` → `COMPLETED`
  - `CREATED` → `CANCELLED`
  - 이미 `CANCELLED` 또는 `COMPLETED` 된 주문은 더 이상 상태 변경 불가.

- **상태 변경 API 예시**
  - `PATCH /api/v1/orders/{id}/pay` → `CREATED` → `PAID`
  - `PATCH /api/v1/orders/{id}/cancel` → `CREATED` → `CANCELLED`
  - `PATCH /api/v1/orders/{id}/complete` → `PAID` → `COMPLETED`

- **요구사항**
  - 잘못된 상태 전이 시 공통 에러 Response 구조로 응답해야 합니다.
    - 예: `status: "ERROR"`, `data: { "code": "INVALID_ORDER_STATUS", "message": "이미 완료된 주문입니다." }`

### 2-4. 주문 관련 API

- **주문 생성**
  - **HTTP Method / Path**
    - `POST /api/v1/orders`
  - **요청 예시**

    ```json
    {
      "items": [
        { "productId": 1, "quantity": 2 },
        { "productId": 3, "quantity": 1 }
      ]
    }
    ```

  - **요구사항**
    - 로그인한 사용자 기준으로 주문을 생성한다.
    - 각 상품의 재고를 검증한다. (재고 부족 시 에러 응답)
    - 주문 생성 시 재고를 차감한다. (동시성 문제는 3단계에서 다룹니다.)

- **내 주문 목록 조회**
  - **HTTP Method / Path**
    - `GET /api/v1/orders/my`
  - **요구사항**
    - 로그인한 사용자의 주문만 조회한다.
    - 페이지네이션 및 상태 필터(optional: `status=CREATED` 등)를 지원하면 좋습니다.

- **주문 상세 조회**
  - **HTTP Method / Path**
    - `GET /api/v1/orders/{id}`
  - **요구사항**
    - 일반 사용자는 **본인 주문만** 조회할 수 있다.
    - `ADMIN`은 모든 사용자의 주문을 조회할 수 있다.

### 2-5. 상품 승인 API

#### 상품 승인
- **HTTP Method / Path**
  - `PATCH /api/v1/products/{id}/approve`
- **요청**
  - (선택) 승인 사유 등 부가 정보 — 필드는 자유롭게 설계
- **응답**
  - 타입: `Response<ProductResponse>`
  - `ProductResponse`
    - `id`, `status`, `name`, `price`, `stock`, `description`, `createdAt`, `updatedAt`
- **요구사항**
  - `status = PENDING` 인 상품만 승인 가능 (그 외 상태에서 호출 시 에러)
  - 승인 성공 시 `status = APPROVED` 로 변경
  - 승인 이후부터 일반 사용자 상품 목록/단건 조회에 노출
  - 이 API는 `ADMIN` Role 에서만 호출 가능

---

## 3단계: 고급 주제 (동시성, Refresh Token, 이미지 업로드)

3단계는 **시간/실력에 따라 선택적으로 구현**해도 됩니다.  
다만, **동시성 관련 내용은 꼭 한 번 경험**해보는 것을 추천합니다.

### 3-1. Refresh Token (선택)

- **목표**
  - Access Token의 만료 시간을 짧게 설정한다.
  - Refresh Token으로 Access Token을 재발급하는 전체 흐름을 스스로 설계하고 구현해본다.

- **기본 요구사항**
  - `POST /api/v1/auth/refresh` 엔드포인트를 하나 정의한다.
    - 유효한 Refresh Token을 보내면 새로운 Access Token을 발급한다.
  - Refresh Token을 어디에, 어떤 형식으로 저장/관리할지는 직접 설계한다.
  - 로그아웃 시 Refresh Token을 어떻게 무효화할지에 대한 정책을 정하고 구현한다.

### 3-2. 동시성 문제 / Race Condition 해결 (선택)

- **시나리오 1: 재고 차감 동시성**
  - 재고가 1개 남은 인기 상품에 두 명 이상이 동시에 주문을 넣었을 때,
  - 재고가 0 아래로 내려가거나, 실제 재고보다 많이 팔리는 문제가 발생하지 않도록 해야 한다.

- **시나리오 2: 상품 등록/승인 동시성**
  - 동일한 비즈니스 키(예: 상품명 등)에 대해 여러 클라이언트가 동시에 상품 등록 요청을 보내는 상황을 가정한다.
  - 관리자 승인 흐름(상품 상태 `PENDING` → `APPROVED`)과 함께 고려했을 때,
    - 중복 상품이 과도하게 생성되거나,
    - 승인되지 말아야 할 상품이 노출되는 등의 문제가 발생하지 않도록 해야 한다.
  - 여러 개의 상품 등록 요청과 승인 API (`PATCH /api/v1/products/{id}/approve`) 호출이 섞여 들어오는 상황을 테스트해본다.

- **필수 요구**
  - 위 두 시나리오 모두에 대해, 동시성 문제가 발생하는 상황을 재현할 수 있는 간단한 테스트 코드나 설명을 남긴다.
  - 각 시나리오에서 동시성 문제를 어떻게 해결할지 스스로 조사하여 하나 이상 선택하고 적용한다.
  - 해결 전/후에 대해 `README` 에 간단히 설명을 남긴다.

### 3-3. Multipart 파일 업로드 - 상품 이미지 (선택)

- **목표**
  - Spring의 `MultipartFile`을 사용하여 이미지 파일을 업로드하고 관리하는 기능을 구현해본다.
  - 파일 저장 방식(로컬 파일 시스템, 클라우드 스토리지 등)을 선택하고 구현한다.

- **기본 요구사항**
  - `POST /api/v1/products/{id}/images` 엔드포인트를 정의한다.
    - `multipart/form-data` 형식으로 이미지 파일을 받는다.
    - 한 번에 여러 이미지를 업로드할 수 있도록 한다.
  - 업로드된 이미지 파일의 저장 위치를 결정한다.
    - 로컬 파일 시스템에 저장하거나
    - 클라우드 스토리지(AWS S3, Azure Blob Storage 등)에 저장
  - 상품 엔티티에 이미지 URL 필드를 추가한다.
    - 예: `imageUrls` (List<String>) 또는 `mainImageUrl` (String)
  - 이미지 파일 검증을 구현한다.
    - 파일 형식 검증 (jpg, png, gif 등)
    - 파일 크기 제한 (예: 최대 5MB)
  - 이미지 삭제 기능도 구현한다.
    - `DELETE /api/v1/products/{id}/images/{imageId}` 또는 유사한 엔드포인트
  - 상품 조회 시 이미지 URL이 포함되어야 한다.

- **추가 고려사항**
  - 이미지 리사이징/썸네일 생성
  - 이미지 최적화
  - 파일명 중복 방지 (UUID 사용 등)

---

## 실행 방법

### 1. 인프라 실행 (Docker Compose)

프로젝트 루트 디렉토리에서 다음 명령어로 PostgreSQL과 Redis를 실행합니다:

```bash
docker-compose up -d
```

서비스 상태 확인:
```bash
docker-compose ps
```

서비스 중지:
```bash
docker-compose down
```

데이터까지 함께 삭제하려면:
```bash
docker-compose down -v
```

### 2. 애플리케이션 실행

Gradle을 사용하여 애플리케이션을 실행합니다:

```bash
# Windows
gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun
```

또는 빌드 후 실행:

```bash
# 빌드
./gradlew build

# 실행
java -jar build/libs/shopping-mall-0.0.1-SNAPSHOT.jar
```

### 3. 설정 확인

`application.properties` 파일에서 다음 설정을 확인하고 필요시 수정하세요:

- PostgreSQL 연결 정보 (기본값: localhost:5432)
- Redis 연결 정보 (기본값: localhost:6379)
- JWT Secret Key (프로덕션 환경에서는 반드시 변경 필요)

### 4. 데이터베이스 초기화

JPA의 `ddl-auto=update` 설정으로 자동으로 테이블이 생성됩니다.




