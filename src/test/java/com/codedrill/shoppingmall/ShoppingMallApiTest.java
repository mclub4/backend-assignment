package com.codedrill.shoppingmall;

import com.codedrill.shoppingmall.user.entity.User;
import com.codedrill.shoppingmall.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShoppingMallApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static int totalScore = 0;
    private static final int MAX_SCORE = 115; // 총 29개 테스트, 115점 만점 (로그인 관리자 테스트 제거)
    private static Long productId = null;
    private static Long orderId = null;
    private static Long userId = null;
    private static Long adminId = null;
    private static String userAccessToken = null;
    private static String adminAccessToken = null;

    @BeforeAll
    static void setUp() {
        System.out.println("\n==========================================");
        System.out.println("쇼핑몰 API 테스트 시작");
        System.out.println("==========================================\n");
    }

    @AfterAll
    static void tearDown() {
        System.out.println("\n==========================================");
        System.out.println("테스트 완료");
        System.out.println("==========================================");
        System.out.printf("획득 점수: %d / %d 점\n", totalScore, MAX_SCORE);
        System.out.printf("최종 점수: %.1f 점\n", (double) totalScore / MAX_SCORE * 100);
        System.out.println("==========================================\n");
    }

    private void addScore(int score, String testName) {
        totalScore += score;
        System.out.printf("✓ [%d점] %s\n", score, testName);
    }

    // ==================== 인증 관련 테스트 ====================

    @Test
    @Order(1)
    @DisplayName("1. 회원 가입 - 일반 사용자")
    void testSignupUser() throws Exception {
        String request = """
                {
                    "email": "user@test.com",
                    "password": "Test1234!",
                    "name": "테스트유저"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.email").value("user@test.com"))
                .andExpect(jsonPath("$.data.name").value("테스트유저"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
        userId = Long.valueOf(data.get("id").toString());

        addScore(3, "회원 가입 - 일반 사용자");
    }

    @Test
    @Order(2)
    @DisplayName("2. 회원 가입 - 관리자")
    void testSignupAdmin() throws Exception {
        String request = """
                {
                    "email": "admin@test.com",
                    "password": "Admin1234!",
                    "name": "관리자"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.email").value("admin@test.com"))
                .andExpect(jsonPath("$.data.role").value("USER")) // 기본값은 USER
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
        adminId = Long.valueOf(data.get("id").toString());

        addScore(3, "회원 가입 - 관리자");
    }

    @Test
    @Order(3)
    @DisplayName("3. 비밀번호 암호화 검증")
    void testPasswordEncryption() throws Exception {
        String plainPassword = "Encrypt1!@";
        String request = """
                {
                    "email": "encrypt@test.com",
                    "password": "%s",
                    "name": "암호화테스트"
                }
                """.formatted(plainPassword);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
        Long testUserId = Long.valueOf(data.get("id").toString());

        // 데이터베이스에서 사용자 조회
        User savedUser = userRepository.findById(testUserId)
                .orElseThrow(() -> new AssertionError("사용자를 찾을 수 없습니다."));

        String savedPassword = savedUser.getPassword();

        // 1. 원본 비밀번호와 저장된 비밀번호가 다른지 확인
        assertNotEquals(plainPassword, savedPassword, "비밀번호가 암호화되지 않았습니다.");

        // 2. BCrypt 형식인지 확인 (BCrypt 해시는 $2a$, $2b$, $2y$로 시작)
        assertTrue(savedPassword.startsWith("$2a$") || savedPassword.startsWith("$2b$") || savedPassword.startsWith("$2y$"),
                "비밀번호가 BCrypt 형식이 아닙니다. 저장된 비밀번호: " + savedPassword);

        // 3. 원본 비밀번호와 저장된 비밀번호가 매칭되는지 확인
        assertTrue(passwordEncoder.matches(plainPassword, savedPassword),
                "비밀번호 인코더를 통한 검증이 실패했습니다.");

        addScore(4, "비밀번호 암호화 검증");
    }

    @Test
    @Order(4)
    @DisplayName("4. 로그인 - 일반 사용자")
    void testLoginUser() throws Exception {
        String signupRequest = """
                {
                    "email": "user@test.com",
                    "password": "Test1234!",
                    "name": "테스트유저"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupRequest))
                .andExpect(status().isOk());

        String request = """
                {
                    "email": "user@test.com",
                    "password": "Test1234!"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
        userAccessToken = (String) data.get("accessToken");

        addScore(5, "로그인 - 일반 사용자");
    }

    // ==================== 상품 관련 테스트 ====================

    @Test
    @Order(6)
    @DisplayName("6. 상품 등록")
    @WithMockUser(roles = "ADMIN")
    void testCreateProduct() throws Exception {
        String request = """
                {
                    "name": "테스트 상품",
                    "price": 10000,
                    "stock": 100,
                    "description": "테스트 상품 설명"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("테스트 상품"))
                .andExpect(jsonPath("$.data.price").value(10000))
                .andExpect(jsonPath("$.data.stock").value(100))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
        productId = Long.valueOf(data.get("id").toString());

        addScore(8, "상품 등록");
    }

    @Test
    @Order(7)
    @DisplayName("7. 상품 목록 조회 (페이지네이션)")
    void testGetProductList() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").exists())
                .andExpect(jsonPath("$.data.totalPages").exists())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));

        addScore(5, "상품 목록 조회 (페이지네이션)");
    }

    @Test
    @Order(8)
    @DisplayName("7-1. 상품 목록 조회 - 검색 기능 (가격 범위, 이름 검색)")
    void testGetProductListWithSearch() throws Exception {
        // 검색용 상품 생성
        String productRequest1 = """
                {
                    "name": "검색테스트 노트북",
                    "price": 15000,
                    "stock": 50,
                    "description": "검색 테스트용"
                }
                """;

        MvcResult result1 = mockMvc.perform(post("/api/v1/products")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest1))
                .andReturn();

        String productResponse1 = result1.getResponse().getContentAsString();
        Map<String, Object> productResponseMap1 = objectMapper.readValue(productResponse1, Map.class);
        Map<String, Object> productData1 = (Map<String, Object>) productResponseMap1.get("data");
        Long searchProductId1 = Long.valueOf(productData1.get("id").toString());

        // 상품 승인
        mockMvc.perform(patch("/api/v1/products/" + searchProductId1 + "/approve")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")));

        // 가격 범위 검색 테스트
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("minPrice", "10000")
                        .param("maxPrice", "20000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isArray());

        // 이름 검색 테스트
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("name", "노트북"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isArray());

        // 복합 검색 테스트
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("minPrice", "10000")
                        .param("maxPrice", "20000")
                        .param("name", "노트북"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isArray());

        addScore(3, "상품 목록 조회 - 검색 기능");
    }

    @Test
    @Order(9)
    @DisplayName("8. 상품 단건 조회")
    void testGetProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.name").value("테스트 상품"))
                .andExpect(jsonPath("$.data.price").value(10000))
                .andExpect(jsonPath("$.data.stock").value(100));

        addScore(5, "상품 단건 조회");
    }

    @Test
    @Order(10)
    @DisplayName("8-1. PENDING 상품은 일반 사용자에게 노출되지 않음")
    void testPendingProductNotVisibleToUser() throws Exception {
        // PENDING 상태의 상품 생성
        String pendingProductRequest = """
                {
                    "name": "PENDING 상품",
                    "price": 5000,
                    "stock": 20,
                    "description": "승인 대기 중"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pendingProductRequest))
                .andReturn();

        String productResponse = result.getResponse().getContentAsString();
        Map<String, Object> productResponseMap = objectMapper.readValue(productResponse, Map.class);
        Map<String, Object> productData = (Map<String, Object>) productResponseMap.get("data");
        Long pendingProductId = Long.valueOf(productData.get("id").toString());

        // 일반 사용자로 조회 시도 (404 또는 에러 응답)
        mockMvc.perform(get("/api/v1/products/" + pendingProductId))
                .andExpect(status().isNotFound());

        // 일반 사용자 목록 조회에서도 보이지 않아야 함 (승인된 상품만)
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        addScore(3, "PENDING 상품은 일반 사용자에게 노출되지 않음");
    }

    @Test
    @Order(11)
    @DisplayName("9. 상품 수정")
    @WithMockUser(roles = "ADMIN")
    void testUpdateProduct() throws Exception {
        String request = """
                {
                    "name": "수정된 상품",
                    "price": 15000,
                    "stock": 200,
                    "description": "수정된 설명"
                }
                """;

        mockMvc.perform(put("/api/v1/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("수정된 상품"))
                .andExpect(jsonPath("$.data.price").value(15000))
                .andExpect(jsonPath("$.data.stock").value(200));

        addScore(5, "상품 수정");
    }

    @Test
    @Order(12)
    @DisplayName("9-1. Validation 테스트 - 잘못된 입력값")
    void testValidation() throws Exception {
        // 상품 등록 - 잘못된 입력값
        String invalidRequest1 = """
                {
                    "name": "A",
                    "price": -1000,
                    "stock": -10,
                    "description": "잘못된 입력"
                }
                """;

        mockMvc.perform(post("/api/v1/products")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest1))
                .andExpect(status().isBadRequest());

        // 회원 가입 - 잘못된 이메일 형식
        String invalidRequest2 = """
                {
                    "email": "invalid-email",
                    "password": "Test1234!",
                    "name": "테스트"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest2))
                .andExpect(status().isBadRequest());

        // 회원 가입 - 짧은 비밀번호
        String invalidRequest3 = """
                {
                    "email": "test@test.com",
                    "password": "Short1!",
                    "name": "테스트"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest3))
                .andExpect(status().isBadRequest());

        addScore(3, "Validation 테스트");
    }

    @Test
    @Order(13)
    @DisplayName("9-2. Role 기반 접근 제어 - USER는 상품 등록/수정/삭제 불가")
    void testRoleBasedAccessControl() throws Exception {
        String productRequest = """
                {
                    "name": "권한 테스트 상품",
                    "price": 1000,
                    "stock": 10,
                    "description": "권한 테스트"
                }
                """;

        // USER가 상품 등록 시도 (실패해야 함)
        mockMvc.perform(post("/api/v1/products")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest))
                .andExpect(status().isForbidden());

        // USER가 상품 수정 시도 (실패해야 함)
        mockMvc.perform(put("/api/v1/products/" + productId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest))
                .andExpect(status().isForbidden());

        // USER가 상품 삭제 시도 (실패해야 함)
        mockMvc.perform(delete("/api/v1/products/" + productId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isForbidden());

        // USER가 상품 승인 시도 (실패해야 함)
        mockMvc.perform(patch("/api/v1/products/" + productId + "/approve")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isForbidden());

        addScore(4, "Role 기반 접근 제어");
    }

    @Test
    @Order(14)
    @DisplayName("10. 상품 승인")
    @WithMockUser(roles = "ADMIN")
    void testApproveProduct() throws Exception {
        mockMvc.perform(patch("/api/v1/products/" + productId + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        addScore(9, "상품 승인");
    }

    @Test
    @Order(15)
    @DisplayName("11. 상품 삭제 (Soft Delete)")
    @WithMockUser(roles = "ADMIN")
    void testDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/v1/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // 삭제된 상품은 조회되지 않아야 함
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isNotFound());

        addScore(9, "상품 삭제 (Soft Delete)");
    }

    // ==================== 주문 관련 테스트 ====================

    @Test
    @Order(16)
    @DisplayName("12. 주문 생성")
    void testCreateOrder() throws Exception {
        // 먼저 새로운 상품 생성 (관리자 권한 필요)
        String productRequest = """
                {
                    "name": "주문 테스트 상품",
                    "price": 5000,
                    "stock": 50,
                    "description": "주문 테스트용"
                }
                """;

        MvcResult productResult = mockMvc.perform(post("/api/v1/products")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest))
                .andReturn();

        String productResponse = productResult.getResponse().getContentAsString();
        Map<String, Object> productResponseMap = objectMapper.readValue(productResponse, Map.class);
        Map<String, Object> productData = (Map<String, Object>) productResponseMap.get("data");
        Long orderProductId = Long.valueOf(productData.get("id").toString());

        // 상품 승인
        mockMvc.perform(patch("/api/v1/products/" + orderProductId + "/approve")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")));

        // 주문 생성 (일반 사용자 권한)
        String orderRequest = """
                {
                    "items": [
                        {
                            "productId": %d,
                            "quantity": 2
                        }
                    ]
                }
                """.formatted(orderProductId);

        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.totalPrice").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
        orderId = Long.valueOf(data.get("id").toString());

        addScore(4, "주문 생성");
    }

    @Test
    @Order(17)
    @DisplayName("12-1. 주문 생성 - 재고 부족 시 실패")
    void testCreateOrderWithInsufficientStock() throws Exception {
        // 재고 1개인 상품 생성
        String productRequest = """
                {
                    "name": "재고 부족 테스트 상품",
                    "price": 1000,
                    "stock": 1,
                    "description": "재고 부족 테스트"
                }
                """;

        MvcResult productResult = mockMvc.perform(post("/api/v1/products")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest))
                .andReturn();

        String productResponse = productResult.getResponse().getContentAsString();
        Map<String, Object> productResponseMap = objectMapper.readValue(productResponse, Map.class);
        Map<String, Object> productData = (Map<String, Object>) productResponseMap.get("data");
        Long stockProductId = Long.valueOf(productData.get("id").toString());

        // 상품 승인
        mockMvc.perform(patch("/api/v1/products/" + stockProductId + "/approve")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")));

        // 재고보다 많은 수량 주문 시도
        String orderRequest = """
                {
                    "items": [
                        {
                            "productId": %d,
                            "quantity": 10
                        }
                    ]
                }
                """.formatted(stockProductId);

        mockMvc.perform(post("/api/v1/orders")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest))
                .andExpect(status().isBadRequest());

        addScore(3, "주문 생성 - 재고 부족 시 실패");
    }

    @Test
    @Order(18)
    @DisplayName("13. 내 주문 목록 조회")
    @WithMockUser(roles = "USER")
    void testGetMyOrders() throws Exception {
        mockMvc.perform(get("/api/v1/orders/my")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").exists());

        addScore(3, "내 주문 목록 조회");
    }

    @Test
    @Order(19)
    @DisplayName("13-1. 내 주문 목록 조회 - 상태 필터링")
    @WithMockUser(roles = "USER")
    void testGetMyOrdersWithStatusFilter() throws Exception {
        // CREATED 상태 필터
        mockMvc.perform(get("/api/v1/orders/my")
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").exists());

        // PAID 상태 필터
        mockMvc.perform(get("/api/v1/orders/my")
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").exists());

        addScore(2, "내 주문 목록 조회 - 상태 필터링");
    }

    @Test
    @Order(20)
    @DisplayName("14. 주문 상세 조회")
    @WithMockUser(roles = "USER")
    void testGetOrder() throws Exception {
        mockMvc.perform(get("/api/v1/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.items").isArray());

        addScore(3, "주문 상세 조회");
    }

    @Test
    @Order(21)
    @DisplayName("14-1. 주문 상세 조회 - 본인 주문만 조회 가능")
    void testGetOrderOnlyOwnOrder() throws Exception {
        // 다른 사용자 생성
        String otherUserRequest = """
                {
                    "email": "other@test.com",
                    "password": "Other1234!",
                    "name": "다른사용자"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(otherUserRequest))
                .andExpect(status().isOk());

        // 다른 사용자가 다른 사람의 주문 조회 시도 (실패해야 함)
        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("other").roles("USER")))
                .andExpect(status().isForbidden());

        addScore(3, "주문 상세 조회 - 본인 주문만 조회 가능");
    }

    @Test
    @Order(22)
    @DisplayName("15. 주문 결제")
    @WithMockUser(roles = "USER")
    void testPayOrder() throws Exception {
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        addScore(3, "주문 결제");
    }

    @Test
    @Order(23)
    @DisplayName("16. 주문 완료")
    @WithMockUser(roles = "USER")
    void testCompleteOrder() throws Exception {
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        addScore(3, "주문 완료");
    }

    @Test
    @Order(24)
    @DisplayName("16-1. 잘못된 상태 전이 테스트")
    void testInvalidStatusTransition() throws Exception {
        // 새로운 주문 생성
        String productRequest = """
                {
                    "name": "상태 전이 테스트 상품",
                    "price": 2000,
                    "stock": 20,
                    "description": "상태 전이 테스트"
                }
                """;

        MvcResult productResult = mockMvc.perform(post("/api/v1/products")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest))
                .andReturn();

        String productResponse = productResult.getResponse().getContentAsString();
        Map<String, Object> productResponseMap = objectMapper.readValue(productResponse, Map.class);
        Map<String, Object> productData = (Map<String, Object>) productResponseMap.get("data");
        Long statusProductId = Long.valueOf(productData.get("id").toString());

        mockMvc.perform(patch("/api/v1/products/" + statusProductId + "/approve")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")));

        String orderRequest = """
                {
                    "items": [
                        {
                            "productId": %d,
                            "quantity": 1
                        }
                    ]
                }
                """.formatted(statusProductId);

        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest))
                .andReturn();

        String orderResponse = orderResult.getResponse().getContentAsString();
        Map<String, Object> orderResponseMap = objectMapper.readValue(orderResponse, Map.class);
        Map<String, Object> orderData = (Map<String, Object>) orderResponseMap.get("data");
        Long statusOrderId = Long.valueOf(orderData.get("id").toString());

        // CREATED 상태에서 바로 COMPLETED로 변경 시도 (실패해야 함)
        mockMvc.perform(patch("/api/v1/orders/" + statusOrderId + "/complete")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isBadRequest());

        // 결제 후 취소 시도 (PAID 상태에서 CANCELLED로 변경 불가)
        mockMvc.perform(patch("/api/v1/orders/" + statusOrderId + "/pay")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER")));

        mockMvc.perform(patch("/api/v1/orders/" + statusOrderId + "/cancel")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isBadRequest());

        addScore(3, "잘못된 상태 전이 테스트");
    }

    @Test
    @Order(25)
    @DisplayName("17. 주문 취소 (새 주문 생성 후)")
    void testCancelOrder() throws Exception {
        // 새로운 상품 생성 (관리자 권한)
        String productRequest = """
                {
                    "name": "취소 테스트 상품",
                    "price": 3000,
                    "stock": 30,
                    "description": "취소 테스트용"
                }
                """;

        MvcResult productResult = mockMvc.perform(post("/api/v1/products")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest))
                .andReturn();

        String productResponse = productResult.getResponse().getContentAsString();
        Map<String, Object> productResponseMap = objectMapper.readValue(productResponse, Map.class);
        Map<String, Object> productData = (Map<String, Object>) productResponseMap.get("data");
        Long cancelProductId = Long.valueOf(productData.get("id").toString());

        mockMvc.perform(patch("/api/v1/products/" + cancelProductId + "/approve")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")));

        // 주문 생성 (일반 사용자 권한)
        String orderRequest = """
                {
                    "items": [
                        {
                            "productId": %d,
                            "quantity": 1
                        }
                    ]
                }
                """.formatted(cancelProductId);

        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest))
                .andReturn();

        String orderResponse = orderResult.getResponse().getContentAsString();
        Map<String, Object> orderResponseMap = objectMapper.readValue(orderResponse, Map.class);
        Map<String, Object> orderData = (Map<String, Object>) orderResponseMap.get("data");
        Long cancelOrderId = Long.valueOf(orderData.get("id").toString());

        // 주문 취소 (일반 사용자 권한)
        mockMvc.perform(patch("/api/v1/orders/" + cancelOrderId + "/cancel")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        addScore(2, "주문 취소");
    }

    // ==================== 3단계: 고급 주제 테스트 ====================

    @Test
    @Order(26)
    @DisplayName("18. Refresh Token 발급 및 재발급")
    void testRefreshToken() throws Exception {
        // 먼저 로그인하여 refreshToken 획득
        String loginRequest = """
                {
                    "email": "user@test.com",
                    "password": "Test1234!"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        Map<String, Object> loginResponseMap = objectMapper.readValue(loginResponse, Map.class);
        Map<String, Object> loginData = (Map<String, Object>) loginResponseMap.get("data");

        // refreshToken이 있는지 확인 (구현 여부에 따라 선택적)
        if (loginData.containsKey("refreshToken")) {
            String refreshToken = (String) loginData.get("refreshToken");

            // Refresh Token으로 새로운 Access Token 발급
            String refreshRequest = """
                    {
                        "refreshToken": "%s"
                    }
                    """.formatted(refreshToken);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").exists());
        } else {
            // refreshToken이 없으면 테스트 통과 (구현하지 않은 경우)
            System.out.println("RefreshToken이 구현되지 않았습니다. 테스트를 건너뜁니다.");
        }

        addScore(3, "Refresh Token 발급 및 재발급");
    }

    @Test
    @Order(27)
    @DisplayName("19. 동시성 문제 - 재고 차감 Race Condition 예방")
    void testConcurrentStockDecrease() throws Exception {
        // 새로운 상품 생성 (재고 10개)
        String productRequest = """
                {
                    "name": "동시성 테스트 상품",
                    "price": 1000,
                    "stock": 10,
                    "description": "동시성 테스트용"
                }
                """;

        MvcResult productResult = mockMvc.perform(post("/api/v1/products")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest))
                .andReturn();

        String productResponse = productResult.getResponse().getContentAsString();
        Map<String, Object> productResponseMap = objectMapper.readValue(productResponse, Map.class);
        Map<String, Object> productData = (Map<String, Object>) productResponseMap.get("data");
        Long concurrentProductId = Long.valueOf(productData.get("id").toString());

        // 상품 승인
        mockMvc.perform(patch("/api/v1/products/" + concurrentProductId + "/approve")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")));

        // 동시에 여러 주문 생성 (재고 10개에 15개 주문 시도)
        String orderRequest = """
                {
                    "items": [
                        {
                            "productId": %d,
                            "quantity": 1
                        }
                    ]
                }
                """.formatted(concurrentProductId);

        // 15개의 동시 요청 시뮬레이션
        int successCount = 0;
        int failCount = 0;
        Object object = new Object[0];

        for (int i = 0; i < 15; i++) {
            try {
                MvcResult result = mockMvc.perform(post("/api/v1/orders")
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(orderRequest))
                        .andReturn();

                if (result.getResponse().getStatus() == 200) {
                    synchronized (object) {
                        successCount++;
                    }
                } else {
                    synchronized (object) {
                        failCount++;
                    }
                }
            } catch (Exception e) {
                synchronized (object) {
                    failCount++;
                }
            }
        }

        // 재고가 0 아래로 내려가지 않았는지 확인
        // 최대 10개의 주문만 성공해야 함
        assertTrue(successCount <= 10,
                "재고보다 많은 주문이 성공했습니다. 성공: " + successCount + ", 실패: " + failCount);

        // 최소 5개 이상의 주문이 실패해야 함 (재고 보호)
        assertTrue(failCount >= 5,
                "동시성 문제가 해결되지 않았습니다. 성공: " + successCount + ", 실패: " + failCount);

        addScore(3, "동시성 문제 - 재고 차감 Race Condition 예방");
    }

    @Test
    @Order(28)
    @DisplayName("20. Multipart 파일 업로드 - 상품 이미지")
    @WithMockUser(roles = "ADMIN")
    void testProductImageUpload() throws Exception {
        // 새로운 상품 생성
        String productRequest = """
                {
                    "name": "이미지 테스트 상품",
                    "price": 3000,
                    "stock": 30,
                    "description": "이미지 업로드 테스트용"
                }
                """;

        MvcResult productResult = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest))
                .andReturn();

        String productResponse = productResult.getResponse().getContentAsString();
        Map<String, Object> productResponseMap = objectMapper.readValue(productResponse, Map.class);
        Map<String, Object> productData = (Map<String, Object>) productResponseMap.get("data");
        Long imageProductId = Long.valueOf(productData.get("id").toString());

        // 더미 이미지 파일 생성 (1x1 픽셀 PNG)
        byte[] imageBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG 시그니처
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // IHDR 청크
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, // 1x1 픽셀
                0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53, (byte) 0xDE,
                0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54, // IDAT 청크
                0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00, 0x00,
                0x03, 0x01, 0x01, 0x00, 0x18, (byte) 0xDD, (byte) 0x8D, (byte) 0xB4, // IEND 청크
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };

        org.springframework.mock.web.MockMultipartFile imageFile =
                new org.springframework.mock.web.MockMultipartFile(
                        "images", // 파라미터 이름
                        "test-image.png", // 파일명
                        "image/png", // Content-Type
                        imageBytes // 파일 내용
                );

        // 이미지 업로드 테스트
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/products/" + imageProductId + "/images")
                        .file(imageFile)
                        .file(imageFile)) // 여러 이미지 업로드 테스트
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andReturn();

        // 상품 조회 시 이미지 URL이 포함되어 있는지 확인
        mockMvc.perform(get("/api/v1/products/" + imageProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").exists());
                // 이미지 URL 필드가 있다면 추가 검증
                // .andExpect(jsonPath("$.data.imageUrls").exists());

        addScore(4, "Multipart 파일 업로드 - 상품 이미지");
    }

    @Test
    @Order(29)
    @DisplayName("21. 이메일 중복 검증")
    void testDuplicateEmail() throws Exception {
        String request = """
                {
                    "email": "duplicate@test.com",
                    "password": "Test1234!",
                    "name": "중복테스트"
                }
                """;

        // 첫 번째 가입 성공
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());

        // 동일한 이메일로 재가입 시도 (실패해야 함)
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        addScore(2, "이메일 중복 검증");
    }

    @Test
    @Order(30)
    @DisplayName("22. Soft Delete된 상품 수정 불가")
    @WithMockUser(roles = "ADMIN")
    void testUpdateDeletedProduct() throws Exception {
        // 이미 삭제된 상품 수정 시도 (실패해야 함)
        String request = """
                {
                    "name": "수정 시도",
                    "price": 1000,
                    "stock": 10,
                    "description": "수정"
                }
                """;

        mockMvc.perform(put("/api/v1/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound());

        addScore(2, "Soft Delete된 상품 수정 불가");
    }
}
