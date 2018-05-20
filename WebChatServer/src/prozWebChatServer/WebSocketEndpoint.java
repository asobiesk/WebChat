package prozWebChatServer;

import java.io.IOException;
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
		System.out.println("Zalogowa³em sesje z nullem!");

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

			if (FileJustReceived) { //Server received a filename -> file handling
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
					
					
					} catch (IOException e) {
					e.printStackTrace();
				}
			}

			else if (users.get(session.getId()) == null) { //Server received a user's login -> user handling
				System.out.println("Dosta³em w³aœnie login!");
				users.replace(session.getId(), message);
				DAO.LogUser(message, session.getId());
				return;
			} 
			
			else { //Server received a normal message -> message handling
				System.out.println("Normalna wiadomoœæ");
				String UserLogin = users.get(session.getId());
				DAO.LogMessage(UserLogin, message, session.getId());
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
			for (Session oneSession : session.getOpenSessions()) {
				if (oneSession.isOpen() && !oneSession.getId().equals(session.getId())) {
					System.out.println("Wys³ano plik");
					oneSession.getBasicRemote().sendBinary(file);
					System.out.println("Serwer: wyslallem plik");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

} // public class WebSocketEndpoint
