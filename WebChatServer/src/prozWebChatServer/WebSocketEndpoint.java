/**
 * Klasy aplikacji serwera
 */
package prozWebChatServer;

import java.io.IOException;
import java.util.Random;
import java.util.HashMap;
import java.nio.ByteBuffer;
import java.sql.*;
import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Klasa główna aplikacji serwerowej Odpowiada za komnunikację serwera z
 * aplikacjami klienckimi
 * 
 * @author Adam Sobieski
 *
 */
@ApplicationScoped
@ServerEndpoint(value = "/websocketendpoint")
public class WebSocketEndpoint {

	private WebChatDAO DAO; // obiekt służący do komunikacji z bazą danych
	private static HashMap<String, String> users = new HashMap<String, String>(); // SessionID-Login
	private static HashMap<String, String> keys = new HashMap<String, String>(); // SessionID-Key
	private boolean FileJustReceived = false; // flaga mówiąca o tym, czy plik właśnie został otrzymany
	/**
	 * Zestaw znaków służących do generowania kluczy
	 */
	private static final String charset = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890";

	/**
	 * Metoda wywoływana domyślnie podczas nawiązania połączenia aplikacji klienta z
	 * serwerem Rejestruje nową sesję do mapy <code>users</code> oraz wywołuje na
	 * rzecz tej sesji metodę generującą klucz do szyfrowania/deszyfrowania
	 * wiadomości. Tworzy również obiekt DAO (obiekt klasy WebChatDAO) służacy do
	 * komunikacji z bazą danych oraz wywołuję na tym obiekcie metodę
	 * <code>connect</code>, nawiązując tym samym połączenie z bazą danych
	 * 
	 * @param session
	 *            Sesja
	 * @throws SQLException
	 * @throws IOException
	 */
	@OnOpen
	public void onOpen(Session session) throws SQLException, IOException {
		System.out.println("Witam");
		DAO = new WebChatDAO();
		System.out.println("Tutaj jeszcze tak");
		DAO.connect();
		String UserID = session.getId();
		users.put(UserID, null);
		System.out.println("Zalogowa�em sesje z nullem!");
		String key = generateKey(session);
		session.getBasicRemote().sendText(key);

	}

	/**
	 * Metoda wywoływana domyślnie na koniec sesji
	 * 
	 * @param session
	 */
	@OnClose
	public void onClose(Session session) {
		System.out.println("Closed!");
	}

	/**
	 * Metoda wywoływana domyślnie przy wystąpieniu błędu
	 * 
	 * @param error
	 */
	@OnError
	public void onError(Throwable error) {
	}

	/**
	 * Metoda wywoływana podczas otrzymania wiadomości tekstowej Jeśli flaga
	 * fileJustReceived jest ustawiona na <code>true</code>, to metoda obsługuje
	 * otrzymanie nazwy pliku (przesyła ją do pozostałych użytkowników). Jeśli login
	 * nadawcy nie został dotychczas umieszczony w mapie <code>users</code>, to
	 * metoda obsługuje otrzymanie nazwy użytkownika (loguje ją do mapy
	 * <code>users</code> W przeciwnym razie metoda deszyfruje wiadomość kluczem
	 * należącym do jej nadawcy, po czym dla każdej aktywnej sesji szyfruje ją
	 * należącym do niej kluczem i wysyła zakodowaną wiadomość
	 * 
	 * @param message
	 *            Wiadomość tekstowa/Nazwa pliku
	 * @param session
	 *            Sesja
	 * @throws SQLException
	 */
	@OnMessage
	public void onMessage(String message, Session session) throws SQLException {
		try {
			System.out.println("onMessage String");

			if (FileJustReceived) { // Server received a filename -> file handling
				System.out.println("Plik");
				FileJustReceived = false;
				String UserLogin = users.get(session.getId());
				DAO.LogFile(UserLogin, message, session.getId());

				try {
					for (Session oneSession : session.getOpenSessions()) {
						if (oneSession.isOpen() && !oneSession.getId().equals(session.getId())) {
							oneSession.getBasicRemote().sendText(message);
						}
					}
					return;

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			else if (users.get(session.getId()) == null) { // Server received a user's login -> user handling
				System.out.println("Dosta�em w�a�nie login!");
				users.replace(session.getId(), message);
				DAO.LogUser(message, session.getId());
				return;
			}

			else { // Server received a normal message -> message handling
				System.out.println("Normalna wiadomo��: " + message);
				String UserLogin = users.get(session.getId());
				String decrypted = decryptMessage(message, session);
				DAO.LogMessage(UserLogin, decrypted, session.getId());

				for (Session oneSession : session.getOpenSessions()) {
					if (oneSession.isOpen()) {
						String encrypted = encryptMessage(decrypted, oneSession);
						oneSession.getBasicRemote().sendText(encrypted);
						System.out.println(
								"===Wysy�am do usera: " + users.get(oneSession.getId()) + " wiadomosc: " + encrypted);

					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Metoda wywoływana w momencie otrzymania pliku Ustawia flagę fileJustReceived
	 * na true, po czym wysyła plik każdemu aktywnemu użytkownikowi
	 * 
	 * @param file
	 *            Przesyłany plik
	 * @param session
	 */
	@OnMessage
	public void onMessage(ByteBuffer file, Session session) {
		FileJustReceived = true;
		System.out.println("Serwer: dodarl plik");

		try {
			for (Session oneSession : session.getOpenSessions()) {
				if (oneSession.isOpen() && !oneSession.getId().equals(session.getId())) {
					System.out.println("Wys�ano plik");
					oneSession.getBasicRemote().sendBinary(file);
					System.out.println("Serwer: wyslallem plik");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Metoda generująca losowy, 16-bajtowy klucz do szyfrowania/deszyfrowania
	 * wiadomości Po wygenerowaniu klucza do użytkownika loguje parę ID sesji -
	 * Klucz do mapy <code>keys</code>
	 * 
	 * @param session
	 *            Sesja
	 * @return Klucz
	 */
	public String generateKey(Session session) {
		Random random = new Random();
		StringBuffer key = new StringBuffer();
		int number = 0;
		for (int i = 0; i < 16; ++i) // generate random 16-characters string
		{
			number = random.nextInt(charset.length());
			if (number == charset.length())
				--number;
			char ch = charset.charAt(number);
			key.append(ch);
		}
		String generatedKey = key.toString();
		keys.put(session.getId(), generatedKey); // log key to HashMap
		return generatedKey;

	}

	/**
	 * Szyfrowanie wiadomości algorytmem Cezara na podstawie klucza
	 * 
	 * @param message
	 *            Wiadomość
	 * @param session
	 * @return Zawszyfrowana wiadomość
	 */
	public String encryptMessage(String message, Session session) {
		try {

			String key = keys.get(session.getId());
			System.out.println("===Szyfruje wiadomosc: " + message + " //kluczem " + key);
			StringBuffer encrypted = new StringBuffer();
			for (int i = 0; i < message.length(); ++i) {
				int newChar = (int) message.charAt(i) + key.charAt(i % 15);
				encrypted.append((char) newChar);
			}
			String encryptedMessage = encrypted.toString();
			System.out.println("Zaszyfrowana: " + encryptedMessage);
			return encryptedMessage;

		} catch (Exception e) {
			e.printStackTrace();
			return encryptMessage(message, session);
		}

	}

	/**
	 * Szyfrowanie wiadomości algorytmem Cezara na podstawie klucza
	 * 
	 * @param message
	 *            Wiadomość
	 * @param session
	 *            Sesja
	 * @return Odszyfrowana wiadomość
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public String decryptMessage(String message, Session session)
			throws IllegalBlockSizeException, BadPaddingException {
		try {

			String key = keys.get(session.getId());
			System.out.println("===Deszyfruje wiadomosc: " + message + " //kluczem " + key);
			StringBuffer decrypted = new StringBuffer();
			for (int i = 0; i < message.length(); ++i) {
				int newChar = (int) message.charAt(i) - key.charAt(i % 15);
				decrypted.append((char) newChar);
			}
			String encryptedMessage = decrypted.toString();
			System.out.println("Odszyfrowana: " + encryptedMessage);
			return encryptedMessage;
		} catch (Exception e) {
			e.printStackTrace();
			return "Zagubiona";
		}
	}

} // public class WebSocketEndpoint
