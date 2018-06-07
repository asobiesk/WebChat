package prozWebChat;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * Klasa <code>Main</code> służy do wywołania sceny graficznej aplikacji
 * klienta.
 * 
 * @author Adam Sobieski
 *
 */
public class Main extends Application {
	/**
	 * Wyświetlanie okna graficznego wstępne ustawienia *
	 */
	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ChatLayout.fxml"));
			AnchorPane root = fxmlLoader.load();
			Scene scene = new Scene(root);
			primaryStage.setScene(scene);
			primaryStage.setResizable(false);
			primaryStage.setTitle("PROZ Web Chat");
			primaryStage.setOnHiding(e -> primaryStage_Hiding(e, fxmlLoader));
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Obsługa zamknięcia aplikacji Rozłączenie sesji z serwerem
	 * 
	 * @param e
	 *            zdarzenie zamykające
	 * @param fxmlLoader fxmlLoader
	 */
	private void primaryStage_Hiding(WindowEvent e, FXMLLoader fxmlLoader) {
		try {
			((WebSocketChatStageControler) fxmlLoader.getController())
					.closeSession(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Stage is hiding"));
		} catch (NullPointerException n) { // in there is no connection to close
		}

	}

}
