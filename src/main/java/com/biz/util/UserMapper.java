package com.biz.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;

public class UserMapper implements RowMapper<Map<String, String>> {
	public Map<String, String> mapRow(ResultSet rs, int arg1) throws SQLException {
		Map<String, String> user = new HashMap<String, String>();
		user.put("username", rs.getString("username"));
		user.put("password", rs.getString("password"));

		return user;
	}
}