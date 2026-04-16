package org.example.goldenheartrestaurant.common.exception;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
/**
 * Maps validation, security and business exceptions into a consistent JSON error contract.
 */
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .success(false)
                        .message("Validation failed")
                        .errors(fieldErrors)
                        .build()
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .success(false)
                        .message(exception.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception) {
        String rootMessage = exception.getMostSpecificCause() != null
                ? exception.getMostSpecificCause().getMessage()
                : exception.getMessage();

        log.warn("Data integrity violation: {}", rootMessage);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .success(false)
                        .message(resolveDataIntegrityMessage(rootMessage))
                        .build()
        );
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder()
                        .success(false)
                        .message(exception.getMessage())
                        .build()
        );
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(Exception exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.builder()
                        .success(false)
                        .message(exception.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .success(false)
                        .message("Username or password is incorrect")
                        .build()
        );
    }

    @ExceptionHandler({JwtException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleTokenErrors(RuntimeException exception) {
        // IllegalArgumentException is also used for malformed refresh-cookie input.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .success(false)
                        .message(exception.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String parameterName = exception.getName();
        String rejectedValue = exception.getValue() == null ? "null" : String.valueOf(exception.getValue());

        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .success(false)
                        .message(buildTypeMismatchMessage(parameterName, rejectedValue))
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected server error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .success(false)
                        .message("Unexpected server error")
                        .build()
        );
    }

    private String resolveDataIntegrityMessage(String rootMessage) {
        if (rootMessage == null || rootMessage.isBlank()) {
            return "Data integrity violation";
        }

        if (rootMessage.contains("uk_inventory_branch_ingredient_active")
                || rootMessage.contains("uk_inventory_branch_ingredient")) {
            return "Chi nhanh nay da co inventory item cho nguyen lieu nay";
        }

        if (rootMessage.contains("uk_ingredients_name") || rootMessage.contains("ingredients.name")) {
            return "Ten nguyen lieu da ton tai";
        }

        if (rootMessage.contains("uk_menu_items_branch_category_name")) {
            return "Ten mon da ton tai trong chi nhanh va danh muc nay";
        }

        if (rootMessage.contains("uk_refresh_tokens_token_hash")) {
            return "Refresh token hash bi trung";
        }

        return "Data integrity violation";
    }

    private String buildTypeMismatchMessage(String parameterName, String rejectedValue) {
        if (rejectedValue != null && rejectedValue.matches("\\{[^{}]+}")) {
            return "Invalid parameter '" + parameterName + "': received '" + rejectedValue
                    + "'. This usually means a Postman path/query variable was not resolved.";
        }

        return "Invalid value for parameter '" + parameterName + "': " + rejectedValue;
    }
}
