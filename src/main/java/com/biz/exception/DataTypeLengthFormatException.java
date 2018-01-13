package com.biz.exception;

public class DataTypeLengthFormatException extends RuntimeException{
   
	private String type;
	public DataTypeLengthFormatException(String type,String message) {
		super(message);
		this.type = type;
	}
}
