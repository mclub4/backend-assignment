## 쇼핑몰 백엔드 과제 안내서

이 저장소는 **백엔드 입문자**를 위한 쇼핑몰 백엔드 과제 템플릿입니다.  
과제는 **3단계**로 구성되어 있으며, 단계별로 요구사항이 점점 어려워집니다.

---

## 시스템 개요

### 도메인 설명

이 프로젝트는 **온라인 쇼핑몰 백엔드 시스템**을 구현합니다. 주요 도메인은 다음과 같습니다:

1. **사용자 (User) 도메인**
   - 회원 가입, 로그인, 인증/인가 관리
   - Role 기반 접근 제어 (USER, ADMIN)
   - JWT 토큰 기반 인증

2. **상품 (Product) 도메인**
   - 상품 CRUD 기능
   - 상품 승인 시스템 (PENDING → APPROVED)
   - 페이지네이션 및 검색 기능
   - Soft Delete 지원

3. **주문 (Order) 도메인**
   - 주문 생성 및 관리
   - 상태 머신 기반 주문 상태 관리 (CREATED → PAID → COMPLETED / CANCELLED)
   - 재고 관리 및 동시성 제어

### 시스템 아키텍처

- **레이어드 아키텍처**: Controller → Service → Repository → Entity
- **인증/인가**: Spring Security + JWT
- **데이터베이스**: PostgreSQL (메인 DB), Redis (캐시 등)
- **ORM**: JPA, QueryDSL(QueryDSL말고 Native Query 쓰셔도 됩니다.)
- **API 형식**: RESTful API (`/api/v1/**`)
- **DTO 매핑**: `MapStruct` 사용 시 반복 매핑 코드를 줄일 수 있습니다.
  - 예시: `@Mapper(componentModel = "spring")` 인터페이스에 `ProductMapper` 정의 후 `Product` ↔ DTO 변환 메서드 선언.

---

## 0. 공통 규칙

### 기술 스택
  - **Java 17+**
  - **Spring Boot** (Spring Web, Spring Data JPA, Spring Security)
- **DB**: PostgreSQL, Redis

### API 공통 규칙

#### 엔드포인트 규칙
- 모든 API는 **`/api/v1/**` 형태의 엔드포인트 사용**

#### Swagger / OpenAPI
- Swagger UI 제공: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI 문서: `http://localhost:8080/v3/api-docs`
- Security 설정 시 **Swagger 관련 경로는 permitAll**로 허용해야 합니다.
  - 예) `/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-resources/**` 등

#### 응답 형식
  - 모든 API 응답은 **공통 Response 객체(`Response<T>`)** 를 사용해야 합니다.
- `Response<T>` 의 JSON 구조:

    ```json
    {
      "status": "SUCCESS 또는 ERROR 등 상태 문자열",
      "data": {
        "...": "실제 비즈니스 응답 페이로드(제네릭 T)"
      }
    }
    ```

#### 인증 헤더
- 인증이 필요한 API는 다음 헤더를 포함해야 합니다:
  ```
  Authorization: Bearer <access_token>
  ```

#### 에러 응답 규칙
- Validation, 인증/인가, 비즈니스 에러 등은 아래와 같이 응답:

    ```json
    {
      "status": "ERROR",
      "data": {
        "code": "VALIDATION_ERROR",
        "message": "필수 값이 비어 있습니다."
      }
    }
    ```

### Validation 규칙

#### Bean Validation 사용
- Request Body의 필드 검증은 **Jakarta Bean Validation**을 사용합니다.
- 주요 어노테이션:
  - `@NotBlank`: null, 빈 문자열, 공백만 있는 문자열 불가
  - `@NotNull`: null 불가
  - `@Email`: 이메일 형식 검증
  - `@Size(min=, max=)`: 문자열 길이 제한
  - `@Min(value)`: 숫자 최소값 제한
  - `@Pattern(regexp=)`: 정규식 패턴 검증

#### Validation 에러 응답
- Validation 실패 시 자동으로 400 Bad Request 응답
- 에러 메시지는 `@NotBlank(message=)` 등으로 커스터마이징 가능

### Role 기반 접근 제어

#### Role 정의
- **`USER`**: 일반 사용자
- **`ADMIN`**: 관리자

#### Role별 권한 정책

**USER 권한:**
- 상품 등록 가능
- 본인이 등록한 상품만 수정 가능
- `APPROVED` 상태인 상품만 조회 가능
- 본인 주문만 조회/생성 가능
- 상품 승인 불가

**ADMIN 권한:**
- 모든 상품 조회/수정/삭제 가능
- 모든 사용자의 주문 조회 가능
- 상품 승인 가능 (`PENDING` → `APPROVED`)

### Soft Delete 정책

- 실제 DB 삭제(DELETE 쿼리)를 하지 않고, 논리 삭제 처리합니다.
- 필드 예시:
  - `deletedAt` (nullable `LocalDateTime`)
- **조회 시에는 Soft Delete 된 데이터는 보이지 않도록 처리**해야 합니다.

---

## 1단계: 기본 기능 (상품 CRUD + 회원/로그인 + Validation + 페이지네이션)

1단계의 목표는 **기본적인 REST API 설계와 JPA 기반 CRUD 구현, Validation, 페이지네이션, 로그인**을 경험하는 것입니다.

### 1-1. 도메인 설계

#### 상품 (`Product`) 엔티티
- **필수 필드**
    - `id` (Long, PK)
  - `status` (enum: `PENDING`, `APPROVED` — 기본값 `PENDING`)
  - `name` (String, 필수, 2~50자)
  - `price` (Long, 0 이상)
    - `stock` (Integer, 0 이상)
    - `description` (String, 옵션)
  - `user` (User, 상품 등록자 - ManyToOne 관계)
  - `createdAt`, `updatedAt` (LocalDateTime)
  - `deletedAt` (LocalDateTime, nullable, Soft Delete 용)

#### 회원 (`User`) 엔티티
- **필수 필드**
    - `id` (Long, PK)
    - `email` (String, unique, 필수)
    - `password` (String, 필수, **암호화 저장 필수**)
  - `name` (String, 2~12자)
  - `role` (enum: `USER`, `ADMIN`, 기본값 `USER`)
  - `createdAt`, `updatedAt` (LocalDateTime)

---

### 1-2. 인증/인가 API

#### 1-2-1. 회원 가입

**HTTP Method / Path**
```
POST /api/v1/auth/signup
```

**인증**
- 인증 불필요 (공개 API)

**Request Body (JSON)**
```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "name": "홍길동"
}
```

**Request Body Validation**
- `email` (String, 필수)
  - `@NotBlank`: 필수
  - `@Email`: 이메일 형식 검증
- `password` (String, 필수)
  - `@NotBlank`: 필수
  - `@Size(min=8, max=12)`: 8자 이상 12자 이하
  - `@Pattern`: 특수문자, 숫자, 대문자 포함 필수
- `name` (String, 필수)
  - `@NotBlank`: 필수
  - `@Size(min=2, max=12)`: 2자 이상 12자 이하

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "role": "USER",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

**Response 타입**: `Response<UserResponse>`

**비즈니스 로직**
- `email`은 unique해야 함 (중복 시 에러 응답)
- 비밀번호는 **반드시 암호화**해서 저장 (Spring Security `BCryptPasswordEncoder` 사용 권장)
- 기본 `role`은 `USER`로 설정

---

#### 1-2-2. 로그인

**HTTP Method / Path**
```
POST /api/v1/auth/login
```

**인증**
- 인증 불필요 (공개 API)

**Request Body (JSON)**
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

**Request Body Validation**
- `email` (String, 필수)
  - `@NotBlank`: 필수
  - `@Email`: 이메일 형식 검증
- `password` (String, 필수)
  - `@NotBlank`: 필수

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Response 타입**: `Response<LoginResponse>`

**비즈니스 로직**
- `email`, `password`로 인증
- 인증 성공 시 JWT Access Token 발급
- 이후 인증이 필요한 API 호출 시 헤더에 포함:
  ```
  Authorization: Bearer <access_token>
  ```

---

#### 1-2-3. 토큰 재발급 (Reissue)

**HTTP Method / Path**
```
POST /api/v1/auth/reissue
```

**인증**
- 인증 불필요 (공개 API)

**Request Body (JSON)**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Request Body Validation**
- `refreshToken` (String, 필수)
  - `@NotBlank`: 필수 (또는 구현 시점에 검증)

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Response 타입**: `Response<ReissueResponse>`

**비즈니스 로직**
- 유효한 Refresh Token을 받아 새로운 Access Token을 발급
- Refresh Token 검증 후 유효하면 새로운 Access Token 반환
- 유효하지 않은 Refresh Token이면 에러 응답

**참고**: 3단계에서 Refresh Token 저장/관리 로직을 구현합니다.

---

#### 1-2-4. 자기 자신의 정보 조회

**HTTP Method / Path**
```
GET /api/v1/users
```

**인증**
- **인증 필수** (JWT 토큰 필요)
- Role: `USER`, `ADMIN` 모두 접근 가능

**Request Body**
- 없음

**Query Parameter**
- 없음

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "role": "USER",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

**Response 타입**: `Response<UserResponse>`

**비즈니스 로직**
- JWT 토큰에서 사용자 정보를 추출하여 본인의 정보만 반환
- 인증되지 않은 사용자는 401 Unauthorized 응답

---

### 1-3. 상품 API

#### 1-3-1. 상품 등록

**HTTP Method / Path**
```
POST /api/v1/products
```

**인증**
- **인증 필수** (JWT 토큰 필요)
- Role: `USER`, `ADMIN` 모두 등록 가능

**Request Body (JSON)**
```json
{
  "name": "노트북",
  "price": 1000000,
  "stock": 10,
  "description": "고성능 노트북입니다."
}
```

**Request Body Validation**
- `name` (String, 필수)
  - `@NotBlank`: 필수
  - `@Size(min=2, max=50)`: 2자 이상 50자 이하
- `price` (Long, 필수)
  - `@NotNull`: 필수
  - `@Min(0)`: 0 이상
- `stock` (Integer, 필수)
  - `@NotNull`: 필수
  - `@Min(0)`: 0 이상
  - `description` (String, 옵션)
  - Validation 없음

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": 1,
    "status": "PENDING",
    "name": "노트북",
    "price": 1000000,
    "stock": 10,
    "description": "고성능 노트북입니다.",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

**Response 타입**: `Response<ProductResponse>`

**비즈니스 로직**
- 생성 직후 `status`는 반드시 `PENDING`
- 상품 등록 시 등록한 사용자 정보가 저장됨 (JWT 토큰에서 추출)
- **한 사용자는 PENDING 상태인 상품이 하나 이상 있으면 새로운 상품을 등록할 수 없음**
  - 기존 PENDING 상품이 승인되거나 취소되어야 새로운 상품 등록 가능
  - `APPROVED` 되기 전까지는 일반 사용자 상품 목록/단건 조회에 노출되지 않음

---

#### 1-3-2. 상품 목록 조회 (페이지네이션 + 검색)

**HTTP Method / Path**
```
GET /api/v1/products
```

**인증**
- 인증 불필요 (공개 API)
- 단, Role에 따라 조회 범위가 달라짐

**Request Body**
- 없음

**Query Parameter**
- `page` (Integer, 옵션, 기본값 0)
  - 페이지 번호 (0부터 시작)
- `size` (Integer, 옵션, 기본값 10)
  - 페이지당 항목 수
- `minPrice` (Long, 옵션)
  - 최소 가격 필터
- `maxPrice` (Long, 옵션)
  - 최대 가격 필터
- `name` (String, 옵션)
  - 상품명 부분 검색 (LIKE 검색)

**요청 예시**
  ```
  GET /api/v1/products?page=0&size=10&minPrice=1000&maxPrice=50000&name=노트북
  ```

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "노트북",
        "price": 1000000,
        "stock": 10,
        "status": "APPROVED"
      },
      {
        "id": 2,
        "name": "마우스",
        "price": 50000,
        "stock": 20,
        "status": "APPROVED"
      }
    ],
    "totalElements": 2,
    "totalPages": 1,
    "page": 0,
    "size": 10
  }
}
```

**Response 타입**: `Response<ProductPageResponse>`

**비즈니스 로직**
- Soft Delete 된 상품은 조회되지 않음
- **일반 사용자(인증 없음 또는 USER Role) 조회 시**: `status = APPROVED` 인 상품만 조회
- **ADMIN Role 조회 시**: 모든 상태의 상품 조회 가능 (PENDING 포함)
  - `minPrice`와 `maxPrice`를 함께 사용하면 가격 범위 검색 가능
  - `name` 파라미터는 부분 일치 검색 (대소문자 구분 없음 권장)

---

#### 1-3-3. 상품 단건 조회

**HTTP Method / Path**
```
GET /api/v1/products/{id}
```

**인증**
- 인증 불필요 (공개 API)
- 단, Role에 따라 조회 범위가 달라짐

**Path Parameter**
- `id` (Long, 필수)
  - 상품 ID

**Request Body**
- 없음

**Query Parameter**
- 없음

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": 1,
    "status": "APPROVED",
    "name": "노트북",
    "price": 1000000,
    "stock": 10,
    "description": "고성능 노트북입니다.",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

**Response 타입**: `Response<ProductDetailResponse>`

**비즈니스 로직**
  - Soft Delete 된 상품은 조회 시 404 또는 적절한 에러 응답
- **일반 사용자(인증 없음 또는 USER Role) 조회 시**: `status = APPROVED` 인 상품만 조회 가능
- **ADMIN Role 조회 시**: 모든 상태의 상품 조회 가능 (PENDING 포함)

---

#### 1-3-4. 상품 수정

**HTTP Method / Path**
```
PUT /api/v1/products/{id}
```

**인증**
- **인증 필수** (JWT 토큰 필요)
- Role: `USER` (본인 상품만), `ADMIN` (모든 상품)

**Path Parameter**
- `id` (Long, 필수)
  - 상품 ID

**Request Body (JSON)**
```json
{
  "name": "노트북 (수정)",
  "price": 1200000,
  "stock": 15,
  "description": "수정된 설명입니다."
}
```

**Request Body Validation**
- `name` (String, 필수)
  - `@NotBlank`: 필수
  - `@Size(min=2, max=50)`: 2자 이상 50자 이하
- `price` (Long, 필수)
  - `@NotNull`: 필수
  - `@Min(0)`: 0 이상
- `stock` (Integer, 필수)
  - `@NotNull`: 필수
  - `@Min(0)`: 0 이상
  - `description` (String, 옵션)
  - Validation 없음

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": 1,
    "status": "PENDING",
    "name": "노트북 (수정)",
    "price": 1200000,
    "stock": 15,
    "description": "수정된 설명입니다.",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T01:00:00"
  }
}
```

**Response 타입**: `Response<ProductResponse>`

**비즈니스 로직**
  - 위 필드들을 업데이트할 수 있어야 함
  - Soft Delete 된 상품은 수정 불가 → 에러 응답
- **USER Role**: 본인이 등록한 상품이 아니면 수정 불가 → 403 Forbidden 에러 응답
- **ADMIN Role**: 모든 상품 수정 가능

---

#### 1-3-5. 상품 삭제 (Soft Delete)

**HTTP Method / Path**
```
DELETE /api/v1/products/{id}
```

**인증**
- **인증 필수** (JWT 토큰 필요)
- Role: `USER` (본인 상품만), `ADMIN` (모든 상품)

**Path Parameter**
- `id` (Long, 필수)
  - 상품 ID

**Request Body**
- 없음

**Query Parameter**
- 없음

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": null
}
```

**Response 타입**: `Response<Void>`

**비즈니스 로직**
- 실제 삭제 대신 Soft Delete 필드(`deletedAt` 또는 `isDeleted`)를 변경
- 이후 목록/단건 조회에서 보이지 않아야 함
- **USER Role**: 본인이 등록한 상품이 아니면 삭제 불가 → 403 Forbidden 에러 응답
- **ADMIN Role**: 모든 상품 삭제 가능

---

## 2단계: Role 기반 접근 제어 + 주문 도메인 + 상태 머신

2단계의 목표는 **Role 기반 권한 제어, 자기 데이터만 조회하기, enum 기반 상태 머신**을 경험하는 것입니다.

### 2-1. 주문 도메인 설계

#### 주문 (`Order`) 엔티티
- **필수 필드**
  - `id` (Long, PK)
  - `user` (User, 주문한 사용자 - ManyToOne 관계)
  - `status` (enum: `CREATED`, `PAID`, `CANCELLED`, `COMPLETED`)
  - `totalPrice` (Long, 주문 총액)
  - `orderItems` (List<OrderItem>, OneToMany 관계)
  - `createdAt`, `updatedAt` (LocalDateTime)

#### 주문상품 (`OrderItem`) 엔티티
- **필수 필드**
  - `id` (Long, PK)
  - `order` (Order, ManyToOne 관계)
  - `product` (Product, ManyToOne 관계)
  - `price` (Long, 주문 시점의 상품 가격 스냅샷)
  - `quantity` (Integer, 주문 수량)

### 2-2. 주문 상태 머신

#### 상태 전이 규칙
  - `CREATED` → `PAID` → `COMPLETED`
  - `CREATED` → `CANCELLED`
- 이미 `CANCELLED` 또는 `COMPLETED` 된 주문은 더 이상 상태 변경 불가

#### 상태별 설명
- **`CREATED`**: 주문 생성 완료 (결제 대기)
- **`PAID`**: 결제 완료 (배송 대기)
- **`CANCELLED`**: 주문 취소됨
- **`COMPLETED`**: 주문 완료 (배송 완료)

---

### 2-3. 주문 API

#### 2-3-1. 주문 생성

**HTTP Method / Path**
```
POST /api/v1/orders
```

**인증**
- **인증 필수** (JWT 토큰 필요)
- Role: `USER`, `ADMIN` 모두 주문 생성 가능

**Request Body (JSON)**
    ```json
    {
      "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 3,
      "quantity": 1
    }
      ]
    }
    ```

**Request Body Validation**
- `items` (List<OrderItemRequest>, 필수)
  - `@NotEmpty`: 최소 1개 이상의 주문 항목 필요
- `items[].productId` (Long, 필수)
  - `@NotNull`: 필수
- `items[].quantity` (Integer, 필수)
  - `@NotNull`: 필수
  - `@Min(1)`: 1 이상

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": 1,
    "userId": 1,
    "status": "CREATED",
    "totalPrice": 2050000,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

**Response 타입**: `Response<OrderResponse>`

**비즈니스 로직**
- 로그인한 사용자 기준으로 주문을 생성 (JWT 토큰에서 사용자 정보 추출)
- 각 상품의 재고를 검증 (재고 부족 시 에러 응답)
- 주문 생성 시 재고를 차감 (동시성 문제는 3단계에서 다룹니다)
- 주문 시점의 상품 가격을 `OrderItem.price`에 스냅샷으로 저장
- 주문 총액(`totalPrice`) 계산: 각 `OrderItem.price * quantity`의 합
- 주문 생성 시 `status`는 `CREATED`로 설정
- Soft Delete 된 상품은 주문 불가 → 에러 응답
- `APPROVED` 상태가 아닌 상품은 주문 불가 → 에러 응답

---

#### 2-3-2. 내 주문 목록 조회

**HTTP Method / Path**
```
GET /api/v1/orders/my
```

**인증**
- **인증 필수** (JWT 토큰 필요)
- Role: `USER`, `ADMIN` 모두 접근 가능

**Request Body**
- 없음

**Query Parameter**
- `page` (Integer, 옵션, 기본값 0)
  - 페이지 번호 (0부터 시작)
- `size` (Integer, 옵션, 기본값 10)
  - 페이지당 항목 수
- `status` (String, 옵션)
  - 주문 상태 필터 (`CREATED`, `PAID`, `CANCELLED`, `COMPLETED`)

**요청 예시**
```
GET /api/v1/orders/my?page=0&size=10&status=CREATED
```

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "content": [
      {
        "id": 1,
        "userId": 1,
        "status": "CREATED",
        "totalPrice": 2050000,
        "createdAt": "2024-01-01T00:00:00",
        "updatedAt": "2024-01-01T00:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "page": 0,
    "size": 10
  }
}
```

**Response 타입**: `Response<Page<OrderResponse>>` (Spring Data의 Page 객체 사용)

**비즈니스 로직**
- 로그인한 사용자의 주문만 조회 (JWT 토큰에서 사용자 정보 추출)
- `status` 파라미터가 있으면 해당 상태의 주문만 필터링
- 페이지네이션 지원

---

#### 2-3-3. 주문 상세 조회

**HTTP Method / Path**
```
GET /api/v1/orders/{id}
```

**인증**
- **인증 필수** (JWT 토큰 필요)
- Role: `USER` (본인 주문만), `ADMIN` (모든 주문)

**Path Parameter**
- `id` (Long, 필수)
  - 주문 ID

**Request Body**
- 없음

**Query Parameter**
- 없음

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": 1,
    "userId": 1,
    "status": "CREATED",
    "totalPrice": 2050000,
    "items": [
      {
        "id": 1,
        "productId": 1,
        "productName": "노트북",
        "price": 1000000,
        "quantity": 2
      },
      {
        "id": 2,
        "productId": 3,
        "productName": "마우스",
        "price": 50000,
        "quantity": 1
      }
    ],
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

**Response 타입**: `Response<OrderDetailResponse>`

**비즈니스 로직**
- **USER Role**: 본인 주문만 조회 가능 (본인 주문이 아니면 403 Forbidden 에러 응답)
- **ADMIN Role**: 모든 사용자의 주문을 조회 가능
- 주문 상세 정보와 함께 주문 항목(`items`) 목록도 포함

---

#### 2-3-4. 주문 결제

**HTTP Method / Path**
```
PATCH /api/v1/orders/{id}/pay
```

**인증**
- **인증 필수** (JWT 토큰 필요)
- Role: `USER` (본인 주문만), `ADMIN` (모든 주문)

**Path Parameter**
- `id` (Long, 필수)
  - 주문 ID

**Request Body**
- 없음

**Query Parameter**
- 없음

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": 1,
    "userId": 1,
    "status": "PAID",
    "totalPrice": 2050000,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T01:00:00"
  }
}
```

**Response 타입**: `Response<OrderResponse>`

**비즈니스 로직**
- 상태 전이: `CREATED` → `PAID`
- **USER Role**: 본인 주문만 결제 가능 (본인 주문이 아니면 403 Forbidden 에러 응답)
- **ADMIN Role**: 모든 주문 결제 가능
- 잘못된 상태 전이 시 에러 응답:
  - 현재 상태가 `CREATED`가 아니면 → `status: "ERROR"`, `data: { "code": "INVALID_ORDER_STATUS", "message": "결제할 수 없는 주문 상태입니다." }`
- 이미 `CANCELLED` 또는 `COMPLETED` 된 주문은 결제 불가

---

#### 2-3-5. 주문 취소

**HTTP Method / Path**
```
PATCH /api/v1/orders/{id}/cancel
```

**인증**
- **인증 필수** (JWT 토큰 필요)
- Role: `USER` (본인 주문만), `ADMIN` (모든 주문)

**Path Parameter**
- `id` (Long, 필수)
  - 주문 ID

**Request Body**
- 없음

**Query Parameter**
- 없음

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": 1,
    "userId": 1,
    "status": "CANCELLED",
    "totalPrice": 2050000,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T01:00:00"
  }
}
```

**Response 타입**: `Response<OrderResponse>`

**비즈니스 로직**
- 상태 전이: `CREATED` → `CANCELLED`
- **USER Role**: 본인 주문만 취소 가능 (본인 주문이 아니면 403 Forbidden 에러 응답)
- **ADMIN Role**: 모든 주문 취소 가능
- 잘못된 상태 전이 시 에러 응답:
  - 현재 상태가 `CREATED`가 아니면 → `status: "ERROR"`, `data: { "code": "INVALID_ORDER_STATUS", "message": "취소할 수 없는 주문 상태입니다." }`
- 주문 취소 시 재고를 복구해야 함
- 이미 `CANCELLED` 또는 `COMPLETED` 된 주문은 취소 불가

---

#### 2-3-6. 주문 완료

**HTTP Method / Path**
```
PATCH /api/v1/orders/{id}/complete
```

**인증**
- **인증 필수** (JWT 토큰 필요)
- Role: `ADMIN`만 접근 가능 (일반 사용자는 접근 불가)

**Path Parameter**
- `id` (Long, 필수)
  - 주문 ID

**Request Body**
- 없음

**Query Parameter**
- 없음

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": 1,
    "userId": 1,
    "status": "COMPLETED",
    "totalPrice": 2050000,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T02:00:00"
  }
}
```

**Response 타입**: `Response<OrderResponse>`

**비즈니스 로직**
- 상태 전이: `PAID` → `COMPLETED`
- **ADMIN Role만 접근 가능** (USER Role은 403 Forbidden 에러 응답)
- 잘못된 상태 전이 시 에러 응답:
  - 현재 상태가 `PAID`가 아니면 → `status: "ERROR"`, `data: { "code": "INVALID_ORDER_STATUS", "message": "완료할 수 없는 주문 상태입니다." }`
- 이미 `CANCELLED` 또는 `COMPLETED` 된 주문은 완료 불가

---

### 2-4. 상품 승인 API

#### 2-4-1. 상품 승인

**HTTP Method / Path**
```
PATCH /api/v1/products/{id}/approve
```

**인증**
- **인증 필수** (JWT 토큰 필요)
- Role: **`ADMIN`만 접근 가능** (USER Role은 403 Forbidden 에러 응답)

**Path Parameter**
- `id` (Long, 필수)
  - 상품 ID

**Request Body**
- 없음 (또는 선택적으로 승인 사유 등 부가 정보 추가 가능)

**Query Parameter**
- 없음

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": 1,
    "status": "APPROVED",
    "name": "노트북",
    "price": 1000000,
    "stock": 10,
    "description": "고성능 노트북입니다.",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T01:00:00"
  }
}
```

**Response 타입**: `Response<ProductResponse>`

**비즈니스 로직**
- `status = PENDING` 인 상품만 승인 가능
  - 승인 성공 시 `status = APPROVED` 로 변경
  - 승인 이후부터 일반 사용자 상품 목록/단건 조회에 노출
- 잘못된 상태에서 호출 시 에러 응답:
  - 현재 상태가 `PENDING`이 아니면 → `status: "ERROR"`, `data: { "code": "INVALID_PRODUCT_STATUS", "message": "승인할 수 없는 상품 상태입니다." }`
- Soft Delete 된 상품은 승인 불가 → 에러 응답

---

## 3단계: 고급 주제 (동시성, Refresh Token, 이미지 업로드)

3단계는 **시간/실력에 따라 선택적으로 구현**해도 됩니다.  
다만, **동시성 관련 내용은 꼭 한 번 경험**해보는 것을 추천합니다.

---

### 3-1. Refresh Token 구현

#### 3-1-1. Refresh Token 저장 및 관리

**목표**
- Access Token의 만료 시간을 짧게 설정 (예: 30분)
- Refresh Token으로 Access Token을 재발급하는 전체 흐름 구현

**구현 요구사항**

1. **Refresh Token 저장 방식 선택**
   - Redis에 저장 (권장)
   - 또는 데이터베이스에 저장
   - 또는 JWT 자체에 포함 (stateless)

2. **로그인 시 Refresh Token 발급**
   - `POST /api/v1/auth/login` 응답에 `refreshToken` 추가
   - Refresh Token 만료 시간은 Access Token보다 길게 설정 (예: 7일, 30일)

3. **토큰 재발급 API**
   - `POST /api/v1/auth/reissue` (1단계에서 껍데기 생성됨)
   - 유효한 Refresh Token을 받아 새로운 Access Token 발급
   - Refresh Token 검증 후 유효하면 새로운 Access Token 반환

4. **로그아웃 시 Refresh Token 무효화**
   - `POST /api/v1/auth/logout` 엔드포인트 구현 (선택)
   - Refresh Token을 삭제하거나 블랙리스트에 추가

**로그인 Response 수정 예시**
```json
{
  "status": "SUCCESS",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

---

### 3-2. 동시성 문제 / Race Condition 해결

#### 3-2-1. 재고 차감 동시성 문제

**문제 시나리오**
- 재고가 1개 남은 인기 상품에 두 명 이상이 동시에 주문을 넣었을 때
- 재고가 0 아래로 내려가거나, 실제 재고보다 많이 팔리는 문제 발생

**해결 방법 선택**
다음 중 하나 이상을 선택하여 구현:

1. **비관적 락 (Pessimistic Lock)**
   - `@Lock(LockModeType.PESSIMISTIC_WRITE)` 사용
   - 트랜잭션 종료 시까지 해당 레코드에 락을 걸어 다른 트랜잭션의 접근 차단

2. **낙관적 락 (Optimistic Lock)**
   - `@Version` 필드 사용
   - 버전 충돌 시 예외 발생하여 재시도 로직 구현

3. **Redis 분산 락**
   - Redis를 이용한 분산 락 구현
   - Redisson 라이브러리 사용 권장

4. **데이터베이스 레벨 제약 조건**
   - `CHECK (stock >= 0)` 제약 조건 추가
   - 재고 부족 시 예외 처리

**구현 위치**
- `POST /api/v1/orders` (주문 생성) API에서 재고 차감 시 적용

**테스트 요구사항**
- 동시성 문제가 발생하는 상황을 재현할 수 있는 테스트 코드 작성
- 해결 전/후의 차이를 보여주는 테스트

---

#### 3-2-2. 상품 등록/승인 동시성 문제

**문제 시나리오**
- 동일한 비즈니스 키(예: 상품명 등)에 대해 여러 클라이언트가 동시에 상품 등록 요청
- 관리자 승인 흐름(상품 상태 `PENDING` → `APPROVED`)과 함께 고려했을 때:
  - 중복 상품이 과도하게 생성되거나
  - 승인되지 말아야 할 상품이 노출되는 등의 문제 발생

**해결 방법 선택**
다음 중 하나 이상을 선택하여 구현:

1. **데이터베이스 Unique 제약 조건**
   - 상품명에 Unique 제약 조건 추가
   - 중복 등록 시 예외 처리

2. **비관적 락**
   - 상품 등록 시 락을 걸어 동시 등록 방지

3. **Redis 분산 락**
   - Redis를 이용한 분산 락으로 중복 등록 방지

**구현 위치**
- `POST /api/v1/products` (상품 등록) API
- `PATCH /api/v1/products/{id}/approve` (상품 승인) API

**테스트 요구사항**
- 여러 개의 상품 등록 요청과 승인 API 호출이 섞여 들어오는 상황을 테스트
- 해결 전/후의 차이를 보여주는 테스트

**README 문서화**
- 해결 전/후에 대해 `README` 에 간단히 설명을 남기기

---

### 3-3. Multipart 파일 업로드 - 상품 이미지 (선택)

#### 3-3-1. 상품 이미지 업로드

**HTTP Method / Path**
```
POST /api/v1/products/{id}/images
```

**인증**
- **인증 필수** (JWT 토큰 필요)
- Role: `USER` (본인 상품만), `ADMIN` (모든 상품)

**Path Parameter**
- `id` (Long, 필수)
  - 상품 ID

**Request Body (multipart/form-data)**
- `images` (MultipartFile[], 필수)
  - 여러 이미지 파일 업로드 가능

**Request Body Validation**
- `images` (MultipartFile[], 필수)
  - 최소 1개 이상의 파일 필요
  - 파일 형식: jpg, png, gif 등 (구현 시 검증)
  - 파일 크기: 최대 5MB (구현 시 검증)

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": {
    "imageUrls": [
      "https://example.com/images/product-1-image-1.jpg",
      "https://example.com/images/product-1-image-2.jpg"
    ]
  }
}
```

**비즈니스 로직**
- 한 번에 여러 이미지를 업로드할 수 있도록 구현
- 업로드된 이미지 파일의 저장 위치 결정:
    - 로컬 파일 시스템에 저장하거나
    - 클라우드 스토리지(AWS S3, Azure Blob Storage 등)에 저장
- 상품 엔티티에 이미지 URL 필드 추가:
  - `imageUrls` (List<String>) 또는 `mainImageUrl` (String)
- 이미지 파일 검증:
    - 파일 형식 검증 (jpg, png, gif 등)
    - 파일 크기 제한 (예: 최대 5MB)
- 파일명 중복 방지 (UUID 사용 등)
- **USER Role**: 본인 상품만 이미지 업로드 가능
- **ADMIN Role**: 모든 상품에 이미지 업로드 가능

---

#### 3-3-2. 상품 이미지 삭제

**HTTP Method / Path**
```
DELETE /api/v1/products/{id}/images/{imageId}
```

**인증**
- **인증 필수** (JWT 토큰 필요)
- Role: `USER` (본인 상품만), `ADMIN` (모든 상품)

**Path Parameter**
- `id` (Long, 필수)
  - 상품 ID
- `imageId` (Long, 필수)
  - 이미지 ID

**Request Body**
- 없음

**Response (200 OK)**
```json
{
  "status": "SUCCESS",
  "data": null
}
```

**비즈니스 로직**
- 이미지 파일과 DB 레코드 모두 삭제
- **USER Role**: 본인 상품만 이미지 삭제 가능
- **ADMIN Role**: 모든 상품의 이미지 삭제 가능

---

#### 3-3-3. 상품 조회 시 이미지 URL 포함

**수정 필요 API**
- `GET /api/v1/products/{id}` (상품 단건 조회)
- `GET /api/v1/products` (상품 목록 조회)

**Response 수정 예시**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": 1,
    "status": "APPROVED",
    "name": "노트북",
    "price": 1000000,
    "stock": 10,
    "description": "고성능 노트북입니다.",
    "imageUrls": [
      "https://example.com/images/product-1-image-1.jpg",
      "https://example.com/images/product-1-image-2.jpg"
    ],
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

**추가 고려사항**
  - 이미지 리사이징/썸네일 생성
  - 이미지 최적화

---

## 4단계: 배포 & 리버스 프록시 (선택)

### 4-1. Docker 기반 배포
- **목표**: 애플리케이션을 컨테이너화하고, 이미지 빌드/실행 파이프라인을 구성합니다.
- **과제**:
  - `Dockerfile` 작성 (JDK 17, JAR 복사, `ENTRYPOINT ["java","-jar","app.jar"]` 형태).
  - 멀티스테이지 빌드로 이미지 슬림화.
  - 로컬에서 `docker build` / `docker run`으로 기동 확인.

### 4-2. 클라우드 배포 (VPC 설계 포함)
- **목표**: 퍼블릭/프라이빗 서브넷을 가진 VPC에 애플리케이션을 배포합니다.
- **VPC 구조(예시)**:
  - 퍼블릭 서브넷 1개: Reverse Proxy/ALB 배치, 인터넷 게이트웨이 연결.
  - 프라이빗 서브넷 1개: 애플리케이션 인스턴스(EC2) 배치.
  - RDS(PostgreSQL) & ElastiCache(Redis) 는 프라이빗 서브넷에 배치.
  - NAT Gateway(선택): 앱이 외부로 나가야 할 경우 사용.
- **보안**:
  - SG: 퍼블릭 → 80/443 허용, 프라이빗 앱 SG는 퍼블릭/ALB SG만 허용.
  - DB/Redis SG는 앱 SG에서만 접근 허용.
- **과제**:
  - 위 구조로 VPC, Subnet, IGW, (필요시 NAT) 구성.
  - 애플리케이션 이미지를 레지스트리(ECR 등)에 업로드 후 EC2에 배포.
  - RDS/ElastiCache 연결 설정 및 보안 그룹 구성.

### 4-3. 리버스 프록시 구성
- **목표**: Nginx(또는 ALB)로 HTTPS 종료 및 경로 라우팅을 수행합니다.
- **과제**:
  - 퍼블릭 서브넷에 Nginx/ALB를 두고, 백엔드(프라이빗)로 프록시 패스.
  - 기본 헬스체크 엔드포인트 지정 (`/actuator/health` 등).
  - 정적 파일/압축/캐싱 헤더 설정은 선택.
  - HTTPS 인증서(예: ACM/Let's Encrypt) 적용.

### 4-4. 이미지 업로드 클라우드화 및 캐싱
- **목표**: S3로 이미지 저장소를 옮기고, presigned URL 방식으로 개선합니다. 또한, 이미지를 캐싱한다.
- **과제**:
  - 기존 로컬 저장소에서 S3로 이미지 저장소를 옮긴다.
  - Multipart 방식이 아닌, Presigned URL 방식으로 바꾼다.
  - CloudFront와 같은 CDN을 이용하여 캐싱을 진행한다.

---

## 5단계: 심화 주제 (난이도 ↑)

### 5-1. 배포/인프라 테스트 자동화 (CI/CD 파이프라인 구축)
- **목표**: 배포 및 인프라 구성을 검증할 수 있는 테스트를 마련합니다.
- **과제**:
  - 애플리케이션: `@SpringBootTest` + 간단한 통합 테스트로 헬스 체크 또는 주요 API 1~2개 호출.
  - 인프라: Testcontainers(선택)로 로컬에서 Postgres/Redis 연동 테스트.
  - CI에서 `./gradlew test`와 `./gradlew bootJar`를 최소 스텝으로 실행하도록 구성.
  - 계약/스냅샷 테스트(선택): DTO/Response 구조가 깨지지 않는지 검증.
  - E2E(선택): 간단한 API 시나리오를 RestAssured 등으로 작성.

### 5-2. 성능/부하 테스트
- k6/JMeter 등으로 기본 시나리오 부하 테스트(목록 조회, 주문 생성).
- 목표 TPS/지연시간을 정하고 결과를 정리.

### 5-3. 캐싱 (선택)

#### 목표
- 상품 목록 첫 페이지(예: `page=0`, 기본 size)에 대한 응답을 캐싱하여 조회 성능을 개선합니다.

#### 구현 가이드
- Spring Cache 사용 (`spring-boot-starter-cache`, `@EnableCaching`).
- 캐시 저장소: Redis 사용 권장 (이미 의존성 포함).
- 캐싱 대상:
  - `GET /api/v1/products?page=0&size=...` (필터 조건이 없는 기본 목록) 응답을 캐싱.
  - 다른 페이지나 필터(`name`, `minPrice`, `maxPrice`)는 캐싱하지 않음.
- 캐시 키 설계: `products:page:{page}:size:{size}` (page=0일 때만 저장).
- 무효화:
  - 상품 생성/수정/삭제/승인 시 해당 키 무효화(`@CacheEvict`).
  - 재고/가격 변경에 따라 목록 정보가 바뀌면 무효화.

### 5-4. 비동기 처리 (선택)

#### 목표
- 주요 흐름을 블로킹하지 않으면서 부가 작업을 처리해 성능과 사용자 경험을 개선합니다.

#### 아이디어
- 주문 생성 후:
  - 알림/이메일 발송을 `@Async` 메서드로 분리.
  - 재고 변경 이벤트를 메시지 큐(선택) 또는 비동기 로깅으로 처리.
- 상품 등록/승인 후:
  - 검색 인덱스 동기화(선택)를 비동기로 처리.

#### 구현 가이드
- Spring `@EnableAsync` + `@Async` 사용.
- 스레드 풀 설정 (`TaskExecutor`)로 기본 풀 크기/큐 사이즈 지정.
- 트랜잭션 경계 주의:
  - 비동기 메서드에서 필요한 데이터는 DTO 형태로 전달하거나 재조회.
  - 예외 발생 시 로깅 및 재시도 정책(필요 시) 고려.

---

## 6단계: 초심화 주제 (난이도 ↑)

### 6-1. 관측 가능성(Observability)
- Distributed Tracing: OpenTelemetry + OTLP(jaeger/tempo 등)로 주요 API 트레이스 수집.
- Metrics: Micrometer + Prometheus/Grafana로 주요 비즈니스/시스템 메트릭 노출.
- Log 구조화: JSON 로그 + Correlation ID/Trace ID 포함.

### 6-2. 회복탄력성(Resilience)
- Resilience4j로 `retry`, `circuit breaker`, `rate limiter` 적용 (예: 외부 결제/메일).
- 타임아웃/폴백 전략 정의, 실패 메트릭 수집.

### 6-3. 메시지/이벤트 기반 확장
- 주문 이벤트를 메시지 큐(Kafka/SQS/RabbitMQ 등)로 발행, 비동기 후처리(알림/적립금 등).
- Outbox 패턴(선택)으로 트랜잭션 정합성 확보.

### 6-4. 배포 전략 고도화
- Blue-Green 또는 Rolling 업데이트 시나리오 정리 및 스크립트화.
- 데이터베이스 마이그레이션 전략 명시(예: Flyway)와 롤백 절차 문서화.

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
