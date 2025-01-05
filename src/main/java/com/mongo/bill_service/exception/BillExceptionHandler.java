package com.mongo.bill_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletResponse;

@RestControllerAdvice
public class BillExceptionHandler {

	@ExceptionHandler(exception = BillException.class)
	public ResponseEntity<BillExceptionResponse> handleBillException(HttpServletResponse response,
			BillException billException) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new BillExceptionResponse(billException));
	}
}
