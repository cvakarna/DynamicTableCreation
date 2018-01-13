package com.biz.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.biz.model.Fields;

public class ConstraintsAddDropUtil {

	
	public String ConstructQueryToADD(String tableName,String newIndexField,String oldindexField,Fields fieldsList)
	{
		String query = "";
		
			if (newIndexField.toUpperCase().equals("UNIQUE")) {
				// create unique field
				query = "ALTER TABLE " + tableName + " ADD UNIQUE(" + fieldsList.getFieldName() + ")";
				
			} else if (newIndexField.toUpperCase().equals("INDEX")) {
				// create index
				query = "CREATE INDEX " + fieldsList.getFieldName() + "_index  ON " + tableName + " ("
						+ fieldsList.getFieldName() + ")";

			} else if (newIndexField.toUpperCase().equals("PRIMARY")) {
				// create primary
                 query = "ALTER TABLE "+tableName+" ADD PRIMARY KEY("+fieldsList.getFieldName()+")";
			} else {
				// create foriagnkey
			}
		
		return query;
	}
	
	public String ConstructQueryToDrop(String tableName,String newIndexField,String oldindexField,Fields fieldsList,JdbcTemplate sql)
	{
		String query = "";
		if(oldindexField.toUpperCase().equals("UNIQUE"))
		{
			//drop index
			query = "DROP INDEX "+fieldsList.getFieldName()+" ON "+tableName;
		}
		else if(oldindexField.toUpperCase().equals("PRIMARY"))
		{
			String checkPrimaryKey= "show keys from "+tableName+" where Key_name = 'PRIMARY'";
			SqlRowSet rowset = sql.queryForRowSet(checkPrimaryKey);
			String primaryKeyColumnName = rowset.getString("Column_name");
			if(primaryKeyColumnName.equals(fieldsList.getFieldName()))
			{
				//drop the primary key
				query = "ALTER TABLE "+tableName+" DROP PRIMARY KEY";
			}
			
		}
		else if(oldindexField.toUpperCase().equals("INDEX"))
		{
			query  = "ALTER TABLE "+tableName+" DROP INDEX "+fieldsList.getFieldName()+"_index;";
		}
		return query;
	}
}
