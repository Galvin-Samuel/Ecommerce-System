package com.ecommerce.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Automated tests for verifying the health controller endpoints:
 * 1. GET /api/health (Application process liveness)
 * 2. GET /api/health/dependencies (External dependency readiness)
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DependencyHealthService dependencyHealthService;

    @Test
    @DisplayName("GET /api/health should return HTTP 200 with UP status and application name")
    void healthCheckShouldReturnOkAndApplicationStatus() throws Exception {
        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.application", is("ecommerce-backend")));
    }

    @Test
    @DisplayName("GET /api/health/dependencies should return HTTP 200 when all dependencies are UP")
    void dependencyHealthShouldReturnOkWhenAllUp() throws Exception {
        when(dependencyHealthService.checkDependencies())
                .thenReturn(new DependencyHealthResponse("ecommerce-backend", "UP", "UP"));

        mockMvc.perform(get("/api/health/dependencies")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.application", is("ecommerce-backend")))
                .andExpect(jsonPath("$.mongodb", is("UP")))
                .andExpect(jsonPath("$.redis", is("UP")));
    }

    @Test
    @DisplayName("GET /api/health/dependencies should return HTTP 503 when MongoDB is DOWN")
    void dependencyHealthShouldReturnServiceUnavailableWhenMongoDown() throws Exception {
        when(dependencyHealthService.checkDependencies())
                .thenReturn(new DependencyHealthResponse("ecommerce-backend", "DOWN", "UP"));

        mockMvc.perform(get("/api/health/dependencies")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.application", is("ecommerce-backend")))
                .andExpect(jsonPath("$.mongodb", is("DOWN")))
                .andExpect(jsonPath("$.redis", is("UP")));
    }

    @Test
    @DisplayName("GET /api/health/dependencies should return HTTP 503 when Redis is DOWN")
    void dependencyHealthShouldReturnServiceUnavailableWhenRedisDown() throws Exception {
        when(dependencyHealthService.checkDependencies())
                .thenReturn(new DependencyHealthResponse("ecommerce-backend", "UP", "DOWN"));

        mockMvc.perform(get("/api/health/dependencies")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.application", is("ecommerce-backend")))
                .andExpect(jsonPath("$.mongodb", is("UP")))
                .andExpect(jsonPath("$.redis", is("DOWN")));
    }

    @Test
    @DisplayName("GET /api/health/dependencies should return HTTP 503 when both dependencies are DOWN")
    void dependencyHealthShouldReturnServiceUnavailableWhenAllDown() throws Exception {
        when(dependencyHealthService.checkDependencies())
                .thenReturn(new DependencyHealthResponse("ecommerce-backend", "DOWN", "DOWN"));

        mockMvc.perform(get("/api/health/dependencies")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.application", is("ecommerce-backend")))
                .andExpect(jsonPath("$.mongodb", is("DOWN")))
                .andExpect(jsonPath("$.redis", is("DOWN")));
    }
}
