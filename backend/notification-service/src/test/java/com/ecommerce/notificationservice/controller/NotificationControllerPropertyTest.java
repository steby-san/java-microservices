package com.ecommerce.notificationservice.controller;

// Feature: notification-service
// Property 5: API filter by orderId correctness  — Validates: Requirements 4.2
// Property 6: API get-by-id round-trip           — Validates: Requirements 4.3, 4.6, 4.7
// Property 8: Error response safety              — Validates: Requirements 7.2, 7.5
// Property 9: Invalid path variable → 400        — Validates: Requirements 4.5, 7.4

import com.ecommerce.notificationservice.dto.NotificationDTO;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.exception.GlobalExceptionHandler;
import com.ecommerce.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based tests for NotificationController.
 * No Spring context — uses standaloneSetup with Jackson converter + GlobalExceptionHandler.
 */
class NotificationControllerPropertyTest {

    private NotificationService notificationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        NotificationController controller = new NotificationController(notificationService);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

        // Configure Jackson with JavaTimeModule so records and LocalDateTime serialize correctly
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .setMessageConverters(converter)
                .build();
    }

    // -----------------------------------------------------------------------
    // Property 6: API Get-By-Id Round-Trip
    // Validates: Requirements 4.3, 4.6, 4.7
    // -----------------------------------------------------------------------

    static Stream<NotificationDTO> notificationDTOVariants() {
        LocalDateTime now = LocalDateTime.of(2024, 6, 15, 12, 0, 0);
        LocalDateTime sentTime = LocalDateTime.of(2024, 6, 15, 12, 1, 30);
        return Stream.of(
            new NotificationDTO(1L, "order-001", "user-1", "alice@example.com",
                "Order confirmed", NotificationStatus.SENT, now, sentTime),
            new NotificationDTO(2L, "order-002", "user-2", "bob@example.com",
                "Order confirmed", NotificationStatus.FAILED, now, null),
            new NotificationDTO(3L, "order-003", "user-3", "user@example.com",
                "Order confirmed", NotificationStatus.INVALID_EMAIL, now, null),
            new NotificationDTO(42L, "order-unicode", "user-uni", "uni@example.com",
                "Subject happy order", NotificationStatus.SENT,
                LocalDateTime.of(2023, 12, 31, 23, 59, 59),
                LocalDateTime.of(2024, 1, 1, 0, 0, 1)),
            new NotificationDTO(100L, "o", "u", "a@b.com", "S",
                NotificationStatus.INVALID_EMAIL,
                LocalDateTime.of(2020, 1, 1, 0, 0, 0), null)
        );
    }

    /**
     * Property 6: GET /api/notifications/{id} returns HTTP 200 with all 8 DTO fields.
     * Validates: Requirements 4.3, 4.6, 4.7
     */
    @ParameterizedTest(name = "Property 6 — variant [{index}]")
    @MethodSource("notificationDTOVariants")
    @DisplayName("Property 6 — GET /api/notifications/{id} returns 200 with all 8 DTO fields matching")
    void property6_getByIdRoundTrip(NotificationDTO dto) throws Exception {
        // Feature: notification-service, Property 6: API get-by-id round-trip
        when(notificationService.getNotificationById(anyLong())).thenReturn(dto);

        var result = mockMvc.perform(get("/api/notifications/{id}", dto.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dto.id()))
                .andExpect(jsonPath("$.orderId").value(dto.orderId()))
                .andExpect(jsonPath("$.userId").value(dto.userId()))
                .andExpect(jsonPath("$.recipientEmail").value(dto.recipientEmail()))
                .andExpect(jsonPath("$.subject").value(dto.subject()))
                .andExpect(jsonPath("$.status").value(dto.status().name()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                // content field is on Entity only, must not appear in DTO response
                .andExpect(jsonPath("$.content").doesNotExist());

        if (dto.sentAt() == null) {
            result.andExpect(jsonPath("$.sentAt").doesNotExist());
        } else {
            result.andExpect(jsonPath("$.sentAt").isNotEmpty());
        }
    }

    // -----------------------------------------------------------------------
    // Property 5: API Filter by OrderId Correctness
    // Validates: Requirements 4.2
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Property 5 — GET /api/notifications?orderId=X returns only matching notifications")
    void property5_filterByOrderId() throws Exception {
        // Feature: notification-service, Property 5: API filter by orderId correctness
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        String targetOrderId = "order-target";

        List<NotificationDTO> filtered = List.of(
            new NotificationDTO(1L, targetOrderId, "u1", "a@x.com", "S1",
                NotificationStatus.SENT, now, now),
            new NotificationDTO(2L, targetOrderId, "u2", "b@x.com", "S2",
                NotificationStatus.SENT, now, now)
        );

        when(notificationService.getNotificationsByOrderId(eq(targetOrderId))).thenReturn(filtered);

        mockMvc.perform(get("/api/notifications").param("orderId", targetOrderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].orderId").value(targetOrderId))
            .andExpect(jsonPath("$[1].orderId").value(targetOrderId));
    }

    // -----------------------------------------------------------------------
    // Property 8: Error Response Safety
    // Validates: Requirements 7.2, 7.5
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Property 8 — RuntimeException returns 500 with safe error body (no stack trace)")
    void property8_runtimeExceptionResponseIsSafe() throws Exception {
        // Feature: notification-service, Property 8: Error response safety
        when(notificationService.getNotificationById(anyLong()))
            .thenThrow(new RuntimeException("internal error detail"));

        mockMvc.perform(get("/api/notifications/999"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
            .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // -----------------------------------------------------------------------
    // Property 9: Invalid Path Variable → 400
    // Validates: Requirements 4.5, 7.4
    // -----------------------------------------------------------------------

    @ParameterizedTest(name = "Property 9 — invalid id: [{0}]")
    @ValueSource(strings = {"abc", "xyz", "one"})
    @DisplayName("Property 9 — Non-integer path variable returns HTTP 400")
    void property9_invalidPathVariable_returns400(String invalidId) throws Exception {
        // Feature: notification-service, Property 9: Invalid path variable → 400
        mockMvc.perform(get("/api/notifications/" + invalidId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }
}
