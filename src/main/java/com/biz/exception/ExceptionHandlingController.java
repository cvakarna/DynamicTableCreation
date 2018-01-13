package com.biz.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ExceptionHandlingController extends ResponseEntityExceptionHandler {

	@ExceptionHandler(DataTypeLengthFormatException.class)
	public ResponseEntity<ExceptionResponse> lengthShouldNotZure(DataTypeLengthFormatException ex)
	{
		ExceptionResponse response = new ExceptionResponse();
		 response.setErrorCode("Forbidden");
	     response.setErrorMessage(ex.getMessage());
	     return new ResponseEntity<ExceptionResponse>(response, HttpStatus.FORBIDDEN);
	}
}
