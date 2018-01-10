package com.biz.demo;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.web.bind.annotation.CrossOrigin;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

@CrossOrigin(origins = "http://localhost:4201")
@RestController
public class HomeController {

	// DB Connection
	@Autowired
	JdbcTemplate sql;

	@Autowired
	TableUtil util;

	
	@CrossOrigin(origins = "http://localhost:4201")
	@RequestMapping(value = "/read1", method = RequestMethod.GET)
	public String checkUserExists(@RequestParam(value = "userName") String userName,@RequestParam(value = "ApiKey") String ApiKey) {
		// String dbName = "springbootdb";
		String dbName = "mastercompanies";
		String tableName = "T100";
		String tableSpace="";
		String userMaster="";
		String columnInfoQuery = "SELECT TABLESPACE,USERMASTER FROM "+ dbName+"."+tableName +"  WHERE APIKEY = "+ApiKey;
		SqlRowSet rowSet = sql.queryForRowSet(columnInfoQuery);
		int num = rowSet.getRow();
		while(rowSet.next())
		{
			
			 tableSpace = rowSet.getNString("TABLESPACE");
			 userMaster = rowSet.getString("USERMASTER");
			 
			 
		}
	//	String result = this.util.DisplayTableInfo(rowSet, tableRowset, tableName);

		/*
		 * Gson gson = new Gson(); return
		 * gson.toJson(this.util.getEntitiesFromResultSet(rowSet));
		 */
		return null;

	}
	
	
	
	
	@CrossOrigin(origins = "http://localhost:4201")
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public String getTableList() {
		// String dbName = "springbootdb";
		String dbName = "muleesb";
		SqlRowSet rowSet = sql.queryForRowSet("select * from information_schema.tables where TABLE_SCHEMA='muleesb'");
		String resultAsJsonString = this.util.getTableDefinition(rowSet);
		return resultAsJsonString;
	}

	@CrossOrigin(origins = "http://localhost:4201")
	@RequestMapping(value = "/create", method = RequestMethod.POST)
	public String createTable(@RequestBody String tableInfo) {

		Map<String, String> mapResult = this.util.creatTable(tableInfo);
		String query = mapResult.get("Query");
		String indexeColumnArrayString = mapResult.get("indexes");
		JsonParser parser = new JsonParser();
		JsonArray indexesArray = (JsonArray) parser.parse(indexeColumnArrayString);
		sql.execute(query);
		for (JsonElement jsonElement : indexesArray) {
			String indexQuery = jsonElement.getAsString();
			sql.execute(indexQuery);
		}

		return "Table created successfully!!!!";

	}

	@CrossOrigin(origins = "http://localhost:4201")
	@RequestMapping(value = "/read", method = RequestMethod.GET)
	public String readTableInfo(@RequestParam(value = "tableName") String tableName) {
		// String dbName = "springbootdb";
		String dbName = "muleesb";
		String columnInfoQuery = "SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME ='" + tableName
				+ "' and TABLE_SCHEMA = '" + dbName + "'";
		SqlRowSet rowSet = sql.queryForRowSet(columnInfoQuery);
		SqlRowSet tableRowset = sql
				.queryForRowSet("SELECT TABLE_COMMENT FROM INFORMATION_SCHEMA.tables WHERE TABLE_SCHEMA='" + dbName
						+ "' AND TABLE_NAME= '" + tableName + "'");
		String result = this.util.DisplayTableInfo(rowSet, tableRowset, tableName);

		/*
		 * Gson gson = new Gson(); return
		 * gson.toJson(this.util.getEntitiesFromResultSet(rowSet));
		 */
		return result;

	}

	@CrossOrigin(origins = "http://localhost:4201")
	@RequestMapping(value = "/update", method = RequestMethod.POST)
	public String updateTable(@RequestParam(value = "tableName") String tableName,
			@RequestParam(value = "operationType") String operationType, @RequestBody String data) {
		// String dbName = "springbootdb";
		String dbName = "muleesb";
		JsonParser parser = new JsonParser();
		
			Map<String, String> mapResult = this.util.UpdateTable(operationType, data, tableName, dbName);
			if (!operationType.equals("FIELDSMODIFY")) {
			String query = mapResult.get("Query");
			sql.execute(query);
			String indexeColumnArrayString = mapResult.get("indexes");
			if (indexeColumnArrayString != null) {
				
				JsonArray indexesArray = (JsonArray) parser.parse(indexeColumnArrayString);
				for (JsonElement jsonElement : indexesArray) {
					String indexQuery = jsonElement.getAsString();
					sql.execute(indexQuery);
				}
			}
			
		}
			else if(operationType.equals("FIELDSMODIFY"))
			{
				String query = mapResult.get("Query");
				JsonArray indexesArray = (JsonArray) parser.parse(query);
				for (JsonElement jsonElement : indexesArray) {
					String indexQuery = jsonElement.getAsString();
					sql.execute(indexQuery);
				}
				
			}

		return "Table Updated Succesfully!!!";
	}

	@CrossOrigin(origins = "http://localhost:4201")
	@RequestMapping(value = "/delete", method = RequestMethod.DELETE)
	public String deleteTable(@RequestParam(value = "tableName") String tableName) {

		String dbName = "muleesb";
		String query = "drop table if exists " + dbName + "." + tableName;
		sql.execute(query);

		return "Table Deleted";
	}

}
