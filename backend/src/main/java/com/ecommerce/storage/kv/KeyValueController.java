package com.ecommerce.storage.kv;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/**
 * REST controller exposing key-value store operations for diagnostic inspection
 * and operational demonstration.
 */
@RestController
@RequestMapping("/api/v1/kv")
public class KeyValueController {

    private final KeyValueStore<String, String> keyValueStore;

    public KeyValueController(KeyValueStore<String, String> keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<String>> put(
            @PathVariable String key,
            @RequestBody String value,
            @RequestParam(required = false) Long ttlSeconds
    ) {
        if (ttlSeconds != null && ttlSeconds > 0) {
            keyValueStore.put(key, value, Duration.ofSeconds(ttlSeconds));
        } else {
            keyValueStore.put(key, value);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Key '" + key + "' stored successfully", value));
    }

    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<String>> get(@PathVariable String key) {
        String value = keyValueStore.get(key)
                .orElseThrow(() -> new ResourceNotFoundException("Key '" + key + "' not found or expired"));
        return ResponseEntity.ok(ApiResponse.success("Key '" + key + "' retrieved successfully", value));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<ApiResponse<Boolean>> delete(@PathVariable String key) {
        boolean deleted = keyValueStore.delete(key);
        if (!deleted) {
            throw new ResourceNotFoundException("Key '" + key + "' not found to delete");
        }
        return ResponseEntity.ok(ApiResponse.success("Key '" + key + "' deleted successfully", true));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview() {
        Map<String, Object> overview = Map.of(
                "size", keyValueStore.size(),
                "storageType", "InMemoryKeyValueStore (Volatile)"
        );
        return ResponseEntity.ok(ApiResponse.success("Key-value store overview", overview));
    }
}
