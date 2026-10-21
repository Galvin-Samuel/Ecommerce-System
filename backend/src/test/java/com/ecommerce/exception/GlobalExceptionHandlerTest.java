package com.ecommerce.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Payload validation failure should return HTTP 400 with structured field errors")
    void shouldReturnBadRequestWithFieldErrorsOnValidationFailure() throws Exception {
        String invalidJson = """
                {
                    "title": "",
                    "amount": 0
                }
                """;

        mockMvc.perform(post("/test/api/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.path", is("/test/api/validate")))
                .andExpect(jsonPath("$.validationErrors", hasSize(2)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Malformed JSON request should return HTTP 400 without technical stack trace")
    void shouldReturnBadRequestOnMalformedJson() throws Exception {
        String malformedJson = "{\"title\": \"Incomplete JSON...";

        mockMvc.perform(post("/test/api/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Malformed JSON request body")));
    }

    @Test
    @DisplayName("Missing required query parameter should return HTTP 400")
    void shouldReturnBadRequestOnMissingQueryParam() throws Exception {
        mockMvc.perform(get("/test/api/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Required query parameter 'requiredField' is missing")));
    }

    @Test
    @DisplayName("Unsupported HTTP method should return HTTP 405 Method Not Allowed")
    void shouldReturnMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/test/api/param"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status", is(405)))
                .andExpect(jsonPath("$.error", is("Method Not Allowed")))
                .andExpect(jsonPath("$.message", containsString("HTTP method 'POST' is not supported")));
    }

    @Test
    @DisplayName("ResourceNotFoundException should return HTTP 404 Not Found")
    void shouldReturnNotFoundOnResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/test/api/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Resource with ID 12345 was not found.")))
                .andExpect(jsonPath("$.path", is("/test/api/not-found")));
    }

    @Test
    @DisplayName("ConflictException should return HTTP 409 Conflict")
    void shouldReturnConflictOnConflictException() throws Exception {
        mockMvc.perform(get("/test/api/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("Resource already exists")));
    }

    @Test
    @DisplayName("Unhandled unexpected exception should return HTTP 500 without leaking sensitive internal details")
    void shouldReturnInternalServerErrorWithoutSensitiveDataLeak() throws Exception {
        mockMvc.perform(get("/test/api/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("An unexpected internal server error occurred. Please contact support if the issue persists.")))
                .andExpect(jsonPath("$.message", not(containsString("SECRET_DB_PASS"))))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}

