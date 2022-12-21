package com.remisiki.illust.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class Sqlite {

	protected String path;
	protected Connection connection;
	protected Statement statement;

	public Sqlite(String path) {
		this.path = path;
	}

	public Sqlite(File file) {
		this.path = file.getAbsolutePath();
	}

	public Sqlite(Path path) {
		this.path = path.toString();
	}

	public void connect() throws SQLException, ClassNotFoundException, FileNotFoundException {
		File file = new File(this.path);
		if (!file.exists()) throw new FileNotFoundException();
		Class.forName("org.sqlite.JDBC");
		this.connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.path));
		this.statement = connection.createStatement();
	}

	public void close() throws SQLException {
		if (this.statement != null) {
			this.statement.close();
		}
		if (this.connection != null) {
			this.connection.close();
		}
	}

	public void executeBatch(String[] requests) throws SQLException {
		this.connection.setAutoCommit(false);
		for (String request: requests) {
			this.statement.addBatch(request);
		}
		this.statement.executeBatch();
		this.connection.commit();
		this.statement.clearBatch();
		this.connection.setAutoCommit(true);
	}

	public void execute(String request) throws SQLException {
		this.executeBatch(new String[] {request});
	}

	public String[][] query(String request) throws SQLException {
		List<String[]> result = new ArrayList<>();
		ResultSet rs = this.statement.executeQuery(request);
		int columnSize = rs.getMetaData().getColumnCount();
		while (rs.next()) {
			String[] row = new String[columnSize];
			for (int i = 0; i < columnSize; i ++) {
				row[i] = rs.getString(i + 1);
			}
			result.add(row);
		}
		String[][] arrayResult = new String[result.size()][];
		for (int i = 0; i < result.size(); i ++) {
			arrayResult[i] = result.get(i);
		}
		return arrayResult;
	}

}