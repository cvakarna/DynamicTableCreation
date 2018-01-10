package com.biz.model;

public class FieldsModified {
	
	private String oldField;
	private String newField;
	private String fieldType;
	private Fields newFieldRecord;
	
	
	public FieldsModified(String oldFieldName, String newFieldName, String fieldType, Fields newFieldRecord) {
		super();
		this.oldField = oldFieldName;
		this.newField = newFieldName;
		this.fieldType = fieldType;
		this.newFieldRecord = newFieldRecord;
	}


	public String getOldField() {
		return oldField;
	}


	public void setOldField(String oldField) {
		this.oldField = oldField;
	}


	public String getNewField() {
		return newField;
	}


	public void setNewField(String newField) {
		this.newField = newField;
	}


	public String getFieldType() {
		return fieldType;
	}


	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}


	public Fields getNewFieldRecord() {
		return newFieldRecord;
	}


	public void setNewFieldRecord(Fields newFieldRecord) {
		this.newFieldRecord = newFieldRecord;
	}
	

}
