package com.biz.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;


import org.apache.log4j.Logger;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import com.biz.configuration.ConnectionProperty;
import com.biz.exception.DataTypeLengthFormatException;
import com.biz.model.Fields;
import com.biz.model.FieldsModified;
import com.biz.model.Table;
import com.biz.model.TableChangeInfo;
import com.biz.model.TableData;
import com.biz.model.TableDetails;
import com.biz.model.TableFieldsInfo;
import com.biz.util.Constraints;
import com.biz.util.ConstraintsAddDropUtil;
import com.biz.util.DataTypeUtil;
import com.biz.util.UserMapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Component
public class TableUtil {

	private static final Logger logger = Logger.getLogger(TableUtil.class);

	protected Map<String, String> getDatabaseAndUserTableName(String domainName, JdbcTemplate sql,
			ConnectionProperty connProp) {

		String columnInfoQuery = "SELECT TABLESPACE,USERMASTER FROM " + connProp.getMasterDatabaseName() + "."
				+ connProp.getMasterTableName() + " WHERE NAME = '" + domainName + "'";
		SqlRowSet rowSet = sql.queryForRowSet(columnInfoQuery);
		Map<String, String> data = new HashMap<>();
		while (rowSet.next()) {
			data.put("TABLESPACE", rowSet.getString(1));
			data.put("USERMASTER", rowSet.getString(2));
		}
		return data;
	}

	protected String getDatabaseName(String domainName, JdbcTemplate sql, ConnectionProperty connProp) {

		String columnInfoQuery = "SELECT TABLESPACE FROM " + connProp.getMasterDatabaseName() + "."
				+ connProp.getMasterTableName() + " WHERE NAME = '" + domainName + "'";
		SqlRowSet rowSet = sql.queryForRowSet(columnInfoQuery);
		String data = null;
		while (rowSet.next()) {
			data = rowSet.getString(1);
		}
		return data;
	}

	public Map<String, String> checkUserExists(String userName, String password, Map<String, String> domainandUserName,
			JdbcTemplate jdbcTemplate) {

		String dbName = domainandUserName.get("TABLESPACE");
		String tableName = domainandUserName.get("USERMASTER");
		String query = "select * from " + dbName + "." + tableName + " where username='" + userName + "' and password='"
				+ password + "'";

		List<Map<String, String>> users = jdbcTemplate.query(query, new UserMapper());
		return users.size() > 0 ? users.get(0) : null;

	}

	protected Map<String, String> creatTable(String table, String dbName,String domainName,JdbcTemplate sql,ConnectionProperty connProp) {

		Gson gson = new Gson();
		Table t = gson.fromJson(table, Table.class);
		JsonParser jsonParser = new JsonParser();
		JsonObject tableAsJsonObj = (JsonObject) jsonParser.parse(table);
		JsonElement ele = tableAsJsonObj.get("TableDetails");
		TableDetails tableDetails = gson.fromJson(ele, TableDetails.class);

		JsonElement info = tableAsJsonObj.get("TableFieldsInfo");
		TableFieldsInfo tableFieldsInfo = gson.fromJson(info, TableFieldsInfo.class);
		List<Fields> fieldsList = tableFieldsInfo.getFields();

		String tableName = dbName + "." + tableDetails.getTableName().trim();

		String tableDescription = tableDetails.getTableDescription()!=null?tableDetails.getTableDescription().toString():"";

		Map<String, String> mapResult = createColumns(fieldsList);
		String indexesArray = mapResult.get("indexes");
		JsonParser p = new JsonParser();
		List<String> indexList = new ArrayList<>();
		JsonArray jarray = (JsonArray) p.parse(indexesArray);
		for (JsonElement jsonElement : jarray) {

			String column = jsonElement.getAsString();
			String index = "CREATE INDEX " + column + "_index ON " + tableName + "(" + column + ")";
			indexList.add(index);

		}
		String indexListAsString = gson.toJson(indexList);

		String partialQuery = mapResult.get("Query");
		// String indexColumnJson = mapResult.get("indexes");

		String query = "CREATE TABLE IF NOT EXISTS " + tableName + "(" + partialQuery + ") COMMENT '" + tableDescription
				+ "'";
		//sql.execute(query);
		//store table info (createdby,createdOn,modifiedBy etc)
		String queryTable = "insert into "+connProp.getMasterDatabaseName()+"."+dbName+"_tableInfo (tableName,createdBy,createdOn,modifiedBy,modifiedOn,tableComment)  values ('"+tableDetails.getTableName().trim()+"','"+tableDetails.getCreatedBy()+"','"+tableDetails.getCreatedOn()+"','"+tableDetails.getModifiedBy()+"','"+tableDetails.getModifiedOn()+"','"+tableDetails.getTableDescription()+"')";
		//sql.execute(queryTable);
        logger.info("Table Create Query:"+query);
		mapResult.put("Query", query);
		mapResult.put("indexes", indexListAsString);
		mapResult.put("TableUpdate", queryTable);
		
		
		return mapResult;

	}

	protected String DisplayTableInfo(SqlRowSet rowset, SqlRowSet tableRowset, String tableName) {
		String tableDescription = "";
		TableDetails tableDetails = new TableDetails();
		TableFieldsInfo info = new TableFieldsInfo();
		List<Fields> fieldsList = new ArrayList<>();
		Gson gson = new GsonBuilder().serializeNulls().create();
		while (rowset.next()) {
			String fieldName = rowset.getString("COLUMN_NAME");
			String type = rowset.getString("DATA_TYPE").toUpperCase();

			String columnType = rowset.getString("COLUMN_TYPE");
			System.out.println(columnType);
			String length = "";
			/*if (!columnType.toUpperCase().equals("DATE") && !columnType.toUpperCase().equals("DOUBLE")
					&& !columnType.toUpperCase().equals("DATETIME")&&!columnType.toUpperCase().equals("TEXT")) {
				columnType = columnType.split("\\(")[1];
				length = columnType.substring(0, columnType.length() - 1);
			}*/
			if (columnType.toUpperCase().contains("(")) {
				columnType = columnType.split("\\(")[1];
				length = columnType.substring(0, columnType.length() - 1);
			}
			

			// String length = columnType.substring(0, columnType.length() - 1);
			boolean isNull = false;
			String checkNull = rowset.getString("IS_NULLABLE");
			if (!checkNull.equals("NO")) {
				isNull = true;
			}

			String collationName = rowset.getString("COLLATION_NAME");

			String columnKey = rowset.getString("COLUMN_KEY").toUpperCase();
			if (columnKey.equals("PRI")) {
				columnKey = "Primary";
			} else if (columnKey.equals("UNI")) {
				columnKey = "Unique";
			} else if (columnKey.equals("MUL")) {
				columnKey = "Index";
			}
			String columnComment = rowset.getString("COLUMN_COMMENT");
			Fields field = new Fields(fieldName, type, length, collationName, isNull, columnKey, columnComment);
			fieldsList.add(field);

		}
		if (tableRowset.next()) {
			tableDescription = tableRowset.getString("TABLE_COMMENT");

		}
		info.setFields(fieldsList);
		JsonObject jsonObj = new JsonObject();
		tableDetails.setTableDescription(tableDescription);
		tableDetails.setTableName(tableName);
		String tableDetailsAsJson = gson.toJson(tableDetails);
		JsonParser parser = new JsonParser();
		JsonObject tableJSON = (JsonObject) parser.parse(tableDetailsAsJson);

		String fieldsInfo = gson.toJson(info);
		JsonObject fieldsInfoJson = (JsonObject) parser.parse(fieldsInfo);
		jsonObj.add("TableDetails", tableJSON);
		jsonObj.add("TableFieldsInfo", fieldsInfoJson);

		String finalJson = jsonObj.toString();

		return finalJson;
	}

	protected String getTableDefinition(SqlRowSet rowSet) {
		List<TableData> tableList = new LinkedList<>();
		Gson mapper = new GsonBuilder().serializeNulls().create();
		while (rowSet.next()) {

		/*	String createdBy = "";
			String changedby = "";
			String tableName = rowSet.getString("TABLE_NAME");
			String tableDescription = rowSet.getString("TABLE_COMMENT");
			String tableChangedOn = rowSet.getString("UPDATE_TIME");
			String createdOn="";
			
		    createdOn = rowSet.getString("CREATE_TIME");*/
			
			String tableName = rowSet.getString("tableName");
			String tableDescription = rowSet.getString("tableComment");
			String createdBy = rowSet.getString("createdBy");
			String modifiedBy = rowSet.getString("modifiedBy");
			String  modifiedOn = rowSet.getString("modifiedOn");
			String   createdOn = rowSet.getString("createdOn");
			TableData data = new TableData(tableName, tableDescription, createdOn, createdBy, modifiedOn,
					modifiedBy);
			tableList.add(data);
			
		}
		String resultAsJson = mapper.toJson(tableList);
		// List<Map<String, Object>> objects =
		// this.getEntitiesFromResultSet(rowSet);

		return resultAsJson;
	}
  
	protected List<Map<String, Object>> getEntitiesFromResultSet(SqlRowSet rowSet) {
		ArrayList<Map<String, Object>> entities = new ArrayList<>();
		while (rowSet.next()) {
			entities.add(getEntityFromResultSet(rowSet));
		}
		return entities;
	}

	protected Map<String, Object> getEntityFromResultSet(SqlRowSet rowSet) {
		SqlRowSetMetaData metaData = rowSet.getMetaData();
		int columnCount = metaData.getColumnCount();
		Map<String, Object> resultsMap = new HashMap<>();
		for (int i = 1; i <= columnCount; ++i) {
			String columnName = metaData.getColumnName(i).toLowerCase();
			Object object = rowSet.getObject(i);
			resultsMap.put(columnName, object);
		}
		return resultsMap;
	}

	protected Map<String, String> UpdateTable(String operationType, String data, String tableName, String dbName,JdbcTemplate sql,ConnectionProperty connProp) {
         String tblName = tableName;
		JsonParser jsonParser = new JsonParser();
		JsonObject jObj = (JsonObject) jsonParser.parse(data);
		tableName = dbName + "." + tableName.trim();
		JsonObject tableChangeJson = (JsonObject) jObj.get("tableChangeData");
		JsonElement jele = jObj.get("modifiedBy");
		String modifiedBy = jele.getAsString();
		JsonElement modifiedOnJele = jObj.get("modifiedOn");
		String modifiedOn = modifiedOnJele.getAsString();
		//String modifiedOn = jObj.get("modifiedOn")!=null?jObj.get("modifiedOn"):"";
		//String modifiedBy = jObj.get("modifiedBy")!=null?jObj.get("modifiedBy").toString():"";
		
		Gson gson1 = new GsonBuilder().create();
		TableChangeInfo tableChangeInfo = gson1.fromJson(tableChangeJson, TableChangeInfo.class);
		String tableDescription = tableChangeInfo.getTableNewDescription();
		String newTableName = tableChangeInfo.getTableNewName();
		String tableOldName = tableChangeInfo.getTableOldName();
		//check whether table name or table table description changed or not
		
	     if(tableDescription!=null)
		 {
	    	logger.info("valu:"+tableDescription);
			String query = "ALTER TABLE "+tableName+" COMMENT '"+tableDescription+"'";
			sql.execute(query);
		 }
	     else if(newTableName!=null)
	     {
	    	 tableOldName = dbName+"."+tableOldName;
	    	 String tableNewName =  dbName+"."+newTableName;
	    	 logger.info("Table Name going to  change  with  the :"+newTableName);
			 String query = "RENAME TABLE  "+tableOldName+" TO  "+tableNewName+"";
			 sql.execute(query);
	     }
		String query = "";
		Map<String, String> mapResult = null;
		switch (operationType.toUpperCase()) {

		case "FIELDCREATION": {

			
			JsonArray ele = (JsonArray) jObj.get("fields");

			Fields[] arrName = new Gson().fromJson(ele, Fields[].class);
			List<Fields> listFields = new ArrayList<>();
			listFields = Arrays.asList(arrName);

			// List<Fields> fieldList = new GsonBuilder().create().fromJson(ele,
			// List.class);
			mapResult = createColumns(listFields);
			String partialQuery = mapResult.get("Query");
			String indexColumnJson = mapResult.get("indexes");
			Gson gson = new Gson();

			JsonParser p = new JsonParser();
			List<String> indexList = new ArrayList<>();
			JsonArray jarray = (JsonArray) p.parse(indexColumnJson);
			for (JsonElement jsonElement : jarray) {

				String column = jsonElement.getAsString();
				String index = "CREATE INDEX " + column + "_index ON " + tableName + "(" + column + ")";
				indexList.add(index);

			}
			String indexListAsString = gson.toJson(indexList);

			query = "ALTER TABLE " + tableName + " ADD COLUMN (" + partialQuery + ")";

			mapResult.put("Query", query);
			mapResult.put("indexes", indexListAsString);

			break;

		}
		case "FIELDSDROP": {

			
			JsonArray ele = (JsonArray) jObj.get("fields");
			String[] arrName = new Gson().fromJson(ele, String[].class);
			StringBuffer buffer = new StringBuffer();

			Arrays.stream(arrName).forEach(column -> {

				String qry = "DROP COLUMN " + column;
				buffer.append(qry + ",");
			});

			if (buffer.length() != 0) {
				buffer.setLength(buffer.length() - 1);
				String partialQuery = buffer.toString();
				query = "ALTER TABLE " + tableName + " " + partialQuery;
			}
			mapResult = new HashMap<>();
			mapResult.put("Query", query);
			String indexListAsString = "[]";
			mapResult.put("indexes", indexListAsString);

			break;
		}
		case "FIELDSMODIFY": {

			//JsonObject jObj = (JsonObject) jsonParser.parse(data);
			JsonArray jarray = (JsonArray) jObj.get("FieldsChanged");
			FieldsModified[] fieldsModifiedList = new GsonBuilder().create().fromJson(jarray, FieldsModified[].class);
			List<String> listOfQueries = fieldsModifiedCheck(fieldsModifiedList, tableName, dbName,sql);
			String queries = new GsonBuilder().create().toJson(listOfQueries);

			mapResult = new HashMap<>();
			mapResult.put("Query", queries);
			mapResult.put("Indexes", "[]");

			break;
		}
		default:
			break;
		}
		String updateTableInfoquery = "update "+connProp.getMasterDatabaseName()+"."+dbName+"_tableInfo set modifiedOn='"+modifiedOn+"', modifiedBy='"+modifiedBy+"' where tableName = '"+tblName+"'";
		sql.execute(updateTableInfoquery);
		return mapResult;
	}

	private List<String> fieldsModifiedCheck(FieldsModified[] fieldsModifiedList, String tableName, String dbName,JdbcTemplate sql) {
		// String tableName = dbName + "." + tablename;
		List<String> queryList = new ArrayList<>();

		Arrays.stream(fieldsModifiedList).forEach(fieldmodfiedObj -> {
			String query = "";
			String fieldType = fieldmodfiedObj.getFieldType();
			Fields fieldsList = fieldmodfiedObj.getNewFieldRecord();
			String fieldLength = fieldsList.getLength()!=null? fieldsList.getLength():"";
			switch (fieldType.toUpperCase()) {
			case "FIELDNAME": {
				
				String newFieldName = fieldmodfiedObj.getNewField()!=null?fieldmodfiedObj.getNewField():"";
				String oldFieldName = fieldmodfiedObj.getOldField()!=null?fieldmodfiedObj.getOldField():"";
				if(newFieldName.length()!=0&&newFieldName!=null&&!newFieldName.equals(""))
				{
				if(fieldLength.equals("0")||fieldLength.equals(""))
				{
					 if(Arrays.asList(DataTypeUtil.OPTIONAL_LENGTH).contains(fieldsList.getType().toUpperCase()))
					 {
						 query = "ALTER TABLE " + tableName + " change " + oldFieldName + " " + fieldsList.getFieldName()
			              + " " + fieldsList.getType();
					 }
					 else
						 throw new DataTypeLengthFormatException(fieldsList.getType().toUpperCase(),"Length sholud not zero for the type "+ fieldsList.getType().toUpperCase());
				}
				else{
					 query = "ALTER TABLE " + tableName + " change " + oldFieldName + " " + fieldsList.getFieldName()
						+ " " + fieldsList.getType() + "(" + fieldsList.getLength() + ")";
				}
				}
				else
					logger.error("new field sholud not zero for the type " + fieldsList.getType().toUpperCase());//through exception
				
				queryList.add(query);
				break;
			}

			case "TYPE": {
				
				String newFieldName = fieldmodfiedObj.getNewField();
				String oldFieldName = fieldmodfiedObj.getOldField();
				
				if(fieldLength.equals("0")||fieldLength.equals(""))
				{
					 if(Arrays.asList(DataTypeUtil.OPTIONAL_LENGTH).contains(fieldsList.getType().toUpperCase()))
					 {
							query = "ALTER TABLE " + tableName + " modify " + fieldsList.getFieldName() + " "
									+ fieldsList.getType();
					 }
					 else
					 {
						 logger.error("Length sholud not zero for the type " + fieldsList.getType().toUpperCase());
						 throw new DataTypeLengthFormatException(fieldsList.getType().toUpperCase(),"Length sholud not zero for the type "+ fieldsList.getType().toUpperCase());
					 }
				}
				else{
					
					query = "ALTER TABLE " + tableName + " modify " + fieldsList.getFieldName() + "  "
							+ fieldsList.getType() + "(" + fieldsList.getLength() + ")";
				}
				
				queryList.add(query);
				break;

			}

			case "LENGTH":
				
			{
				
				if(fieldLength.equals("0")||fieldLength.equals(""))
				{
					 if(Arrays.asList(DataTypeUtil.OPTIONAL_LENGTH).contains(fieldsList.getType().toUpperCase()))
					 {
						 query = "ALTER TABLE " + tableName + " modify " + fieldsList.getFieldName() + "  "
									+ fieldsList.getType();
					 }
					 else
					 {
						 logger.error("Length sholud not zero for the type " + fieldsList.getType().toUpperCase());
						 throw new DataTypeLengthFormatException(fieldsList.getType().toUpperCase(),"Length sholud not zero for the type "+ fieldsList.getType().toUpperCase());

					 }
				}
				else{
					if(!Arrays.asList(DataTypeUtil.LENGTH_NOT_REQUIRED_TYPES).contains(fieldsList.getType().toUpperCase()))
					query = "ALTER TABLE " + tableName + " modify " + fieldsList.getFieldName() + "  "
							+ fieldsList.getType() + "(" + fieldsList.getLength() + ")";
					else{
						throw new DataTypeLengthFormatException(fieldType, "The Field Type "+fieldType+" Don't require the Length!!");
					}
				}
			
                queryList.add(query);
				break;
			}
			case "COLLATION":
                  
				break;
			case "NULL":
				if (!fieldsList.getLength().equals("0")&&fieldsList.getLength().length()!=0) {
					if (fieldsList.getNull()) {
						query = "ALTER TABLE " + tableName + " MODIFY " + fieldsList.getFieldName() + " "
								+ fieldsList.getType() + "(" + fieldsList.getLength() + ")" + " NOT NULL";
						queryList.add(query);
					} else {
						query = "ALTER TABLE " + tableName + " MODIFY " + fieldsList.getFieldName() + " "
								+ fieldsList.getType() + "(" + fieldsList.getLength() + ")" + " NULL";
						queryList.add(query);
					}

				} else {
					if (fieldsList.getNull()) {
						query = "ALTER TABLE " + tableName + " MODIFY " + fieldsList.getFieldName() + " "
								+ fieldsList.getType() + " NOT NULL";
						queryList.add(query);
					} else {
						query = "ALTER TABLE " + tableName + " MODIFY " + fieldsList.getFieldName() + " "
								+ fieldsList.getType() + " NULL";
						queryList.add(query);
					}
				}
				
				break;
			case "INDEX":
			{
				ConstraintsAddDropUtil addOrDrop = new ConstraintsAddDropUtil();
				String newIndexField = fieldmodfiedObj.getNewField();
				String oldindexField = fieldmodfiedObj.getOldField();
				if (oldindexField==null||oldindexField.equals("")) {
					query = addOrDrop.ConstructQueryToADD(tableName, newIndexField, oldindexField, fieldsList);
					 if(query.length()!=0||!query.equals(""))
					 {
						 queryList.add(query);
					 }
				}
				else if(newIndexField.equals("")||newIndexField.equals(null)){//drop indexes
					 query = addOrDrop.ConstructQueryToDrop(tableName, newIndexField, oldindexField, fieldsList, sql);
					 if(query.length()!=0||!query.equals(""))
					 {
						 queryList.add(query);
					 }
				}
				else if(Arrays.asList(Constraints.CONSTRAINTS).contains(oldindexField.toUpperCase()))
				{
					if(newIndexField!=null&&!newIndexField.equals(""))
					{
						 query = addOrDrop.ConstructQueryToDrop(tableName, newIndexField, oldindexField, fieldsList, sql);
						 if(query.length()!=0||!query.equals(""))
						 {
							 queryList.add(query);
						 }
						 String addQuery = addOrDrop.ConstructQueryToADD(tableName, newIndexField, oldindexField, fieldsList);
						 if(query.length()!=0||!query.equals(""))
						 {
							 queryList.add(addQuery);
						 }
					}
				}
              
				break;
			}
			case "FIELDDESCRIPTION":
				if (!fieldsList.getLength().equals("0"))
					query = "ALTER TABLE " + tableName + " MODIFY" + " " + fieldsList.getFieldName() + " "
							+ fieldsList.getType() + "(" + fieldsList.getLength() + ") COMMENT '"
							+ fieldsList.getFieldDescription() + "'";
				else {
					query = "ALTER TABLE " + tableName + " MODIFY" + " " + fieldsList.getFieldName() + " "
							+ fieldsList.getType() + " COMMENT '" + fieldsList.getFieldDescription() + "'";

				}
				queryList.add(query);
				break;
			default:
				break;
			}

		});
		return queryList;
	}

	private Map<String, String> createColumns(List<Fields> fields) {
		StringBuffer buffer = new StringBuffer();
		List<String> indexes = new ArrayList<>();
		AtomicReference<String> primarykey = new AtomicReference<>();
		Map<String, String> map = new HashMap<>();

		fields.forEach(field -> {

			String columnName = field.getFieldName();
			String columnDescription = field.getFieldDescription();
			String columnType = field.getType();
			String columnLength = field.getLength()!=null? field.getLength():"";
			String collationWithType = field.getCollation();
			String charecterSet = "";
			String collation = "";
			if(collationWithType.length()!=0&&collationWithType!=null)
			{
			  String []collationArray = collationWithType.split("-");
			  charecterSet = collationArray[0].trim();
			  collation = collationArray[1].trim();
			}
					
			boolean isNull = field.getNull();
			String nullField = isNull ? "NULL" : "NOT NULL";
			String index = field.getIndex().toUpperCase().trim();

			if (!index.equals("") && index!=null && !index.equals("INDEX")) {
				if (collation.equals("") || collation.equals(null)) {
					if (index.equals("UNIQUE")) {
						
						if(columnLength.equals("") || columnLength.equals("0"))
						{
							 if(Arrays.asList(DataTypeUtil.OPTIONAL_LENGTH).contains(columnType.toUpperCase()))
							 {
								  String partilaQuery = columnName + " " + columnType + "  " + nullField + " " + index
											+ " COMMENT " + "'" + columnDescription + "'";
									    buffer.append(partilaQuery + " ,");
							 }
							 else
							 {
								 logger.error("Length sholud not zero for the type " + columnType.toUpperCase());
								 throw new DataTypeLengthFormatException(columnType,"Length sholud not zero for the type "+ columnType.toUpperCase());
							 }
						}
						else{
							   String partilaQuery = columnName + " " + columnType + "(" + columnLength + ")  "
									+ nullField + " " + index + " COMMENT " + "'" + columnDescription + "'";
							buffer.append(partilaQuery + " ,");
						}
						
						
					} // end of unique field check
					else if (index.equals("PRIMARY")) {
						
						if(columnLength.equals("") || columnLength.equals("0"))
						{
							 if(Arrays.asList(DataTypeUtil.OPTIONAL_LENGTH).contains(columnType.toUpperCase()))
							 {
								 String partilaQuery = columnName + " " + columnType + "  " + nullField + " COMMENT " + "'"
											+ columnDescription + "' ";
									buffer.append(partilaQuery + " ,");
									primarykey.set(columnName);
						     }
							 else
							 {
								 logger.error("Length sholud not zero for the type " + columnType.toUpperCase());
								 throw new DataTypeLengthFormatException(columnType,"Length sholud not zero for the type "+ columnType.toUpperCase());
							 }
						}
						else{
							
							String partilaQuery = columnName + " " + columnType + "(" + columnLength + ")  "
									+ nullField + " COMMENT " + "'" + columnDescription + "' ";
							buffer.append(partilaQuery + " ,");
							primarykey.set(columnName);
						}
						
						

					}
				} // end of collation null check  condition
				else {
					if (index.equals("UNIQUE")) {
						
						if(columnLength.equals("") || columnLength.equals("0"))
						{
							 if(Arrays.asList(DataTypeUtil.OPTIONAL_LENGTH).contains(columnType.toUpperCase()))
							 {
								 String partilaQuery = columnName + " " + columnType + " CHARACTER SET "+charecterSet+" COLLATE " + collation + " "
											+ nullField + " " + index + " COMMENT " + "'" + columnDescription + "'";
									   buffer.append(partilaQuery + ",");
							 }
							 else
							 {
								 logger.error("Length sholud not zero for the type " + columnType.toUpperCase());
								 throw new DataTypeLengthFormatException(columnType,"Length sholud not zero for the type "+ columnType.toUpperCase());
							 }

						}
						else{
							String partilaQuery = columnName + " " + columnType + "(" + columnLength + ") CHARACTER SET "+charecterSet+" COLLATE "
									+ collation + " " + nullField + " " + index + " COMMENT " + "'"
									+ columnDescription + "'";
							buffer.append(partilaQuery + ",");
						}
					

					

					} else if (index.equals("PRIMARY")) {
						
						
						if(columnLength.equals("") || columnLength.equals("0"))
						{
							 if(Arrays.asList(DataTypeUtil.OPTIONAL_LENGTH).contains(columnType.toUpperCase()))
							 {
								 String partilaQuery = columnName + " " + columnType + " CHARACTER SET "+charecterSet+"  COLLATE " + collation + " "
											+ nullField + " COMMENT " + "'" + columnDescription + "'";
									buffer.append(partilaQuery + ",");
									primarykey.set(columnName);
							 }
							 else
							 {
								 logger.error("Length sholud not zero for the type " + columnType.toUpperCase());
								 throw new DataTypeLengthFormatException(columnType,"Length sholud not zero for the type "+ columnType.toUpperCase());
							 }

						}
						else{
							String partilaQuery = columnName + " " + columnType + "(" + columnLength + ") CHARACTER SET "+charecterSet+" COLLATE "
									+ collation + " " + nullField + " COMMENT " + "'" + columnDescription + "'";
							buffer.append(partilaQuery + ",");
							primarykey.set(columnName);
						}

					}
				}

			} // end of index field check INDEX not
			else {
				if (collation.equals("") || collation==null) {
					
					
					if(columnLength.equals("") || columnLength.equals("0"))
					{
						 if(Arrays.asList(DataTypeUtil.OPTIONAL_LENGTH).contains(columnType.toUpperCase()))
						 {
							 String partilaQuery = columnName + " " + columnType + " " + nullField + " COMMENT " + "'"
										+ columnDescription + "'";
								    buffer.append(partilaQuery + ",");
						 }
						 else
						 {
							 logger.error("Length sholud not zero for the type " + columnType.toUpperCase());
							 throw new DataTypeLengthFormatException(columnType,"Length sholud not zero for the type "+ columnType.toUpperCase());
						 }

					}
					else{
						String partilaQuery = columnName + " " + columnType + "(" + columnLength + ")  " + nullField
								+ " COMMENT " + "'" + columnDescription + "'";
						buffer.append(partilaQuery + ",");
					}
					

				} else {
					
					if(columnLength.equals("") || columnLength.equals("0"))
					{
						 if(Arrays.asList(DataTypeUtil.OPTIONAL_LENGTH).contains(columnType.toUpperCase()))
						 {
							 String partilaQuery = columnName + " " + columnType + " CHARACTER SET "+charecterSet+"  COLLATE '" + collation + "'  "
										+ nullField + " COMMENT " + "'" + columnDescription + "'";
								buffer.append(partilaQuery + ",");
						 }
						 else
						 {
							 logger.error("Length sholud not zero for the type " + columnType.toUpperCase());
						     throw new DataTypeLengthFormatException(columnType,"Length sholud not zero for the type "+ columnType.toUpperCase());
						 }

					}
					else{
						String partilaQuery = columnName + " " + columnType + "(" + columnLength + ") CHARACTER SET "+charecterSet+"  COLLATE '"
								+ collation + "'  " + nullField + " COMMENT " + "'" + columnDescription + "'";
						buffer.append(partilaQuery + ",");
					}

				}
				if (index.equals("INDEX")) {
					indexes.add(columnName);
				}
			}

		});
		Gson gson = new Gson();
		if (primarykey.get() != null) {
			buffer.append("PRIMARY KEY (" + primarykey.get().toString() + ")" + ",");
		}
		String indexJson = gson.toJson(indexes);
		buffer.setLength(buffer.length() - 1);
		map.put("Query", buffer.toString());
		map.put("indexes", indexJson);
		return map;

	}

}
