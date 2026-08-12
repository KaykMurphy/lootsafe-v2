package com.lootsafe.exception;

import com.lootsafe.dto.response.ErrorResponse;
import com.lootsafe.dto.response.ValidationErrorResponse;
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
                                                                HttpServletRequest request){

        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorResponse error = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(error); //404

    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex,
                                                        HttpServletRequest request){

        HttpStatus status = HttpStatus.BAD_REQUEST;

        ErrorResponse error = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(error); //400

    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        ErrorResponse error = new ErrorResponse(
                status.value(),
                "Forbidden",
                "Acesso negado. Você não tem permissão para este recurso.",
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnautrorized(UnauthorizedException ex,
                                                            HttpServletRequest request){

        HttpStatus status = HttpStatus.FORBIDDEN;

        ErrorResponse error = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(error); //403

    }

    @ExceptionHandler(MPInvalidWebhookSignatureException.class)
    public ResponseEntity<Void> handleInvalidWebhookSignature(MPInvalidWebhookSignatureException ex,
                                                              HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(PaymentProviderException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProvider(PaymentProviderException ex,
                                                               HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_GATEWAY;

        ErrorResponse error = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(error); //502
    }

    @ExceptionHandler(EncryptionException.class)
    public ResponseEntity<ErrorResponse> hadnleEncryption(EncryptionException ex,
                                                          HttpServletRequest request){

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;


        ErrorResponse error = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(error); // 500

    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                                HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        List<ValidationErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationErrorResponse.FieldErrorDetail(error.getField(),
                        error.getDefaultMessage()))
                .toList();

        ValidationErrorResponse response = new ValidationErrorResponse(
                status.value(),
                "Validation Failed",
                "Um ou mais campos estão inválidos. Preencha corretamente e tente novamente.",
                fieldErrors,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response); // 400 valid
    }
}
