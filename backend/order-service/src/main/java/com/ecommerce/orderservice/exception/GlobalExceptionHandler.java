package com.ecommerce.orderservice.exception;

import org.springframework.http.*;
import com.ecommerce.orderservice.dto.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
            ResourceNotFoundException.class
    )
    public ResponseEntity<ErrorResponse>
    handleNotFound(
            ResourceNotFoundException ex
    ) {

        return ResponseEntity.status(
                HttpStatus.NOT_FOUND
        ).body(
                ErrorResponse.builder()
                        .status(404)
                        .message(ex.getMessage())
                        .timestamp(
                                LocalDateTime.now()
                        )
                        .build()
        );
    }

    @ExceptionHandler(
            BadRequestException.class
    )
    public ResponseEntity<ErrorResponse>
    handleBadRequest(
            BadRequestException ex
    ) {

        return ResponseEntity.status(
                HttpStatus.BAD_REQUEST
        ).body(
                ErrorResponse.builder()
                        .status(400)
                        .message(ex.getMessage())
                        .timestamp(
                                LocalDateTime.now()
                        )
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse>
    handleException(
            Exception ex
    ) {

        return ResponseEntity.status(
                HttpStatus.INTERNAL_SERVER_ERROR
        ).body(
                ErrorResponse.builder()
                        .status(500)
                        .message(ex.getMessage())
                        .timestamp(
                                LocalDateTime.now()
                        )
                        .build()
        );
    }
}