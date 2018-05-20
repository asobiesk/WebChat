package prozWebChatServer;

import java.sql.*;
import java.util.Date;
import java.util.HashMap;

import oracle.jdbc.*;
import oracle.jdbc.pool.OracleDataSource;

public class WebChatDAO {
	private static final String user = "****";
	private static final String password = "****";
	private static final String database = "jdbc:oracle:thin:asobiesk/asobiesk@//localhost:1521/xe";

	private Connection conn = null;
	private static HashMap<String, Integer> ids = new HashMap<String, Integer>(); // SessionID-UserID in database

	public void connect() throws SQLException {
		try {
			System.out.println("Wszed³em do connecta!");
			OracleDataSource ods = new OracleDataSource();
			System.out.println("Jestem tutaj!");
			ods.setURL(database);
			ods.setUser(user);
			ods.setPassword(password);
			conn = ods.getConnection();
			System.out.println("Po³¹czono z baz¹ danych!");

		} catch (SQLException s) {
			System.out.println("Failed to connect database" + s.getMessage());
		}
	}

	public void LogUser(String login) throws SQLException {
		try {
			System.out.println("Loguje usera do bd");
			java.util.Date date = new Date();
			Object param = new java.sql.Timestamp(date.getTime());
			PreparedStatement ps = conn.prepareStatement("INSERT INTO \"User\"(login, logindate) VALUES (?,?)");
			ps.setString(1, login);
			ps.setObject(2, param);
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Failed to log User: " + e.getMessage());
		}

	}

	public void LogMessage(String login, String message) {
		try {
			System.out.println("Loguje wiadomoœæ do bd");
			java.util.Date date = new Date();
			Object param = new java.sql.Timestamp(date.getTime());
			PreparedStatement ps = conn
					.prepareStatement("INSERT INTO message(\"Date\", user_login, \"content\") VALUES (?,?,?)");
			ps.setObject(1, param);
			ps.setString(2, login);
			ps.setString(3, message);
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Failed to log user: " + e.getMessage());
		}

	}

	public void LogFile(String login, String Filename) {
		try {
			System.out.println("Loguje plik do bd");
			java.util.Date date = new Date();
			Object param = new java.sql.Timestamp(date.getTime());
			PreparedStatement ps = conn
					.prepareStatement("INSERT INTO \"File\"(\"Date\", filename, user_login) VALUES (?,?,?)");
			ps.setObject(1, param);
			ps.setString(2, Filename);
			ps.setString(3, login);
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Failed to log file: " + e.getMessage());
		}
	}
}
