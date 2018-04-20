package prozWebChat;

import java.io.IOException;
import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

//adnotacja deklaruje klasê gniazda serwerowego
//w kontekœcie aplikacji
//i adres URI u¿ywany przez klientów do komunikacji
@ApplicationScoped
@ServerEndpoint("/websocketendpoint")
public class WebSocketEndpoint {
	// adnotacja metody, która bêdzie wo³ana
	// przy ka¿dym nawi¹zaniu po³¹czenia przez klienta
	@OnOpen
	public void onOpen(Session session) {
	}

	// adnotacja metody, która bêdzie wo³ana
	// przy ka¿dym zamkniêciu po³¹czenia przez klienta
	@OnClose
	public void onClose(Session session) {
	}

	// adnotacja metody, która bêdzie wo³ana po wyst¹pieniu b³êdu
	@OnError
	public void onError(Throwable error) {
	}

	// adnotacja metody, która bêdzie wo³ana po ka¿dym odbiorze wiadomoœci
	@OnMessage
	public void onMessage(String message, Session session) {
		// rozg³oszenie otrzymanej wiadomoœci
		// do wszystkich pod³¹czonych klientów
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
} // public class WebSocketEndpoint
