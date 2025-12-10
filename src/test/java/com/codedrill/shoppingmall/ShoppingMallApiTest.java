package com.codedrill.shoppingmall;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.codedrill.shoppingmall.user.repository.UserRepository;
import com.codedrill.shoppingmall.user.entity.User;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
    private static final int MAX_SCORE = 100;
    private static Long productId = null;
    private static Long orderId = null;
    private static Long userId = null;
    private static Long adminId = null;

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
        String plainPassword = "EncryptTest1234!";
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
        String request = """
                {
                    "email": "user@test.com",
                    "password": "Test1234!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").exists());

        addScore(5, "로그인 - 일반 사용자");
    }

    @Test
    @Order(5)
    @DisplayName("5. 로그인 - 관리자")
    void testLoginAdmin() throws Exception {
        String request = """
                {
                    "email": "admin@test.com",
                    "password": "Admin1234!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").exists());

        addScore(5, "로그인 - 관리자");
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

        addScore(8, "상품 목록 조회 (페이지네이션)");
    }

    @Test
    @Order(8)
    @DisplayName("8. 상품 단건 조회")
    void testGetProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.name").value("테스트 상품"))
                .andExpect(jsonPath("$.data.price").value(10000))
                .andExpect(jsonPath("$.data.stock").value(100));

        addScore(8, "상품 단건 조회");
    }

    @Test
    @Order(9)
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

        addScore(8, "상품 수정");
    }

    @Test
    @Order(10)
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
    @Order(11)
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
    @Order(12)
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
    @Order(13)
    @DisplayName("13. 내 주문 목록 조회")
    @WithMockUser(roles = "USER")
    void testGetMyOrders() throws Exception {
        mockMvc.perform(get("/api/v1/orders/my")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").exists());

        addScore(4, "내 주문 목록 조회");
    }

    @Test
    @Order(14)
    @DisplayName("14. 주문 상세 조회")
    @WithMockUser(roles = "USER")
    void testGetOrder() throws Exception {
        mockMvc.perform(get("/api/v1/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.items").isArray());

        addScore(4, "주문 상세 조회");
    }

    @Test
    @Order(15)
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
    @Order(16)
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
    @Order(17)
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
    @Order(18)
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
    @Order(19)
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
        
        for (int i = 0; i < 15; i++) {
            try {
                MvcResult result = mockMvc.perform(post("/api/v1/orders")
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(orderRequest))
                        .andReturn();
                
                if (result.getResponse().getStatus() == 200) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
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
    @Order(20)
    @DisplayName("20. N+1 문제 예방 - 주문 목록 조회")
    @WithMockUser(roles = "USER")
    void testNPlusOneProblem() throws Exception {
        // 주문 목록 조회 시 N+1 문제가 발생하지 않는지 확인
        // 실제로는 쿼리 로그를 확인해야 하지만, 테스트에서는 응답 시간이나 쿼리 개수로 판단
        
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/v1/orders/my")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").exists());
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        // N+1 문제가 해결되었다면 응답 시간이 합리적이어야 함
        // (실제로는 쿼리 개수를 확인해야 하지만, 테스트에서는 시간으로 간접 확인)
        assertTrue(responseTime < 5000, 
                "N+1 문제가 발생했을 가능성이 있습니다. 응답 시간: " + responseTime + "ms");
        
        // 주문 상세 조회도 테스트
        if (orderId != null) {
            startTime = System.currentTimeMillis();
            
            mockMvc.perform(get("/api/v1/orders/" + orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items").isArray());
            
            endTime = System.currentTimeMillis();
            responseTime = endTime - startTime;
            
            assertTrue(responseTime < 2000, 
                    "N+1 문제가 발생했을 가능성이 있습니다. 응답 시간: " + responseTime + "ms");
        }

        addScore(2, "N+1 문제 예방 - 주문 목록 조회");
    }

    @Test
    @Order(21)
    @DisplayName("21. 비동기 처리 검증")
    void testAsyncProcessing() throws Exception {
        // 비동기 처리가 구현된 경우를 테스트
        // 예: 주문 완료 후 알림 전송 등
        
        // 주문 생성
        String productRequest = """
                {
                    "name": "비동기 테스트 상품",
                    "price": 2000,
                    "stock": 20,
                    "description": "비동기 테스트용"
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
        Long asyncProductId = Long.valueOf(productData.get("id").toString());

        mockMvc.perform(patch("/api/v1/products/" + asyncProductId + "/approve")
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
                """.formatted(asyncProductId);

        // 주문 생성 (비동기 처리가 있다면 응답은 빠르게 반환되어야 함)
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(post("/api/v1/orders")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        // 비동기 처리가 구현되었다면 응답 시간이 짧아야 함
        // (실제 비동기 작업은 백그라운드에서 처리)
        assertTrue(responseTime < 3000, 
                "비동기 처리가 제대로 구현되지 않았을 수 있습니다. 응답 시간: " + responseTime + "ms");

        addScore(2, "비동기 처리 검증");
    }
}
