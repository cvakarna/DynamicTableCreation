package com.biz.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Component;

import com.biz.model.Fields;
import com.biz.model.FieldsModified;
import com.biz.model.Table;
import com.biz.model.TableDetails;
import com.biz.model.TableFieldsInfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Component
public class TableUtil {
	
	
	private static final Logger logger = Logger.getLogger(TableUtil.class);

	protected Map<String, String> creatTable(String table) {

		// String dbName = "springbootdb";
		String dbName = "muleesb";
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
		String tableDescription = tableDetails.getTableDescription();

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

		mapResult.put("Query", query);
		mapResult.put("indexes", indexListAsString);
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
			String length = "";
			if (!columnType.toUpperCase().equals("DATE")) {
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

			String createdBy = "";
			String changedby = "";
			String tableName = rowSet.getString("TABLE_NAME");
			String tableDescription = rowSet.getString("TABLE_COMMENT");
			String tableChangedOn = rowSet.getString("UPDATE_TIME");
			String createdOn = rowSet.getString("CREATE_TIME");
			TableData data = new TableData(tableName, tableDescription, createdOn, createdBy, tableChangedOn,
					changedby);
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

	protected Map<String, String> UpdateTable(String operationType, String data, String tableName, String dbName) {

		JsonParser jsonParser = new JsonParser();
		tableName = dbName + "." + tableName.trim();
		String query = "";
		Map<String, String> mapResult = null;
		switch (operationType.toUpperCase()) {

		case "FIELDCREATION": {

			JsonObject jObj = (JsonObject) jsonParser.parse(data);
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

			JsonObject jObj = (JsonObject) jsonParser.parse(data);
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

			JsonObject jObj = (JsonObject) jsonParser.parse(data);
			JsonArray jarray = (JsonArray) jObj.get("FieldsChanged");
			FieldsModified[] fieldsModifiedList = new GsonBuilder().create().fromJson(jarray, FieldsModified[].class);
			List<String> listOfQueries = fieldsModifiedCheck(fieldsModifiedList,tableName,dbName);
			String queries = new GsonBuilder().create().toJson(listOfQueries);
			
			mapResult = new HashMap<>();
			mapResult.put("Query",queries);
			mapResult.put("Indexes","[]");
			
			break;
		}
		default:
			break;
		}
		return mapResult;
	}

	private List<String> fieldsModifiedCheck(FieldsModified[] fieldsModifiedList, String tableName, String dbName) {
		//String tableName = dbName + "." + tablename;
		List<String> queryList = new ArrayList<>();

		Arrays.stream(fieldsModifiedList).forEach(fieldmodfiedObj -> {
			String query = "";
			String fieldType = fieldmodfiedObj.getFieldType();
			Fields fieldsList = fieldmodfiedObj.getNewFieldRecord();
			switch (fieldType.toUpperCase()) {
			case "FIELDNAME": {
				String newFieldName = fieldmodfiedObj.getNewField();
				String oldFieldName = fieldmodfiedObj.getOldField();
				if (!fieldsList.getLength().equals("0")) {
					query = "ALTER TABLE " + tableName + " change " + oldFieldName + " " + fieldsList.getFieldName()
							+ " " + fieldsList.getType() + "(" + fieldsList.getLength() + ")";
				} else {
					query = "ALTER TABLE " + tableName + " change " + oldFieldName + " " + fieldsList.getFieldName()
							+ " " + fieldsList.getType();
				}
				queryList.add(query);
				break;
			}

			case "TYPE": {
				String newFieldName = fieldmodfiedObj.getNewField();
				String oldFieldName = fieldmodfiedObj.getOldField();
				if (!fieldsList.getLength().equals("0")) {
					query = "ALTER TABLE " + tableName + " modify " + fieldsList.getFieldName() + "  "
							+ fieldsList.getType() + "(" + fieldsList.getLength() + ")";
				} else {
					query = "ALTER TABLE " + tableName + " modify " + fieldsList.getFieldName() + " "
							+ fieldsList.getType();
				}
				queryList.add(query);
				break;

			}

			case "LENGTH":
				if (!fieldsList.getLength().equals("0")) {
					query = "ALTER TABLE " + tableName + " modify " + fieldsList.getFieldName() + "  "
							+ fieldsList.getType() + "(" + fieldsList.getLength() + ")";
				} else {
					query = "ALTER TABLE " + tableName + " modify " + fieldsList.getFieldName() + " "
							+ fieldsList.getType();
				}
				queryList.add(query);
				break;
			case "COLLATION":

				break;
			case "NULL":
				if (!fieldsList.getLength().equals("0")) {
					if (fieldsList.getNull()) {
						query = "ALTER TABLE " + tableName + " MODIFY " + fieldsList.getFieldName() + " "
								+ fieldsList.getType() + "(" + fieldsList.getLength() + ")" + " NOT NULL";
					} else {
						query = "ALTER TABLE " + tableName + " MODIFY " + fieldsList.getFieldName() + " "
								+ fieldsList.getType() + "(" + fieldsList.getLength() + ")" + " NULL";
					}

				} else {
					if (fieldsList.getNull()) {
						query = "ALTER TABLE " + tableName + " MODIFY " + fieldsList.getFieldName() + " "
								+ fieldsList.getType() + " NOT NULL";
					} else {
						query = "ALTER TABLE " + tableName + " MODIFY " + fieldsList.getFieldName() + " "
								+ fieldsList.getType() + " NULL";
					}
				}
				queryList.add(query);
				break;
			case "INDEX":
				
				 String newIndexField = fieldmodfiedObj.getNewField();
				 String oldindexField = fieldmodfiedObj.getOldField();
				 if(oldindexField.equals("")||oldindexField.equals(null))
				 {
					 if(newIndexField.toUpperCase().equals("UNIQUE"))
					 {
						 //create unique field
						 query = "ALTER TABLE "+tableName+ " ADD UNIQUE("+fieldsList.getFieldName()+")";
						 queryList.add(query);
					 }
					 else if(newIndexField.toUpperCase().equals("INDEX"))
					 {
						 //create index
						 query = "CREATE INDEX "+fieldsList.getFieldName()+"_index  ON "+tableName+" ("+fieldsList.getFieldName()+")";
						 
					 }
					 else if(newIndexField.toUpperCase().equals("PRIMARY"))
					 {
						 //create primary
						 
					 }
					 else{
						 //create foriagnkey
					 }
				 }
				 
				 
				break;
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
			String columnLength = field.getLength();
			String collation = field.getCollation();
			boolean isNull = field.getNull();
			String nullField = isNull ? "NULL" : "NOT NULL";
			String index = field.getIndex().toUpperCase().trim();

			if (!index.equals("") && !index.equals(null) && !index.equals("INDEX")) {
				if (collation.equals("") || collation.equals(null)) {
					if (index.equals("UNIQUE")) {
						if (columnType.toUpperCase().equals("DATE")) {
							String partilaQuery = columnName + " " + columnType + "  " + nullField + " " + index
									+ " COMMENT " + "'" + columnDescription + "'";
							buffer.append(partilaQuery + " ,");
						} else {
							String partilaQuery = columnName + " " + columnType + "(" + columnLength + ")  " + nullField
									+ " " + index + " COMMENT " + "'" + columnDescription + "'";
							buffer.append(partilaQuery + " ,");
						}

					} else if (index.equals("PRIMARY")) {
						if (columnType.toUpperCase().equals("DATE")) {
							String partilaQuery = columnName + " " + columnType + "  " + nullField + " COMMENT " + "'"
									+ columnDescription + "' ";
							buffer.append(partilaQuery + " ,");
							primarykey.set(columnName);
						} else {
							String partilaQuery = columnName + " " + columnType + "(" + columnLength + ")  " + nullField
									+ " COMMENT " + "'" + columnDescription + "' ";
							buffer.append(partilaQuery + " ,");
							primarykey.set(columnName);
						}
					}
				} else {
					if (index.equals("UNIQUE")) {
						if (columnType.toUpperCase().equals("DATE")) {
							String partilaQuery = columnName + " " + columnType + "  COLLATE " + collation + " "
									+ nullField + " " + index + " COMMENT " + "'" + columnDescription + "'";
							buffer.append(partilaQuery + ",");
						} else {
							String partilaQuery = columnName + " " + columnType + "(" + columnLength + ") COLLATE "
									+ collation + " " + nullField + " " + index + " COMMENT " + "'" + columnDescription
									+ "'";
							buffer.append(partilaQuery + ",");
						}
					} else if (index.equals("PRIMARY")) {
						if (columnType.toUpperCase().equals("DATE")) {
							String partilaQuery = columnName + " " + columnType + "  COLLATE " + collation + " "
									+ nullField + " COMMENT " + "'" + columnDescription + "'";
							buffer.append(partilaQuery + ",");
						} else {
							String partilaQuery = columnName + " " + columnType + "(" + columnLength + ") COLLATE "
									+ collation + " " + nullField + " COMMENT " + "'" + columnDescription + "'";
							buffer.append(partilaQuery + ",");
						}
					}
				}

			} else {
				if (collation.equals("") || collation.equals(null)) {
					if (columnType.toUpperCase().equals("DATE")) {
						String partilaQuery = columnName + " " + columnType + " " + nullField + " COMMENT " + "'"
								+ columnDescription + "'";
						buffer.append(partilaQuery + ",");
					} else {
						String partilaQuery = columnName + " " + columnType + "(" + columnLength + ")  " + nullField
								+ " COMMENT " + "'" + columnDescription + "'";
						buffer.append(partilaQuery + ",");
					}

				} else {
					if (columnType.toUpperCase().equals("DATE")) {
						String partilaQuery = columnName + " " + columnType + "  COLLATE '" + collation + "'  "
								+ nullField + " COMMENT " + "'" + columnDescription + "'";
						buffer.append(partilaQuery + ",");
					} else {
						String partilaQuery = columnName + " " + columnType + "(" + columnLength + ")  COLLATE '"
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
