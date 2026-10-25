package com.ecommerce.sharding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShardRoutingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should retrieve active cluster shards on GET /api/v1/shards")
    void shouldListClusterShards() throws Exception {
        mockMvc.perform(get("/api/v1/shards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(3)));
    }

    @Test
    @DisplayName("Should resolve target shard and replicas on GET /api/v1/shards/route/{key}")
    void shouldRouteKeyViaApi() throws Exception {
        mockMvc.perform(get("/api/v1/shards/route/{key}", "usr-test-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.partitionKey", is("usr-test-101")))
                .andExpect(jsonPath("$.data.primaryShard", notNullValue()))
                .andExpect(jsonPath("$.data.replicas", hasSize(2)));
    }

    @Test
    @DisplayName("Should simulate scale-up rebalancing on POST /api/v1/shards/simulate-rebalance")
    void shouldSimulateRebalanceViaApi() throws Exception {
        mockMvc.perform(post("/api/v1/shards/simulate-rebalance")
                        .param("newShardId", "shard-elastic-99")
                        .param("sampleKeyCount", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.action", is("ADD_SHARD")))
                .andExpect(jsonPath("$.data.targetShardId", is("shard-elastic-99")))
                .andExpect(jsonPath("$.data.totalKeysAnalyzed", is(200)));
    }
}
