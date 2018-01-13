package com.biz.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.properties")
public class ConnectionProperty {

 @Value("${masterdatabase}")
 private String masterDatabaseName;

 @Value("${mastertable}")
 private String masterTableName;

 public String getMasterDatabaseName() {
  return masterDatabaseName;
 }

 public void setMasterDatabaseName(String masterDatabaseName) {
  this.masterDatabaseName = masterDatabaseName;
 }

 public String getMasterTableName() {
  return masterTableName;
 }

 public void setMasterTableName(String masterTableName) {
  this.masterTableName = masterTableName;
 }
    public ConnectionProperty(){
    }

}
