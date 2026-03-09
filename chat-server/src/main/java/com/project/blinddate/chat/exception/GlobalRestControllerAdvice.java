package com.project.blinddate.chat.exception;

import com.project.blinddate.common.dto.ResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalRestControllerAdvice {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseDto<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument exception: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseDto.of(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ResponseDto<Void>> handleValidationException(Exception e) {
        String message = "잘못된 요청 파라미터입니다.";
        if (e instanceof MethodArgumentNotValidException) {
            message = ((MethodArgumentNotValidException) e).getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else if (e instanceof BindException) {
            message = ((BindException) e).getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        log.warn("Validation exception: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseDto.of(HttpStatus.BAD_REQUEST.value(), message, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDto<Void>> handleException(Exception e) {
        log.error("Internal server error: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDto.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류가 발생했습니다.", null));
    }
}
