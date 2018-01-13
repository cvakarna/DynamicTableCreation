package com.biz.util;

public class DataTypeUtil {
	
	//public static final String[] STRING_DATATYPE = {"TEXT", "BLOB", "TINYBLOB","TINYTEXT","MEDIUMBLOB","MEDIUMTEXT","LONGBLOB","LONGTEXT","ENUM"};
	//public static final String[] NUMBER_DATATYPE = {"INT","TINYINT","SMALLINT","MEDIUMINT","BIGINT","FLOAT","DOUBLE","DECIMAL"};
    //public static final String[] DATE_TIME = {"DATE","DATETIME","TIMESTAMP","TIME","YEAR"};

	public static final String[] STRING_DATATYPE = {"TEXT", "BLOB", "TINYBLOB","TINYTEXT","MEDIUMBLOB","MEDIUMTEXT","LONGBLOB","LONGTEXT","ENUM"};
    public static final String[] NUMBER_DATATYPE = {"INT","TINYINT","SMALLINT","MEDIUMINT","BIGINT"};
    public static final String[] DATE_TIME = {"DATE","DATETIME","TIMESTAMP","TIME","YEAR"};
    public static final String[] CHAR_TYPE = {"CHAR","VARCHAR"};
    
    public static final String[] OPTIONAL_LENGTH = {"TEXT", "BLOB", "TINYBLOB","TINYTEXT","INT","TINYINT","SMALLINT","MEDIUMINT","BIGINT","DATE","DATETIME","TIMESTAMP","TIME","YEAR","CHAR"};
    public static final String[] LENGTH_NOT_REQUIRED_TYPES = {"DATE","DATETIME","TIMESTAMP","TIME","TEXT","BLOB","MEDIUMTEXT"};
     
   

}
