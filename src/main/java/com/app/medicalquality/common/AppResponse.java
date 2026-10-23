package com.app.medicalquality.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
@Setter
public class AppResponse<T> {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String message;
    private String time;
    private T data;

    public AppResponse(T a_data, String a_message) {
        this.message = a_message;
        this.data = a_data;
        this.time = currentTimestamp();
    }

    public AppResponse(String a_message, int a_statusCode) {
        this.message = a_message;
        this.time = currentTimestamp();
    }

    public static <T> ResponseEntity<AppResponse<T>> success(T a_data) {
        return ResponseEntity.ok(new AppResponse<>(a_data, "Successfully Done"));
    }

    public static <T> ResponseEntity<AppResponse<T>> success(String a_message) {
        return ResponseEntity.ok(new AppResponse<>(a_message, HttpStatus.OK.value()));
    }

    public static <T> ResponseEntity<AppResponse<T>> success(T a_data, String a_message) {
        return ResponseEntity.ok(new AppResponse<>(a_data, a_message));
    }

    public static <T> ResponseEntity<AppResponse<T>> success(T a_data, String a_message, HttpStatus a_status) {
        return ResponseEntity.status(a_status).body(new AppResponse<>(a_data, a_message));
    }

    public static <T> ResponseEntity<AppResponse<T>> failure(String a_message, HttpStatus a_status) {
        return ResponseEntity.status(a_status).body(new AppResponse<>(null, a_message));
    }

    public static <T> ResponseEntity<AppResponse<T>> failure(String a_message, T a_data, HttpStatus a_status) {
        return ResponseEntity.status(a_status).body(new AppResponse<>(a_data, a_message));
    }

    private static String currentTimestamp() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }
}
