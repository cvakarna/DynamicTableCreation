package com.biz.demo;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.web.bind.annotation.CrossOrigin;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.biz.configuration.ConnectionProperty;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

@CrossOrigin(origins = "http://localhost:4203")
@RestController
public class HomeController {

	// DB Connection
	@Autowired
	JdbcTemplate sql;

	@Autowired
	TableUtil util;
	 
	@Autowired
	 ConnectionProperty connProp;
	 
	//@CrossOrigin(origins = "http://106.51.126.111:4201")
	@CrossOrigin(origins = "http://localhost:4203")
	 @RequestMapping(value = "/login", method = RequestMethod.GET)
	 public Map<String, String> login(@RequestParam(value = "username") String username,
	   @RequestParam(value = "password") String password,@RequestParam(value = "domainName") String domainName) {
	  
	  Map<String, String> domainUserTableName = util.getDatabaseAndUserTableName(domainName,sql, connProp);
	  
	  if(domainUserTableName.isEmpty())
	   return null;
	  else {
	   Map<String, String> userData = util.checkUserExists(username, password, domainUserTableName, sql);
	   
	   return userData ;
	  }
	 }

	

	
	@CrossOrigin(origins = "http://localhost:4203")
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public String getTableList(@RequestHeader(value="domainName") String domainName) {

		String dbName = util.getDatabaseName(domainName, sql, connProp);
		SqlRowSet rowSet = sql.queryForRowSet("select * from information_schema.tables where TABLE_SCHEMA='"+dbName+"'");
		String resultAsJsonString = this.util.getTableDefinition(rowSet);
		return resultAsJsonString;
	}

	@CrossOrigin(origins = "http://localhost:4203")
	@RequestMapping(value = "/create", method = RequestMethod.POST)
	public String createTable(@RequestHeader(value="domainName") String domainName, @RequestBody String tableInfo) {
		String dbName = util.getDatabaseName(domainName, sql, connProp);

		Map<String, String> mapResult = this.util.creatTable(tableInfo,dbName);
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

	@CrossOrigin(origins = "http://localhost:4203")
	@RequestMapping(value = "/read", method = RequestMethod.GET)
	public String readTableInfo(@RequestHeader(value="domainName") String domainName, @RequestParam(value = "tableName") String tableName) {
		String dbName = util.getDatabaseName(domainName, sql, connProp);
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

	@CrossOrigin(origins = "http://localhost:4203")
	@RequestMapping(value = "/update", method = RequestMethod.POST)
	public String updateTable(@RequestHeader(value="domainName") String domainName, @RequestParam(value = "tableName") String tableName,
			@RequestParam(value = "operationType") String operationType, @RequestBody String data) {
		String dbName = util.getDatabaseName(domainName, sql, connProp);
		JsonParser parser = new JsonParser();
		String result = "Table Updated Succesfully!!!";
			Map<String, String> mapResult = this.util.UpdateTable(operationType, data, tableName, dbName,sql);
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

		return result;
	}

	@CrossOrigin(origins = "http://localhost:4203")
	@RequestMapping(value = "/delete", method = RequestMethod.DELETE)
	public String deleteTable(@RequestHeader(value="domainName") String domainName, @RequestParam(value = "tableName") String tableName) {
		String dbName = util.getDatabaseName(domainName, sql, connProp);
		String query = "drop table if exists " + dbName + "." + tableName;
		sql.execute(query);

		return "Table Deleted";
	}

}
