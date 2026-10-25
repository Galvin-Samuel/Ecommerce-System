package com.ecommerce.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IdGeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdGenerator idGenerator;

    @Test
    @DisplayName("GET /api/v1/ids/next should generate a valid positive 64-bit ID")
    void testGetNextId() throws Exception {
        mockMvc.perform(get("/api/v1/ids/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.idString", notNullValue()))
                .andExpect(jsonPath("$.data.datacenterId").value(idGenerator.getDatacenterId()))
                .andExpect(jsonPath("$.data.workerId").value(idGenerator.getWorkerId()));
    }

    @Test
    @DisplayName("GET /api/v1/ids/batch should generate requested batch of IDs")
    void testGetIdBatch() throws Exception {
        mockMvc.perform(get("/api/v1/ids/batch").param("count", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(5))
                .andExpect(jsonPath("$.data.ids", hasSize(5)))
                .andExpect(jsonPath("$.data.idStrings", hasSize(5)));
    }

    @Test
    @DisplayName("GET /api/v1/ids/decode/{id} should decompose an existing ID")
    void testDecodeId() throws Exception {
        long id = idGenerator.nextId();

        mockMvc.perform(get("/api/v1/ids/decode/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.datacenterId").value(idGenerator.getDatacenterId()))
                .andExpect(jsonPath("$.data.workerId").value(idGenerator.getWorkerId()))
                .andExpect(jsonPath("$.data.instant", notNullValue()))
                .andExpect(jsonPath("$.data.sequence", notNullValue()));
    }
}
