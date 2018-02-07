package com.biz.model;

public class TableDetails {
 
	private String tableName;
	private String createdBy;
	public String getCreatedBy() {
		return createdBy;
	}



	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}



	public String getCreatedOn() {
		return createdOn;
	}



	public void setCreatedOn(String createdOn) {
		this.createdOn = createdOn;
	}



	public String getModifiedOn() {
		return modifiedOn;
	}



	public void setModifiedOn(String modifiedOn) {
		this.modifiedOn = modifiedOn;
	}



	public String getModifiedBy() {
		return modifiedBy;
	}



	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}



	private String createdOn;
	private String modifiedOn;
	private String modifiedBy;
	public String getTableName() {
		return tableName;
	}



	public void setTableName(String tableName) {
		this.tableName = tableName;
	}



	public String getTableDescription() {
		return tableDescription;
	}



	public void setTableDescription(String tableDescription) {
		this.tableDescription = tableDescription;
	}



	private String tableDescription;
	public TableDetails(String tableName, String tableDescription) {
		super();
		this.tableName = tableName;
		this.tableDescription = tableDescription;
	}

	
	
	public TableDetails() {
		// TODO Auto-generated constructor stub
	}
	
}
