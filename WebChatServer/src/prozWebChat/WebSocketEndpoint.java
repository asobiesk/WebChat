package prozWebChat;

import java.io.IOException;
import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

//adnotacja deklaruje klas� gniazda serwerowego
//w kontek�cie aplikacji
//i adres URI u�ywany przez klient�w do komunikacji
@ApplicationScoped
@ServerEndpoint("/websocketendpoint")
public class WebSocketEndpoint {
	// adnotacja metody, kt�ra b�dzie wo�ana
	// przy ka�dym nawi�zaniu po��czenia przez klienta
	@OnOpen
	public void onOpen(Session session) {
	}

	// adnotacja metody, kt�ra b�dzie wo�ana
	// przy ka�dym zamkni�ciu po��czenia przez klienta
	@OnClose
	public void onClose(Session session) {
	}

	// adnotacja metody, kt�ra b�dzie wo�ana po wyst�pieniu b��du
	@OnError
	public void onError(Throwable error) {
	}

	// adnotacja metody, kt�ra b�dzie wo�ana po ka�dym odbiorze wiadomo�ci
	@OnMessage
	public void onMessage(String message, Session session) {
		// rozg�oszenie otrzymanej wiadomo�ci
		// do wszystkich pod��czonych klient�w
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
