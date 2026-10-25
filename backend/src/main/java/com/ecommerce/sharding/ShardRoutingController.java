package com.ecommerce.sharding;

import com.ecommerce.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * REST controller providing topology inspection, key routing diagnostics,
 * and non-destructive rebalance simulation.
 */
@RestController
@RequestMapping("/api/v1/shards")
public class ShardRoutingController {

    private final ShardRouter shardRouter;
    private final ShardRebalanceService rebalanceService;

    public ShardRoutingController(ShardRouter shardRouter, ShardRebalanceService rebalanceService) {
        this.shardRouter = shardRouter;
        this.rebalanceService = rebalanceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Collection<LogicalShard>>> getShards() {
        return ResponseEntity.ok(ApiResponse.success(
                "Active logical cluster shards",
                shardRouter.getRegisteredShards()
        ));
    }

    @GetMapping("/route/{key}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> routeKey(@PathVariable String key) {
        LogicalShard primary = shardRouter.routeKey(key);
        List<LogicalShard> replicas = shardRouter.routeKeyWithReplicas(key, 2);

        Map<String, Object> routingInfo = Map.of(
                "partitionKey", key,
                "primaryShard", primary.getShardId(),
                "connectionUri", primary.getConnectionUri(),
                "replicas", replicas.stream().map(LogicalShard::getShardId).toList()
        );

        return ResponseEntity.ok(ApiResponse.success("Key routing resolved", routingInfo));
    }

    @PostMapping("/simulate-rebalance")
    public ResponseEntity<ApiResponse<RebalancePlan>> simulateRebalance(
            @RequestParam(defaultValue = "shard-new") String newShardId,
            @RequestParam(defaultValue = "500") int sampleKeyCount
    ) {
        List<String> keys = new ArrayList<>(sampleKeyCount);
        for (int i = 0; i < sampleKeyCount; i++) {
            keys.add("customer_user_" + i);
        }

        RebalancePlan plan = rebalanceService.simulateAddShard(new LogicalShard(newShardId), keys);
        return ResponseEntity.ok(ApiResponse.success("Shard rebalance simulation complete", plan));
    }
}
