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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Label;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Font;

public class GuiClient extends Application {

	HashMap<String, Scene> sceneMap;
	Client clientConnection;
	String clientUsername;
	String style = new String("-fx-background-color: blue;"+"-fx-font-family: 'serif';");

	HashSet<String> usernames = new HashSet<String>();
	ListView<String> usersList = new ListView<String>();

	// login scene
	VBox loginSceneBox;
	TextField t2;
	Text error1;
	Button b2;

	//main scenes
	Stage primaryStage;
	Client client;
	Scene scene1Login, scene2Lobby, scene3Game, scene4GameEnd;

	public static void main(String[] args) {
		launch(args);
	}

	private Scene createScene1Login(){
		VBox scene1v;
		BorderPane s1bp;
		Label title, login;

		title = new Label();
		title.setText("Welcome to Checkers! Please create a username:");
		title.setFont(Font.font("Times New Roman", FontWeight.BOLD, 15));

		login = new Label();
		login.setText("LOGIN");
		login.setFont(Font.font("Times New Roman",30));

		t2 = new TextField();
		t2.setPromptText("Enter a username");
		t2.setFont(Font.font("Times New Roman",15));

		error1 = new Text("");

		b2 = new Button("Continue");
		b2.setOnAction(e->{
			if (t2.getText().trim().isEmpty()){
				error1.setText("Invalid username, please enter again.");
			}
			else{
				Message loginRequest = new Message(t2.getText(), Message.MessageType.LoginRequest);
				clientConnection.send(loginRequest);
			}
		});

		scene1v = new VBox(30, title, login, t2, b2, error1);
		scene1v.setStyle("-fx-font-size: 15 ; -fx-font-family: Times New Roman; -fx-background-color: #87ceeb");
		scene1v.setPadding(new Insets(25));
		s1bp = new BorderPane();
		s1bp.setStyle("-fx-background-color: #add8e6");
		s1bp.setPadding(new Insets(25));
		s1bp.setCenter(scene1v);
		return new Scene(s1bp, 700, 700);
	}

	private Scene createScene2Lobby(){
		Button continueButton;
		VBox scene2v;
		BorderPane s2bp;
		Label rule, rule1, rule2, rule3, title;

		rule = new Label();
		rule.setText("Rules of the game:");
		rule.setFont(Font.font("Times New Roman", FontWeight.BOLD, 30));
		rule1 = new Label();
		rule1.setText("1. Black moves first. Players then alternate moves. Moves are allowed on clack square.");
		rule1.setFont(Font.font("Times New Roman",15));
		rule2 = new Label();
		rule2.setText("2. Pieces move diagonally. Kings can also move backwards. Piece can move one square, unless it's making a jump");
		rule2.setFont(Font.font("Times New Roman",15));
		rule3 = new Label();
		rule3.setText("3. When piece is jumped(captured), it is removed from board. A player wins a game when opponent can't make a move");
		rule3.setFont(Font.font("Times New Roman",15));

		VBox rulesBox = new VBox(30, rule, rule1, rule2, rule3);
		rulesBox.setStyle("-fx-font-size: 15 ; -fx-font-family: Times New Roman; -fx-background-color: #87ceeb");
		rulesBox.setPadding(new Insets(25));
		rulesBox.setStyle("-fx-background-radius: 5; -fx-border-color: #000000; -fx-border-radius: 5");

		continueButton = new Button("Find a game");
		continueButton.setOnAction(e->{
            Message findGame = new Message("", Message.MessageType.FindGameReq);
            clientConnection.send(findGame);
		});

		scene2v = new VBox(30, rulesBox, continueButton);
		scene2v.setStyle("-fx-font-size: 15 ; -fx-font-family: Times New Roman; -fx-background-color: #87ceeb");
		scene2v.setPadding(new Insets(25));
		s2bp = new BorderPane();
		s2bp.setStyle("-fx-background-color: #add8e6");
		s2bp.setPadding(new Insets(25));
		s2bp.setCenter(scene2v);
		return new Scene(s2bp, 700, 700);	
	}

	private Scene createScene3Game(){
		Button continueButton;
		VBox scene3v;
		BorderPane s3bp;
		Label title;

		title = new Label();
		title.setText("Checkers!");
		title.setFont(Font.font("Times New Roman", FontWeight.BOLD, 30));

		continueButton = new Button("Press when game is over.");
		continueButton.setOnAction(e->{
			primaryStage.setScene(sceneMap.get("gameEnd"));
		});

		scene3v = new VBox(30, title, continueButton);
		scene3v.setStyle("-fx-font-size: 15 ; -fx-font-family: Times New Roman; -fx-background-color: #87ceeb");
		scene3v.setPadding(new Insets(25));
		s3bp = new BorderPane();
		s3bp.setStyle("-fx-background-color: #add8e6");
		s3bp.setPadding(new Insets(25));
		s3bp.setCenter(scene3v);
		return new Scene(s3bp, 700, 700);	
	}

	private Scene createScene4GameEnd(){
		Button playAgain, quitButton;
		VBox scene4v;
		BorderPane s4bp;
		Label win, lose, option;

		option = new Label();
		option.setText("Press first button to play with same opponent of second button to quit: ");
		option.setFont(Font.font("Times New Roman", 15));

		quitButton = new Button("Quit");
		quitButton.setOnAction(e->{
            primaryStage.setScene(sceneMap.get("lobby"));
		});

		playAgain = new Button("Play again");
		playAgain.setOnAction(e->{
			primaryStage.setScene(sceneMap.get("game"));
		});

		scene4v = new VBox(30, option, playAgain, quitButton);
		scene4v.setStyle("-fx-font-size: 15 ; -fx-font-family: Times New Roman; -fx-background-color: #87ceeb");
		scene4v.setPadding(new Insets(25));
		s4bp = new BorderPane();
		s4bp.setStyle("-fx-background-color: #add8e6");
		s4bp.setPadding(new Insets(25));
		s4bp.setCenter(scene4v);
		return new Scene(s4bp, 700, 700);
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
		}
		else if (message.type == Message.MessageType.UserJoinedNoti) {
			Platform.runLater(()->{
				usernames.add(message.user);
				updateUserList();
			});
		}
		else if (message.type == Message.MessageType.UserLeftNoti) {
			Platform.runLater(()->{
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
				primaryStage.setScene(sceneMap.get("lobby"));
				clientConnection.send(new Message("", Message.MessageType.GetActiveUsers));
				error1.setText("");
				clientUsername = message.body;
			});
		}
        else if (message.type == Message.MessageType.FindGameResponse) {
            Platform.runLater(() ->{
                System.out.println("Found available game id=" + message.body + "; sending join request.");
                clientConnection.send(new Message(message.body, Message.MessageType.JoinGameReq));
            });
        }
        else if (message.type == Message.MessageType.JoinGameOK) {
            Platform.runLater(() -> {
                primaryStage.setScene(sceneMap.get("game"));
            });
        }
		else {
			System.out.println("Unhandled message received: type: " + message.type + ", body: " + message.body);
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// construct a Client with a callback, which adds data received from the server
		clientConnection = new Client(message -> handleReceivedMessage(message, primaryStage));
		clientConnection.start();

		this.primaryStage = primaryStage;

		sceneMap = new HashMap<String, Scene>();
		
		sceneMap.put("login", createScene1Login());
		sceneMap.put("lobby", createScene2Lobby());
		sceneMap.put("game", createScene3Game());
		sceneMap.put("gameEnd", createScene4GameEnd());
		
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });

		primaryStage.setScene(sceneMap.get("login"));
		primaryStage.setTitle("Checkers");
		primaryStage.show();
	}
}
