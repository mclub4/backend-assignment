package com.codedrill.shoppingmall;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShoppingMallApiTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static int totalScore = 0;
    private static final int MAX_SCORE = 120;
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

        addScore(5, "회원 가입 - 일반 사용자");
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

        addScore(5, "회원 가입 - 관리자");
    }

    @Test
    @Order(3)
    @DisplayName("3. 로그인 - 일반 사용자")
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
    @Order(4)
    @DisplayName("4. 로그인 - 관리자")
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
    @Order(5)
    @DisplayName("5. 상품 등록")
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

        addScore(10, "상품 등록");
    }

    @Test
    @Order(6)
    @DisplayName("6. 상품 목록 조회 (페이지네이션)")
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

        addScore(10, "상품 목록 조회 (페이지네이션)");
    }

    @Test
    @Order(7)
    @DisplayName("7. 상품 단건 조회")
    void testGetProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.name").value("테스트 상품"))
                .andExpect(jsonPath("$.data.price").value(10000))
                .andExpect(jsonPath("$.data.stock").value(100));

        addScore(10, "상품 단건 조회");
    }

    @Test
    @Order(8)
    @DisplayName("8. 상품 수정")
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

        addScore(10, "상품 수정");
    }

    @Test
    @Order(9)
    @DisplayName("9. 상품 승인")
    @WithMockUser(roles = "ADMIN")
    void testApproveProduct() throws Exception {
        mockMvc.perform(patch("/api/v1/products/" + productId + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        addScore(10, "상품 승인");
    }

    @Test
    @Order(10)
    @DisplayName("10. 상품 삭제 (Soft Delete)")
    @WithMockUser(roles = "ADMIN")
    void testDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/v1/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // 삭제된 상품은 조회되지 않아야 함
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isNotFound());

        addScore(10, "상품 삭제 (Soft Delete)");
    }

    // ==================== 주문 관련 테스트 ====================

    @Test
    @Order(11)
    @DisplayName("11. 주문 생성")
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

        addScore(10, "주문 생성");
    }

    @Test
    @Order(12)
    @DisplayName("12. 내 주문 목록 조회")
    @WithMockUser(roles = "USER")
    void testGetMyOrders() throws Exception {
        mockMvc.perform(get("/api/v1/orders/my")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").exists());

        addScore(10, "내 주문 목록 조회");
    }

    @Test
    @Order(13)
    @DisplayName("13. 주문 상세 조회")
    @WithMockUser(roles = "USER")
    void testGetOrder() throws Exception {
        mockMvc.perform(get("/api/v1/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.items").isArray());

        addScore(10, "주문 상세 조회");
    }

    @Test
    @Order(14)
    @DisplayName("14. 주문 결제")
    @WithMockUser(roles = "USER")
    void testPayOrder() throws Exception {
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        addScore(5, "주문 결제");
    }

    @Test
    @Order(15)
    @DisplayName("15. 주문 완료")
    @WithMockUser(roles = "USER")
    void testCompleteOrder() throws Exception {
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        addScore(5, "주문 완료");
    }

    @Test
    @Order(16)
    @DisplayName("16. 주문 취소 (새 주문 생성 후)")
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

        addScore(5, "주문 취소");
    }
}
