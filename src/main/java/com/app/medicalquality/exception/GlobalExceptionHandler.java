package com.app.medicalquality.exception;

import com.app.medicalquality.common.AppResponse;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource a_messageSource) {
        this.messageSource = a_messageSource;
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<AppResponse<Object>> handleAppException(AppException a_exception) {
        return AppResponse.failure(
                resolve(a_exception.getMessageKey(), a_exception.getArgs()),
                a_exception.getData(),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AppResponse<Object>> handleValidation(MethodArgumentNotValidException a_exception) {
        FieldError fieldError = a_exception.getBindingResult().getFieldError();
        String message = fieldError == null ? null : fieldError.getDefaultMessage();
        if (message == null || message.isBlank()) {
            message = resolve("error.validation.failed", null);
        }
        return AppResponse.failure(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AppResponse<Object>> handleAccessDenied(AccessDeniedException a_exception) {
        return AppResponse.failure(resolve("error.auth.access_denied", null), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AppResponse<Object>> handleException(Exception a_exception) {
        log.error("Unhandled exception", a_exception);
        return AppResponse.failure(resolve("error.internal", null), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String resolve(String a_key, Object[] a_args) {
        return messageSource.getMessage(a_key, a_args, a_key, Locale.ENGLISH);
    }
}
