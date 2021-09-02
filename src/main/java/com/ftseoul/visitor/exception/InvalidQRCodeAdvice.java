package com.ftseoul.visitor.exception;

import com.ftseoul.visitor.dto.ErrorResponseDto;
import com.ftseoul.visitor.dto.QRCheckResponseDto;
import com.ftseoul.visitor.dto.payload.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class InvalidQRCodeAdvice {

    private final String NotFoundCode = "4040";

    @ExceptionHandler(InvalidQRCodeException.class)
    @ResponseStatus(HttpStatus.OK)
    public QRCheckResponseDto InvalidQRCodeExceptionHandler(InvalidQRCodeException ex) {
        return new QRCheckResponseDto(NotFoundCode, ex.getMessage(), "무효");
    }
}