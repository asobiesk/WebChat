/**
 * 
 */
package prozWebChatServer;

import java.sql.*;
import java.util.Date;
import java.util.HashMap;

import oracle.jdbc.pool.OracleDataSource;

/**
 * Klasa odpowiadająca za współpracę aplikacji serwera z bazą danych Zawiera
 * metody podłączenia do bazy danych oraz logowania danych (używana baza danych:
 * Oracle)
 * 
 * @author Adam Sobieski
 */
public class WebChatDAO {
	private static final String user = "*****";
	private static final String password = "*****";
	private static final String database = "jdbc:oracle:thin:asobiesk/asobiesk@//localhost:1521/xe"; // Dane do
																										// połączenia z
																										// bazą danych
	private Connection conn = null; // Aktywne połączenie z bazą
	private static HashMap<String, Integer> ids = new HashMap<String, Integer>(); // SessionID-UserID in database

	/**
	 * Metoda służąca do nawiązania połączenia z bazą danych Aktywne połączenie
	 * zapisuje w parametrze <code>conn</code>
	 * 
	 * @throws SQLException
	 *             Błąd poleceń SQL
	 */
	public void connect() throws SQLException {
		try {
			OracleDataSource ods = new OracleDataSource();
			ods.setURL(database);
			ods.setUser(user);
			ods.setPassword(password);
			conn = ods.getConnection();

		} catch (SQLException s) {
			System.out.println("Failed to connect database" + s.getMessage());
		}
	}

	/**
	 * Metoda loguje użytkownika aplikacji do bazy danych Pobiera z bazy danych
	 * najwyższy obecny identyfikator użytkownika, przypisuje nowemu użytkownikowi
	 * ów identyfikator zwiększony o jeden oraz loguje parę sesja-identyfikator do
	 * mapy <code>ids</code>. Następnie przy pomocy polecenia SQL zapisuje:
	 * (1)Identyfikator (2)Login (3)Datę zalogowania do bazy danych *
	 * 
	 * @param login
	 *            Nazwa użytkownika
	 * @param sessionId
	 *            ID sesji
	 * @throws SQLException
	 *             Błąd poleceń SQL
	 */
	public void LogUser(String login, String sessionId) throws SQLException {
		try {
			java.util.Date date = new Date();
			Object param = new java.sql.Timestamp(date.getTime());
			Statement statement = conn.createStatement();
			ResultSet resultSet = statement.executeQuery("Select Max(userid) from \"User\"");
			resultSet.next();
			int maxId = resultSet.getInt(1);
			++maxId;
			Integer logMaxID = new Integer(maxId);
			ids.put(sessionId, logMaxID); // log user id to the ids hashmap
			PreparedStatement ps = conn
					.prepareStatement("INSERT INTO \"User\"(userid, login, logindate) VALUES (?,?,?)");
			ps.setInt(1, maxId);
			ps.setString(2, login);
			ps.setObject(3, param);
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Failed to log User: " + e.getMessage());
		}

	}

	/**
	 * Metoda loguje wiadomość do bazy danych. Zapisuje do bazy danych (1)Datę
	 * nadania wiadomości (2)ID nadawcy (3)Login nadawcy (4)Pierwsze 99 znaków
	 * wiadomości
	 * 
	 * @param login
	 *            Nazwa użytkownika
	 * @param message
	 *            Wiadomość
	 * @param sessionId
	 *            Sesja
	 */
	public void LogMessage(String login, String message, String sessionId) {
		try {
			java.util.Date date = new Date();
			Object param = new java.sql.Timestamp(date.getTime());
			int userId = ids.get(sessionId).intValue();
			PreparedStatement ps = conn.prepareStatement(
					"INSERT INTO message(\"Date\",user_userid, user_login, \"content\") VALUES (?,?,?,?)");
			ps.setObject(1, param);
			ps.setInt(2, userId);
			ps.setString(3, login);
			ps.setString(4, message);
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Failed to log user: " + e.getMessage());
		}
	}

	/**
	 * Metoda loguje plik do bazy danych Zapisuje do bazy danych (1)Datę nadania
	 * pliku (2)ID nadawcy (3)Nazwę pliku (4)Login nadawcy
	 * 
	 * @param login
	 *            Nazwa użytkownika (nadawcy)
	 * @param Filename
	 *            Nazwa pliku
	 * @param sessionId
	 * 
	 *            Sesja
	 */
	public void LogFile(String login, String Filename, String sessionId) {
		try {
			java.util.Date date = new Date();
			Object param = new java.sql.Timestamp(date.getTime());
			int userId = ids.get(sessionId).intValue();
			PreparedStatement ps = conn
					.prepareStatement("INSERT INTO \"File\"(\"Date\",userid, filename, user_login) VALUES (?,?,?,?)");
			ps.setObject(1, param);
			ps.setInt(2, userId);
			ps.setString(3, Filename);
			ps.setString(4, login);
			ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Failed to log file: " + e.getMessage());
		}
	}
}
