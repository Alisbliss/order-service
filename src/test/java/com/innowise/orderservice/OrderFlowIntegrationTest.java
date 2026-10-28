package com.innowise.orderservice;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import com.innowise.orderservice.dto.OrderCreateRequest;
import com.innowise.orderservice.dto.OrderItemCreateRequest;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class OrderFlowIntegrationTest {
    static WireMockServer wireMock;

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.cache.type", () -> "none");
        registry.add("user-service.url", () -> "http://localhost:" + wireMock.port());
        registry.add("jwt.secret", () -> "my-super-secret-key-that-is-at-least-256-bits-long-12345");
    }

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(0);
        wireMock.start();
        WireMock.configureFor("localhost", wireMock.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @AfterEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ItemRepository itemRepository;

    private String userToken;
    private Long itemId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();

        Item item = itemRepository.save(Item.builder()
                .name("Mouse").price(new BigDecimal("20.02")).build());
        this.itemId = item.getId();

        this.userToken = generateToken(5L, "USER");

        //200
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/api/users/email/.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {"id":5,"name":"Alex","surname":"Aleksandrov",
                         "birthDate":"1989-04-08","email":"user@example.com","active":true}
                        """)));
    }

    private String generateToken(Long userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(
                "my-super-secret-key-that-is-at-least-256-bits-long-12345"
                        .getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("user@example.com")
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900000))
                .signWith(key)
                .compact();
    }

    private OrderCreateRequest createRequest() {
        return OrderCreateRequest.builder()
                .userEmail("user@example.com")
                .items(List.of(OrderItemCreateRequest.builder()
                        .itemId(itemId).quantity(2).build()))
                .build();
    }

    //full flow
    @Test
    void fullOrderFlow_shouldCreateGetUpdateDelete() throws Exception {
        //CREATE-201
        String json = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.totalPrice").value(40.04))
                .andExpect(jsonPath("$.user.id").value(5))
                .andExpect(jsonPath("$.items[0].itemName").value("Mouse"))
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(json).get("id").asLong();

        //verify user-service called once
        wireMock.verify(moreThanOrExactly(1), WireMock.getRequestedFor(WireMock.urlPathMatching("/api/users/email/.*")));

        //GET-200
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.user.email").value("user@example.com"));

        //UPDATE-200
        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PAID","items":[{"itemId":%d,"quantity":3}]}
                                """.formatted(itemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.totalPrice").value(60.06));

        //DELETE-204
        mockMvc.perform(delete("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        //GET after soft delete-404
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    //401-without token
    @Test
    void create_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnauthorized());
    }

    //400-validation
    @Test
    void create_shouldReturn400WhenValidationFails() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\":\"\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    //404-order not found
    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", 999)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    //503 user-service unavailable (WireMock 500)
    @Test
    void create_shouldReturn503WhenUserServiceUnavailable() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/api/users/email/.*"))
                .willReturn(WireMock.aResponse().withStatus(500)));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isServiceUnavailable());
    }

    //403-USER on getAll
    @Test
    void getAll_shouldReturn403ForUser() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    //200-ADMIN on getAll
    @Test
    void getAll_shouldReturn200ForAdmin() throws Exception {
        String adminToken = generateToken(1L, "ADMIN");

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}