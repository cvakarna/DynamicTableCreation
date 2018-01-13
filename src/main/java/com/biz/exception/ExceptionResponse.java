package com.biz.exception;

public class ExceptionResponse {
   
	public ExceptionResponse() {
		// TODO Auto-generated constructor stub
	}
	 public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	private String errorCode;
	 private String errorMessage;
}
