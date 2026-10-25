package com.ecommerce.id;

import com.ecommerce.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for distributed unique ID generation and decomposition.
 */
@RestController
@RequestMapping("/api/v1/ids")
public class IdGeneratorController {

    private final IdGenerator idGenerator;

    public IdGeneratorController(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    /**
     * Generates a single distributed unique identifier.
     */
    @GetMapping("/next")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNextId() {
        long id = idGenerator.nextId();
        Map<String, Object> response = Map.of(
                "id", id,
                "idString", String.valueOf(id),
                "datacenterId", idGenerator.getDatacenterId(),
                "workerId", idGenerator.getWorkerId()
        );
        return ResponseEntity.ok(ApiResponse.success("ID generated successfully", response));
    }

    /**
     * Generates a batch of unique identifiers sequentially.
     */
    @GetMapping("/batch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIdBatch(
            @RequestParam(defaultValue = "10") int count) {
        if (count < 1 || count > 1000) {
            throw new IllegalArgumentException("Batch count must be between 1 and 1000");
        }
        List<Long> ids = idGenerator.nextIdBatch(count);
        List<String> idStrings = ids.stream().map(String::valueOf).toList();

        Map<String, Object> response = Map.of(
                "count", ids.size(),
                "ids", ids,
                "idStrings", idStrings,
                "datacenterId", idGenerator.getDatacenterId(),
                "workerId", idGenerator.getWorkerId()
        );
        return ResponseEntity.ok(ApiResponse.success("ID batch generated successfully", response));
    }

    /**
     * Decomposes a 64-bit ID into its timestamp, datacenter, worker, and sequence components.
     */
    @GetMapping("/decode/{id}")
    public ResponseEntity<ApiResponse<IdDecomposition>> decodeId(@PathVariable long id) {
        IdDecomposition decomposition = idGenerator.parseId(id);
        return ResponseEntity.ok(ApiResponse.success("ID decoded successfully", decomposition));
    }
}
