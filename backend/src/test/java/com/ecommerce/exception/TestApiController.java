package com.ecommerce.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller used exclusively in tests to verify GlobalExceptionHandler responses.
 */
@RestController
@RequestMapping("/test/api")
public class TestApiController {

    public record SampleRequest(
            @NotBlank(message = "Title cannot be blank")
            String title,

            @Min(value = 1, message = "Amount must be at least 1")
            int amount
    ) {}

    @PostMapping("/validate")
    public String testValidation(@Valid @RequestBody SampleRequest request) {
        return "OK";
    }

    @GetMapping("/param")
    public String testParam(@RequestParam("requiredField") String requiredField) {
        return "Received: " + requiredField;
    }

    @GetMapping("/not-found")
    public String testNotFound() {
        throw new ResourceNotFoundException("Resource with ID 12345 was not found.");
    }

    @GetMapping("/conflict")
    public String testConflict() {
        throw new ConflictException("Resource already exists with email: test@example.com");
    }

    @GetMapping("/server-error")
    public String testServerError() {
        throw new RuntimeException("Database connection password=SECRET_DB_PASS failed at internal socket");
    }
}

