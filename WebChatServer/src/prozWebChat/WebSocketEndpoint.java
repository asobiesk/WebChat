package prozWebChat;

import java.io.IOException;
import java.nio.ByteBuffer;

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

	@OnOpen
	public void onOpen(Session session) {
		System.out.println("Tu jeszcze jest");
	}

	@OnClose
	public void onClose(Session session) {
	}

	@OnError
	public void onError(Throwable error) {
	}

	@OnMessage
	public void onMessage(String message, Session session) {
		try {
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
	public void onMessage(ByteBuffer buffer, Session session) {
		System.out.println("Serwer: dodarl plik");
		try {
			for (Session oneSession : session.getOpenSessions()) {
				if (oneSession.isOpen()) {
					System.out.println("Wys³ano plik");
					oneSession.getBasicRemote().sendBinary(buffer);
					System.out.println("Serwer: wyslallem plik");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
} // public class WebSocketEndpoint
