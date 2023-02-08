package com.appcino.as.sqltoexcel;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

//import com.appiancorp.core.expr.exceptions;


public class DBUtils {

	public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	public static SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

	private static final Logger LOG = Logger.getLogger(DBUtils.class);

	public static Connection getConnection(String dataSourceName, Context ctx_)
			throws NamingException, SQLException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Getting Connection for Datasource name:"	+ dataSourceName);
		}
		DataSource ds = (DataSource) ctx_.lookup(dataSourceName);
		return ds.getConnection();
	}

	public static String getSelectSQL(String tableName, String[] columns) {
		StringBuffer sql = new StringBuffer("select ");
		String columnsAsList = StringUtils.join(columns, ", ");
		sql.append(columnsAsList);
		sql.append(" from ").append(tableName);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Generated Select SQL: " + sql);
		}
		return sql.toString();
	}

	public static int[] getColumnTypes(Connection conn, String tableName,
			String[] columns) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement(getSelectSQL(tableName, columns));
			rs = ps.executeQuery();
			int[] columnTypes = new int[columns.length];
			for (int i = 0; i < columns.length; i++) {
				int coltype = rs.getMetaData().getColumnType(i + 1);
				columnTypes[i] = coltype;
			}
			return columnTypes;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (Exception e) {
			}
			try {
				if (ps != null) {
					ps.close();
				}
			} catch (Exception e) {
			}
		}
	}

	private static String getFormattedValue(ResultSet rs, int column)
			throws SQLException {

		int coltype = rs.getMetaData().getColumnType(column);
		Date d = null;
		switch (coltype) {
		case Types.BIT:
		case Types.BOOLEAN:
			boolean b = rs.getBoolean(column);
			if (rs.wasNull()) {
				return "";
			} else {
				return b ? "1" : "0";
			}
		case Types.DATE:
			d = rs.getDate(column);
			if (d == null || rs.wasNull()) {
				return "";
			} else {
				return DBUtils.DATE_FORMAT.format(d);
			}
		case Types.TIMESTAMP:
			d = rs.getTimestamp(column);
			if (d == null || rs.wasNull()) {
				return "";
			} else {
				return DBUtils.DATETIME_FORMAT.format(d);
			}
		case Types.TIME:
			d = rs.getTime(column);
			if (d == null || rs.wasNull()) {
				return "";
			} else {
				return DBUtils.TIME_FORMAT.format(d);
			}
		case Types.DOUBLE:
		case Types.FLOAT:
		case Types.DECIMAL:
		case Types.NUMERIC:
		case Types.REAL:
			double doub = rs.getDouble(column);
			if (rs.wasNull()) {
				return "";
			} else {
				NumberFormat f = NumberFormat.getInstance();
				f.setGroupingUsed(false);
				f.setMinimumFractionDigits(0);
				f.setMaximumFractionDigits(500);

				return f.format(doub) + "";
			}
		case Types.INTEGER:
		case Types.BIGINT:
		case Types.SMALLINT:
		case Types.TINYINT:
			Long l = rs.getLong(column);
			if (rs.wasNull()) {
				return "";
			} else {
				return l.toString();
			}

		case Types.VARCHAR:
		case Types.CHAR:
		case Types.LONGVARCHAR:
			String s = rs.getString(column);
			if (rs.wasNull()) {
				return "";
			} else if (StringUtils.isEmpty(s)) {
				return "";
			} else {
				s = s.replace("\"", "\"\"");
				return s;
			}

		case Types.NVARCHAR:
		case Types.NCHAR:
		case Types.LONGNVARCHAR:
			String n = rs.getNString(column);
			if (rs.wasNull()) {
				return "";
			} else if (StringUtils.isEmpty(n)) {
				return "";
			} else {
				n = n.replace("\"", "\"\"");
				return n;
			}
			
		default:
			LOG.warn("Error no handler for " + coltype);
			return "";
		}
	}

	public static StringBuffer exportData(Connection conn, String sql, String delimiter, boolean includeHeader)
			throws SQLException, IOException {

		StringBuffer s = new StringBuffer();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();

			ResultSetMetaData md = rs.getMetaData();
			int cols = md.getColumnCount();
			if(includeHeader){
				for (int i = 0; i < cols; i++) {
					s.append("\"").append(md.getColumnName(i + 1)).append("\"");
					if (i + 1 < cols) {
						s.append(delimiter);
					} else {
						s.append("\n");
					}
				}
			}
			while (rs.next()) {
				for (int i = 0; i < cols; i++) {
					s.append(getFormattedValue(rs, i + 1));
					if (i + 1 < cols) {
						s.append(delimiter);
					} else {
						s.append("\n");
					}
				}
			}
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (Exception e) {
			}
			try {
				if (ps != null) {
					ps.close();
				}
			} catch (Exception e) {
			}
		}
		return s;
	}

}