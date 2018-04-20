package prozWebChat;

import java.io.IOException;
import java.net.URI;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

public class WebChatStageController extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ChatLayout.fxml"));
			AnchorPane root = fxmlLoader.load();
			Scene scene = new Scene(root);
			primaryStage.setScene(scene);
			primaryStage.setTitle("JavaFX Web Socket Client");
			primaryStage.setOnHiding(e -> primaryStage_Hiding(e, fxmlLoader));
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void primaryStage_Hiding(WindowEvent e, FXMLLoader fxmlLoader) {
		((WebSocketChatStageControler) fxmlLoader.getController())
				.closeSession(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Stage is hiding"));
	}

	public class WebSocketChatStageControler {
		@FXML
		TextField userTextField;
		@FXML
		TextArea chatTextArea;
		@FXML
		TextField messageTextField;
		@FXML
		Button btnSet;
		@FXML
		Button btnSend;
		private String user;
		private WebSocketClient webSocketClient;

		@FXML
		private void initialize() {
			webSocketClient = new WebSocketClient();
			user = userTextField.getText();
			System.out.println("Dzia³a");
		}

		@FXML
		private void btnSet_Click() {
			if (userTextField.getText().isEmpty()) {
				return;
			}
			user = userTextField.getText();
		}

		@FXML
		private void btnSend_Click() {
			webSocketClient.sendMessage(messageTextField.getText());
		}

		public void closeSession(CloseReason closeReason) {
			try {
				webSocketClient.session.close(closeReason);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@ClientEndpoint
		public class WebSocketClient {
			private Session session;

			public WebSocketClient() {
				connectToWebSocket();
			}

			@OnOpen
			public void onOpen(Session session) {
				System.out.println("Connection is opened.");
				this.session = session;
			}

			@OnClose
			public void onClose(CloseReason closeReason) {
				System.out.println("Connection is closed: " + closeReason.getReasonPhrase());
			}

			@OnError
			public void onError(Throwable throwable) {
				System.out.println("Error occured");
				throwable.printStackTrace();
			}

			@OnMessage
			public void onMessage(String message, Session session) {
				System.out.println("Message was received");
				chatTextArea.setText(chatTextArea.getText() + message + "\n");
			}

			private void connectToWebSocket() {
				WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
				try {
					URI uri = URI.create("ws://localhost:8080/WebSocket/websocketendpoint");
					webSocketContainer.connectToServer(this, uri);
				} catch (DeploymentException | IOException e) {
					e.printStackTrace();
				}
			}

			public void sendMessage(String message) {
				try {
					System.out.println("Message was sent: " + message);
					session.getBasicRemote().sendText(user + ": " + message);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		} // public class WebSocketClient
	} // public class WebSocketChatStageControler
}// clas WeChatStageController