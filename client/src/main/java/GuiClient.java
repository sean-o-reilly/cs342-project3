import java.io.Serializable;
import java.util.HashMap;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.util.HashSet;
import javafx.scene.control.ComboBox;
import java.util.List;
import java.util.ArrayList;
import javafx.beans.value.ObservableValue;

public class GuiClient extends Application {

	HashMap<String, Scene> sceneMap;
	Client clientConnection;
	String clientUsername;
	String style = new String("-fx-background-color: blue;"+"-fx-font-family: 'serif';");

	// messaging scene
	TextField t1;
	Button b1;
	ListView<String> listItems;
	VBox messageSceneBox;
	ComboBox<String> cb1;

	HashSet<String> usernames = new HashSet<String>();
	ListView<String> usersList = new ListView<String>();

	// login scene
	VBox loginSceneBox;
	TextField t2;
	Text error1;
	Button b2;
	
	public static void main(String[] args) {
		launch(args);
	}

	private void updateUserList() {
		usersList.getItems().clear();
		usersList.getItems().addAll(usernames);
	}

	private void handleReceivedMessage(Message message, Stage primaryStage) {
		System.out.println("Received server message: type=" + 
			message.type + "\n" +
			", body=" + message.body + "\n" +
			"user=" + message.user
		);

		if (message.type == Message.MessageType.ChatNoti) {
			Platform.runLater(()->{listItems.getItems().add(message.body);});
		}
		else if (message.type == Message.MessageType.UserJoinedNoti) {
			Platform.runLater(()->{
				listItems.getItems().add(message.body);
				usernames.add(message.user);
				updateUserList();
			});
		}
		else if (message.type == Message.MessageType.UserLeftNoti) {
			Platform.runLater(()->{
				listItems.getItems().add(message.body);
				usernames.remove(message.user);
				updateUserList();
			});
		}
		else if (message.type == Message.MessageType.RespActiveUsers) {
			Platform.runLater(() ->{
				usernames.clear();
				usernames.addAll(message.list);
				updateUserList();
			});
		}
		else if (message.type == Message.MessageType.LoginFailed) {
			Platform.runLater(()->{
				error1.setText("Invalid username, please enter a different one.");
			});
		}
		else if (message.type == Message.MessageType.LoginOK) {
			Platform.runLater(()->{
				primaryStage.setScene(sceneMap.get("messaging"));
				clientConnection.send(new Message("", Message.MessageType.GetActiveUsers));
				error1.setText("");
				clientUsername = message.body;
			});
		}
		else {
			System.out.println("Unhandled message received: " + message.type + message.body);
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		clientConnection = new Client(message -> handleReceivedMessage(message, primaryStage));
		// construct a Client with a callback, which adds data received from the server
							
		clientConnection.start();

		listItems = new ListView<String>();

		sceneMap = new HashMap<String, Scene>();
		
		sceneMap.put("messaging",  createMessagingScene());
		sceneMap.put("login", createLoginScene());
		
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });

		primaryStage.setScene(sceneMap.get("login"));
		primaryStage.setTitle("Client");
		primaryStage.show();
	}

	public Scene createMessagingScene() {
		t1 = new TextField();
		b1 = new Button("Send to...");
		cb1 = new ComboBox<>();
		cb1.getItems().add("All Users");
		cb1.getItems().add("Selected Users");
		cb1.setValue("All Users");

		usersList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		b1.setOnAction(e->{
			if (t1.getText().trim().length() > 0) {
				if (cb1.getValue().equals("All Users")) {
					clientConnection.send(new Message(t1.getText(), Message.MessageType.GlobalTextMessage)); 
				}
				else {
					Message msg = new Message(t1.getText(), Message.MessageType.DirectTextMessage);
					msg.list.addAll(usersList.getSelectionModel().getSelectedItems());
					msg.list.add(clientUsername); // include self in each DM
					clientConnection.send(msg);
				}
			}
			t1.clear();
		});

		messageSceneBox = new VBox(10, t1, new HBox(b1, cb1), listItems, new Text("Users"), usersList);
		messageSceneBox.setStyle(style);

		return new Scene(messageSceneBox, 400, 300);
	}

	public Scene createLoginScene() {
		t2 = new TextField();
		t2.setPromptText("Enter a username");

		b2 = new Button("Login");
		b2.setOnAction(e->{
			Message loginRequest = new Message(t2.getText(), Message.MessageType.LoginRequest);
			clientConnection.send(loginRequest);
		});

		error1 = new Text("");

		loginSceneBox = new VBox(10, t2, b2, error1);
		loginSceneBox.setStyle(style);

		return new Scene(loginSceneBox, 400, 300);
	}
}
