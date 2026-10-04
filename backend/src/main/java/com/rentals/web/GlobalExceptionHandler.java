package com.rentals.web;

import com.rentals.service.InvalidRefreshTokenException;
import com.rentals.service.TenantNotFoundException;
import com.rentals.service.UsernameTakenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Maps exceptions to ProblemDetail (RFC 9457) responses.
 * ARCHITECTURE §4.2: GlobalExceptionHandler.
 *
 * Note: additional handlers for TenantNotFoundException, etc. will be added in B-04.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 401 for bad credentials (login or changePassword).
     * Always returns the same generic message to avoid user enumeration.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setType(URI.create("about:blank"));
        problem.setTitle("Unauthorized");
        problem.setDetail("Usuario o contraseña incorrectos");
        return problem;
    }

    /**
     * 401 for invalid or expired refresh tokens.
     */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setType(URI.create("about:blank"));
        problem.setTitle("Unauthorized");
        problem.setDetail("Token de sesión inválido o expirado");
        return problem;
    }

    /**
     * 409 for duplicate username.
     */
    @ExceptionHandler(UsernameTakenException.class)
    public ProblemDetail handleUsernameTaken(UsernameTakenException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setType(URI.create("about:blank"));
        problem.setTitle("Conflict");
        problem.setDetail("El nombre de usuario ya está en uso");
        return problem;
    }

    /**
     * 404 for tenant (or tenant-owned resource) not found.
     */
    @ExceptionHandler(TenantNotFoundException.class)
    public ProblemDetail handleTenantNotFound(TenantNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create("about:blank"));
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    /**
     * 400 for invalid path variable format (e.g. malformed YYYY-MM month).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("about:blank"));
        problem.setTitle("Bad Request");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    /**
     * 400 for Bean Validation failures. Includes field-level errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("about:blank"));
        problem.setTitle("Bad Request");
        problem.setDetail("Validación fallida");

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.of("field", fe.getField(), "message",
                fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "inválido"))
            .toList();
        problem.setProperty("errors", errors);
        return problem;
    }
}
