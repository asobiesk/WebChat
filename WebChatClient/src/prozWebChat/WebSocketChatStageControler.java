package prozWebChat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import java.awt.Desktop;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

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

	private String user;
	private WebSocketClient webSocketClient;
	private Clip clip = null;
	private boolean sound = true;
	private ByteBuffer storedFile = null;

	@FXML
	private void initialize() {
		System.out.println("Dziala");
		webSocketClient = new WebSocketClient();
		user = userTextField.getText();
		System.out.println("Dzia³a");
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

	@FXML
	private void btnSet_Click() {
		System.out.println("Set");
		if (userTextField.getText().isEmpty()) {
			return;
		}
		user = userTextField.getText();
		btnSend.setDisable(false);
		btnSet.setDisable(true);
		btnFile.setDisable(false);
		userTextField.setDisable(true);
	}

	@FXML
	private void btnSend_Click() {
		System.out.println("Send");
		if (messageTextField.getText().isEmpty())
			return;
		webSocketClient.sendMessage(messageTextField.getText());
		messageTextField.clear();
	}

	@FXML
	private void btnSound_Click() {
		if (sound == true) {
			sound = false;
			btnSound.setText("Sound OFF");
		} else {
			sound = true;
			btnSound.setText("Sound ON");

		}

	}

	@FXML
	private void btnDownload_Click() throws FileNotFoundException, IOException {
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
	}

	@FXML
	private void messageKeyClicked(KeyEvent key) {
		if (key.getCode() == KeyCode.ENTER && !btnSend.isDisabled()) {
			btnSend_Click();
		}

	}

	@FXML
	private void loginKeyClicked(KeyEvent key) {
		if (key.getCode() == KeyCode.ENTER && !btnSet.isDisabled())
			btnSet_Click();
	}

	@FXML
	private void btnFile_Clicked() throws IOException {
		System.out.println("Dzia³a!!!!!");
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		File myFile = fileChooser.showOpenDialog(null);
		byte[] byteArray = getBytesFromFile(myFile);
		ByteBuffer buffer = ByteBuffer.wrap(byteArray);
		String name = myFile.getName();
		webSocketClient.sendMessage(buffer, name);

	}

	// Returns the contents of the file in a byte array.
	private static byte[] getBytesFromFile(File file) throws IOException {
		// Get the size of the file
		long length = file.length();

		// You cannot create an array using a long type.
		// It needs to be an int type.
		// Before converting to an int type, check
		// to ensure that file is not larger than Integer.MAX_VALUE.
		if (length > Integer.MAX_VALUE) {
			// File is too large
			throw new IOException("File is too large!");
		}

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int) length];

		// Read in the bytes
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

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file " + file.getName());
		}
		return bytes;
	}

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

	@ClientEndpoint
	public class WebSocketClient {
		private Session session;

		public WebSocketClient() {
			System.out.println("Konstruktor");
			connectToWebSocket();
			System.out.println("Konstruktor2");

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
			if (sound) {
				clip.start();
				clip.setMicrosecondPosition(0);
			}

		}

		@OnMessage
		@FXML
		public void onMessage(ByteBuffer buffer, Session session) throws IOException {
			System.out.println("File was received");
			lblFile.setVisible(true);
			btnDownload.setVisible(true);
			System.out.println("Rozmiar bufora: " + buffer.capacity());
			storedFile = buffer;

		}

		private void connectToWebSocket() {
			WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
			try {
				URI uri = URI.create("ws://localhost:8080/WebChatServer/websocketendpoint");
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
				System.out.println("Jest error");
			}
		}

		public void sendMessage(ByteBuffer buffer, String name) throws IOException {
			session.getBasicRemote().sendBinary(buffer);
			System.out.println("File was send: " + name);
		}
	} // public class WebSocketClient
} // public class WebSocketChatStageControler
