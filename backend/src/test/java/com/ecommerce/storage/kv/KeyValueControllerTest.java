package com.ecommerce.storage.kv;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KeyValueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KeyValueStore<String, String> keyValueStore;

    @Test
    @DisplayName("Should PUT and GET a key-value entry via REST API")
    void shouldPutAndGetEntryViaApi() throws Exception {
        String testKey = "api_test_key_1";
        String testValue = "hello_system_design";

        // PUT /api/v1/kv/{key}
        mockMvc.perform(put("/api/v1/kv/{key}", testKey)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(testValue))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(testValue)));

        // GET /api/v1/kv/{key}
        mockMvc.perform(get("/api/v1/kv/{key}", testKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(testValue)));
    }

    @Test
    @DisplayName("Should return HTTP 404 when GET on a non-existent key")
    void shouldReturn404ForMissingKey() throws Exception {
        mockMvc.perform(get("/api/v1/kv/definitely_missing_key_999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    @DisplayName("Should DELETE an existing key via REST API")
    void shouldDeleteKeyViaApi() throws Exception {
        String testKey = "api_to_delete";
        keyValueStore.put(testKey, "to_be_deleted");

        mockMvc.perform(delete("/api/v1/kv/{key}", testKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(true)));

        // Verify subsequent GET returns 404
        mockMvc.perform(get("/api/v1/kv/{key}", testKey))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return overview metadata on GET /api/v1/kv")
    void shouldReturnStoreOverview() throws Exception {
        mockMvc.perform(get("/api/v1/kv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.storageType", is("InMemoryKeyValueStore (Volatile)")));
    }
}
