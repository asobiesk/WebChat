package prozWebChatServer;

import java.io.IOException;
import java.util.HashMap;
import java.nio.ByteBuffer;
import java.sql.*;
import oracle.jdbc.*;
import oracle.jdbc.pool.OracleDataSource;
import javax.enterprise.context.ApplicationScoped;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ApplicationScoped
@ServerEndpoint(value = "/websocketendpoint")
public class WebSocketEndpoint {

	private WebChatDAO DAO;
	private static HashMap<String, String> users = new HashMap<String, String>(); // SessionID-Login
	private boolean FileJustReceived = false;

	@OnOpen
	public void onOpen(Session session) throws SQLException {
		System.out.println("Witam");
		DAO = new WebChatDAO();
		System.out.println("Tutaj jeszcze tak");
		DAO.connect();
		String UserID = session.getId();
		users.put(UserID, null);
		System.out.println("Zalogowa�em sesje z nullem!");
		
	}

	@OnClose
	public void onClose(Session session) {
		System.out.println("Closed!");
	}

	@OnError
	public void onError(Throwable error) {
	}

	@OnMessage
	public void onMessage(String message, Session session) throws SQLException {
		try {
			System.out.println("onMessage String");
			if (FileJustReceived) {
				System.out.println("Plik");
				FileJustReceived = false;
				String UserLogin = users.get(session.getId());
				
				DAO.LogFile(UserLogin, message);
			} else if (users.get(session.getId()) == null) {
				System.out.println("Dosta�em w�a�nie login!");
				users.replace(session.getId(), message);
				DAO.LogUser(message);
				return;
			} else {
				System.out.println("Normalna wiadomo��");
				String UserLogin = users.get(session.getId());
				DAO.LogMessage(UserLogin, message);
			}
			for (Session oneSession : session.getOpenSessions()) {
				if (oneSession.isOpen()) {
					oneSession.getBasicRemote().sendText(message);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@OnMessage
	public void onMessage(ByteBuffer file, Session session) {
		FileJustReceived = true;
		System.out.println("Serwer: dodarl plik");

		try {
			String login = users.get(session.getId());

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

} // public class WebSocketEndpoint
