/**
 * Klasy aplikacji klienta
 */
package prozWebChat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;

/**
 * Kontroler sceny zawierający obsługę zdarzeń, komunikację z serwerem oraz
 * współpracę z FXML-em.
 * 
 * @author Adam Sobieski
 */
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
	@FXML
	Button btnSound;
	@FXML
	Button btnFile;
	@FXML
	Button btnDownload;
	@FXML
	Label lblFile;

	private String user = null; // zapamiętany login
	private WebSocketClient webSocketClient; // obsługa gniazda
	private Clip clip = null; // plik dzwiękowy
	private boolean sound = true; // zmienna określa, czy dzwięk jest włączony (true) czy wyłączony (false)
	private ByteBuffer storedFile = null; // aktualnie przechowywany plik (ostatni dodany)
	private String privateKey = null; // unikalny klucz do szyfrowania/deszyfrowania wiadomości

	/**
	 * Inicjalizacja kontrolera wywołany jest konstruktor podklasy WebSocketClient
	 * odpowiedzialnej za komunikację z serwerem oraz zainicjalizowany zostaje klip
	 * dzwiękowy (odtwarzany przy wiadomości)
	 */
	@FXML
	private void initialize() {
		System.out.println("Dziala");
		webSocketClient = new WebSocketClient();
		user = userTextField.getText();
		System.out.println("Dzia�a");
		try {
			// Open an audio input stream.
			File soundFile = new File("C:\\Users\\Adam\\eclipse-workspace\\test\\src\\test\\sound.wav");
			AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
			// Get a sound clip resource.
			clip = AudioSystem.getClip();
			// Open audio clip and load samples from the audio input stream.
			clip.open(audioIn);
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Obsługa kliknięcia w przycisk Set ustawienie loginu
	 * 
	 * @throws IOException
	 */
	@FXML
	private void btnSet_Click() throws IOException {
		System.out.println("Set");
		if (userTextField.getText().isEmpty()) {
			return;
		}
		user = userTextField.getText(); // pole USER ustawiane
		btnSend.setDisable(false);
		btnSet.setDisable(true);
		btnFile.setDisable(false);
		userTextField.setDisable(true);
		webSocketClient.sendUsername(user);
	}

	/**
	 * Obsługa zdarzenia przycisku Send zainicjalizowane wysyłanie wiadomości
	 */
	@FXML
	private void btnSend_Click() {
		System.out.println("Send");
		if (messageTextField.getText().isEmpty()) // nie wysyłaj pustego stringa
			return;
		webSocketClient.sendMessage(messageTextField.getText());
		messageTextField.clear();
	}

	/**
	 * Obsługa zdarzenia przycisku Sound włączanie/wyłączanie dzwięku
	 */
	@FXML
	private void btnSound_Click() {
		System.out.println(privateKey);
		if (sound == true) {
			sound = false;
			btnSound.setText("Sound OFF");
		} else {
			sound = true;
			btnSound.setText("Sound ON");
		}
	}

	/**
	 * Obsługa zdarzenia przycisku Download zapisanie pliku na dysku
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@FXML
	private void btnDownload_Click() throws FileNotFoundException, IOException {
		try {
			if (storedFile != null) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Save file");
				File directory = fileChooser.showSaveDialog(null);
				if (directory != null) {
					FileOutputStream output = new FileOutputStream(directory.getAbsolutePath());
					output.write(storedFile.array());
					output.close();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Obsługa kliknięcia klawisza ENTER - wywołanie zdarzenia przycisku SEND
	 * 
	 * @param key
	 *            naciśnięty klawisz
	 */
	@FXML
	private void messageKeyClicked(KeyEvent key) {
		if (key.getCode() == KeyCode.ENTER && !btnSend.isDisabled()) {
			btnSend_Click();
		}
	}

	/**
	 * Obsługa kliknięcia klawisza ENTER - wywołanie zdarzenia przycisku SET
	 * 
	 * @param key
	 *            naciśnięty klawisz
	 * @throws IOException
	 */
	@FXML
	private void loginKeyClicked(KeyEvent key) throws IOException {
		if (key.getCode() == KeyCode.ENTER && !btnSet.isDisabled())
			btnSet_Click();
	}

	/**
	 * Obsługa zdarzenia przycisku File wybór pliku, inicjalizacja przesłania pliku
	 * 
	 * @throws IOException
	 * @throws EncodeException
	 */
	@FXML
	private void btnFile_Clicked() throws IOException, EncodeException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		File myFile = fileChooser.showOpenDialog(null);
		if(myFile == null)
			return;
		if(myFile.length() >= 1024*1024*4) //sprawdzenie rozmiaru pliku - max 4Mb
		{
			System.out.println("Plik za duży!");
			return;
		}
		byte[] byteArray = getBytesFromFile(myFile);
		ByteBuffer buffer = ByteBuffer.wrap(byteArray);
		String name = myFile.getName();
		webSocketClient.sendMessage(buffer, name);
	}

	/**
	 * Konwersja z typu File na tablicę bajtów potrzebna do przesłania pliku poprzez
	 * ByteBuffer
	 * 
	 * @param file
	 *            Plik do przesłania
	 * @return Plik w postaci tablicy bajtów
	 * @throws IOException
	 */
	private static byte[] getBytesFromFile(File file) throws IOException {
		long length = file.length();
		if (length > Integer.MAX_VALUE) {
			throw new IOException("File is too large!"); // Ograniczenie rozmiaru pliku
		}
		byte[] bytes = new byte[(int) length];

		int offset = 0;
		int numRead = 0;

		InputStream is = new FileInputStream(file);
		try {
			while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
				offset += numRead;
			}
		} finally {
			is.close();
		}

		if (offset < bytes.length) {
			throw new IOException("Could not completely read file " + file.getName());
		}
		return bytes;
	}

	/**
	 * Zamykanie sesji
	 * 
	 * @param closeReason
	 */
	public void closeSession(CloseReason closeReason) {
		try {
			webSocketClient.session.close(closeReason);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getUser() {
		return user;
	}

	/**
	 * Szyfrowanie wiadomości algorytmem Cezara na podstawie otrzymanego od serwera
	 * klucza
	 * 
	 * @param  message wiadomość
	 *            (plaintext)
	 * @return Zaszyfrowana wiadomość
	 */
	public String encryptMessage(String message) {
		try {
			System.out.println("===Wchodze do encrypta===");
			System.out.println("===Szyfruje wiadomosc " + message + " // kluczem: " + privateKey);
			StringBuffer encrypted = new StringBuffer(); // Bufor do tworzenia nowego, zaszyfrowanego stringa
			for (int i = 0; i < message.length(); ++i) { // Przejście po każdym znaku wiadomości
				int newChar = (int) message.charAt(i) + privateKey.charAt(i % 15); // Przesunięcie o odpowiadającą
																					// wartość klucza (szyfr Cezara)
				encrypted.append((char) newChar); // Dodanie nowego znaku do wyjściowego bufora
			}
			String encryptedMessage = encrypted.toString();
			System.out.println("Zaszyfrowana: " + encryptedMessage);
			return encryptedMessage;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Deszyfrowanie wiadomości algorytmem Cezara na podstawie otrzymanego od
	 * serwera klucza
	 * 
	 * @param message zaszyfrowana
	 *            wiadomość
	 * @return Wiadomość (plaintext)
	 */
	public String decryptMessage(String message) {
		try {
			StringBuffer decrypted = new StringBuffer(); // analogicznie jak w funkcji powyżej
			for (int i = 0; i < message.length(); ++i) {
				int newChar = (int) message.charAt(i) - privateKey.charAt(i % 15);
				decrypted.append((char) newChar);
			}
			String encryptedMessage = decrypted.toString();
			System.out.println("Odszyfrowana: " + encryptedMessage);
			return encryptedMessage;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Klasa <code>WebSocketClient</code> - klasa zagnieżdżona odpowiadająca za
	 * komunikację aplikacji z serwerem
	 * 
	 * @author Adam Sobieski
	 *
	 */
	@ClientEndpoint
	public class WebSocketClient {
		private Session session; // Sesja połączenia z serwerem
		private boolean fileJustReceived = false; // Pomocnicza zmienna mówiąca, czy czekamy na obsługę nazwy pliku

		/**
		 * Połączenie z gniazdem serwera
		 */
		public WebSocketClient() {
			connectToWebSocket();
		}

		/**
		 * Metoda wywoływana domyślnie przy otworzeniu sesji
		 * 
		 * @param session
		 */
		@OnOpen
		public void onOpen(Session session) {
			System.out.println("Connection is opened.");
			this.session = session;
		}

		/**
		 * Metoda wywoływana domyślnie przy zakończeniu sesji
		 * 
		 * @param closeReason
		 */
		@OnClose
		public void onClose(CloseReason closeReason) {
			System.out.println("Connection is closed: " + closeReason.getReasonPhrase());
		}

		/**
		 * Metoda wywoływana domyślnie przy błędzie
		 * 
		 * @param throwable
		 *            Typ błędu
		 */
		@OnError
		public void onError(Throwable throwable) {
			System.out.println("Error occured");
			throwable.printStackTrace();
		}

		/**
		 * Metoda wywoływana w momencie otrzymania od serwera wiadomości tekstowej Jeśli
		 * flaga fileJustReceived jest ustawiona na wartość TRUE, metoda obsługuje
		 * otrzymanie nazwy uprzednio otrzymanego pliku. W przeciwnym razie obsługuje
		 * otrzymanie "normalnej" wiadomości, czyli deszyfruje ją i umieszcza w oknie
		 * tekstowym aplikacji (wywołując dzwięk)
		 * 
		 * @param message
		 *            Otrzymana wiadomość (String)
		 * @param session
		 *            Sesja
		 */
		@OnMessage
		public void onMessage(String message, Session session) {
			try {
				if (fileJustReceived) { // Jeśli właśnie dostaliśmy plik
					fileJustReceived = false; // ...to ta sytuacja jest już obsługiwana
					String filename = message;
					Platform.runLater(() -> {
						lblFile.setText("File received: " + filename); // ustawienie komunikatu z nazwą pliku
					});
					return;
				} else if (user == null || user.length() == 0) // privateKey received
				{
					privateKey = message;
					return;

				}
				String decrypted = decryptMessage(message);
				chatTextArea.setText(chatTextArea.getText() + decrypted + "\n");
				System.out.println("Message was received");
				if (sound) {
					clip.start();
					clip.setMicrosecondPosition(0);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		/**
		 * Metoda wywoływana w momencie otrzymania pliku wywołująca dzwięk oraz
		 * ustawiająca flagę fileJustReceived
		 * 
		 * @param transferedFile
		 *            otrzymany Plik
		 * @param session
		 *            sesja
		 * @throws IOException
		 */
		@OnMessage
		@FXML
		public void onMessage(ByteBuffer transferedFile, Session session) throws IOException {
			System.out.println("File was received");
			fileJustReceived = true;
			lblFile.setVisible(true);
			btnDownload.setVisible(true);
			storedFile = transferedFile;
			if (sound) {
				clip.start();
				clip.setMicrosecondPosition(0);
			}

		}

		/**
		 * Metoda łącząca aplikację klienta z serwerem
		 */
		private void connectToWebSocket() {
			WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
			try {
				URI uri = URI.create("ws://localhost:8080/WebChatServer/websocketendpoint");
				webSocketContainer.connectToServer(this, uri);
			} catch (DeploymentException | IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Wysyłanie wiadomości tekstowej wywołanie metody szyfrującej i przesłanie
		 * zaszyfrowanej wiadomości na serwer
		 * 
		 * @param message
		 *            Wiadomość
		 */
		public void sendMessage(String message) {
			try {
				chatTextArea.positionCaret(chatTextArea.getText().length());
				System.out.println("Message was sent: " + message);
				String toSend = user + ": " + message;
				String encrypted = encryptMessage(toSend);
				System.out.println("Message encrypted: " + encrypted);
				session.getBasicRemote().sendText(encrypted);

			} catch (Exception ex) {
				ex.printStackTrace();
				System.out.println("Jest error");
			}
		}

		/**
		 * Metoda wysyła serwerowi ustawiony przez Użytkownika login wywoływana tylko
		 * raz w czasie sesji
		 * 
		 * @param name
		 *            Ustalony login
		 * @throws IOException
		 */
		public void sendUsername(String name) throws IOException {
			session.getBasicRemote().sendText(name);
			System.out.println("Wysy�am login systemowi!");

		}

		/**
		 * Metoda przesyłająca serwerowi plik
		 * 
		 * @param transferedFile
		 *            Plik
		 * @param name
		 *            Nazwa pliku
		 * @throws IOException
		 * @throws EncodeException
		 */
		public void sendMessage(ByteBuffer transferedFile, String name) throws IOException, EncodeException {
			session.getBasicRemote().sendBinary(transferedFile);
			session.getBasicRemote().sendText(name);
			System.out.println("File was send: " + name);
			chatTextArea.positionCaret(chatTextArea.getText().length());
		}
	} // public class WebSocketClient
} // public class WebSocketChatStageControler
