package com.ecommerce.notificationservice.controller;

// Feature: notification-service
// Example: 404 Not Found           — Validates: Yeu cau 4.4
// Example: GET all returns []      — Validates: Yeu cau 4.1

import com.ecommerce.notificationservice.exception.GlobalExceptionHandler;
import com.ecommerce.notificationservice.exception.ResourceNotFoundException;
import com.ecommerce.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Example-based tests for NotificationController edge cases.
 * No Spring context — uses standaloneSetup with Jackson converter + GlobalExceptionHandler.
 * Validates: Yeu cau 4.1, 4.4
 */
class NotificationControllerExampleTest {

    private NotificationService notificationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        NotificationController controller = new NotificationController(notificationService);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

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

    /**
     * Task 14.5 — GET /api/notifications/99 when not found returns HTTP 404.
     * Validates: Yeu cau 4.4
     */
    @Test
    @DisplayName("Example: GET /api/notifications/{id} returns 404 when not found")
    void getById_notFound_returns404() throws Exception {
        when(notificationService.getNotificationById(99L))
            .thenThrow(new ResourceNotFoundException("Notification not found with id: 99"));

        mockMvc.perform(get("/api/notifications/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Notification not found with id: 99"));
    }

    /**
     * Task 14.6 — GET /api/notifications returns 200 and empty array when no data.
     * Validates: Yeu cau 4.1
     */
    @Test
    @DisplayName("Example: GET /api/notifications returns 200 with empty array when no data")
    void getAll_noData_returns200WithEmptyList() throws Exception {
        when(notificationService.getAllNotifications()).thenReturn(List.of());

        mockMvc.perform(get("/api/notifications"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }
}
