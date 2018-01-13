package com.biz.model;




public class TableChangeInfo {
	
	private String tableOldName;
	public String getTableOldName() {
		return tableOldName;
	}
	public void setTableOldName(String tableOldName) {
		this.tableOldName = tableOldName;
	}
	public String getTableNewName() {
		return tableNewName;
	}
	public void setTableNewName(String tableNewName) {
		this.tableNewName = tableNewName;
	}
	public String getTableNewDescription() {
		return tableNewDescription;
	}
	public void setTableNewDescription(String tableNewDescription) {
		this.tableNewDescription = tableNewDescription;
	}
	private String tableNewName;
	private String tableNewDescription;
	public TableChangeInfo()
	{
		
	}
	

}
