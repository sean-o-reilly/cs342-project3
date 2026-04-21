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
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;


public class GuiClient extends Application {

	HashMap<String, Scene> sceneMap;
	Client clientConnection;
	String clientUsername;

	HashSet<String> usernames = new HashSet<String>();
	ListView<String> usersList = new ListView<String>();

	int clickRow = -1;
	int clickCol = -1;
	int clientId = -1;
	Label win = new Label();
	Label error = new Label();
	Label turn = new Label();
	Label opponent = new Label();

	// login scene
	VBox loginSceneBox;
	TextField t2;
	Text error1;
	Button b2;

	//main scenes
	Stage primaryStage;
	Client client;
	Scene scene1Login, scene2Lobby, scene3Game, scene4GameEnd;

    GameStateDTO gameState;
	GridPane boardGrid = new GridPane();
	VBox chat = new VBox(10);
    
    VBox leaderboardData = new VBox(10);
    ScrollPane leaderboard;

	public static void main(String[] args) {
		launch(args);
	}

	private Scene createScene1Login(){
		VBox scene1v, title;
		BorderPane s1bp;
		Label title1, title2, login;

		title1 = new Label();
		title1.setText("Welcome to Checkers!");
		title1.setFont(Font.font("Times New Roman", FontWeight.BOLD, 30));

		title2 = new Label();
		title2.setText("Please enter your username:");
		title2.setFont(Font.font("Times New Roman", FontWeight.BOLD, 20));

		title = new VBox(30, title1, title2);
		title.setStyle("-fx-background-radius: 10; -fx-border-color: #a119b9; -fx-border-radius: 30");
		title.setPadding(new Insets(25));

		login = new Label();
		login.setText("LOGIN");
		login.setFont(Font.font("Times New Roman",30));

		t2 = new TextField();
		t2.setPromptText("Enter a username");
		t2.setFont(Font.font("Times New Roman",15));

		error1 = new Text("");

		b2 = new Button("Continue");
		b2.setStyle("-fx-font-family: Times New Roman; -fx-text-fill: #ffffff; -fx-background-color: #a119b9");

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
		rulesBox.setPadding(new Insets(25));
		rulesBox.setStyle("-fx-background-radius: 5; -fx-border-color: #a119b9; -fx-border-radius: 5");
            
        Label leaderboardLabel = new Label("Leaderboard (User : Wins)");
        leaderboard = new ScrollPane(leaderboardData);

		continueButton = new Button("Find a game");
		continueButton.setStyle("-fx-font-family: Times New Roman; -fx-text-fill: #ffffff; -fx-background-color: #a119b9");
		continueButton.setOnAction(e->{
            Message findGame = new Message("", Message.MessageType.FindGameReq);
            clientConnection.send(findGame);
		});

		scene2v = new VBox(30, rulesBox, leaderboardLabel, leaderboard, continueButton);
		scene2v.setStyle("-fx-font-size: 15 ; -fx-font-family: Times New Roman; -fx-background-color: #87ceeb");
		scene2v.setPadding(new Insets(25));
		s2bp = new BorderPane();
		s2bp.setStyle("-fx-background-color: #add8e6");
		s2bp.setPadding(new Insets(25));
		s2bp.setCenter(scene2v);
		return new Scene(s2bp, 700, 700);	
	}

	private Scene createScene3Game(){
		Button continueButton, sendButton;
		VBox scene3v, sendMessage;
		BorderPane s3bp;
		boardGrid = new GridPane();
		Label title, titleMessage;
		TextField tfInputMessage;

		title = new Label();
		title.setText("Checkers!");
		title.setFont(Font.font("Times New Roman", FontWeight.BOLD, 35));
		title.setStyle("-fx-background-radius: 10; -fx-border-color: #a119b9; -fx-border-radius: 5");
		title.setPadding(new Insets(25));
		
		int[][] piece= new int[8][8];

		//red pieces
		for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 8; col++) {
                    if ((row + col) % 2 == 1) {
                        piece[row][col] = 1;
                    }
                }
            }

		//black pieces
		for (int row = 5; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				if ((row + col) % 2 == 1) {
					piece[row][col] = 2;
				}
			}
		}

		//cells
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				StackPane cell = new StackPane();
				cell.setPrefSize(45,45);
	
				if((row+col)%2 ==1){
					cell.setStyle("-fx-background-color: #afb4b4");
				}
				else{
					cell.setStyle("-fx-background-color: #d1d4d4");
				}

				if(piece[row][col] !=0 && (row+col)%2 ==1){
					Circle circle = new Circle(20);
					if(piece[row][col] == 1){
						circle.setFill(Color.RED);
					}
					else{
						circle.setFill(Color.BLACK);
					}
					cell.getChildren().add(circle);
				}
				boardGrid.add(cell, col, row);
			}
		}

		titleMessage = new Label();
		titleMessage.setText("Message Chat!");
		titleMessage.setFont(Font.font("Times New Roman", FontWeight.BOLD, 20));
	
		chat = new VBox(10);
		chat.setPadding(new Insets(10));

		ScrollPane scroll = new ScrollPane(chat);
		scroll.setPrefHeight(250);
		scroll.setFitToWidth(true);

		tfInputMessage = new TextField();
		tfInputMessage.setPromptText("Write a message to send");
		tfInputMessage.setFont(Font.font("Times New Roman", 20));

		sendButton = new Button("Send");
		sendButton.setStyle("-fx-font-family: Times New Roman; -fx-text-fill: #ffffff; -fx-background-color: #a119b9");
		sendButton.setOnAction(e->{
			String temp = tfInputMessage.getText().trim();
			if(!temp.isEmpty()){
				clientConnection.send(new Message(temp, Message.MessageType.GameChatTextMessage));
				tfInputMessage.clear();
			}
		});

		sendMessage = new VBox(10, titleMessage, scroll, tfInputMessage, sendButton);
		sendMessage.setPadding(new Insets(20));
		sendMessage.setStyle("-fx-background-color: #add8e6; -fx-background-radius: 5; -fx-border-color: #000000; -fx-border-radius: 5");
		sendMessage.setPrefHeight(250);
		sendMessage.setMaxWidth(300);

		continueButton = new Button("Press when game is over.");
		continueButton.setStyle("-fx-font-family: Times New Roman; -fx-text-fill: #ffffff; -fx-background-color: #a119b9");
		
		continueButton.setOnAction(e->{
			primaryStage.setScene(sceneMap.get("gameEnd"));
		});

		scene3v = new VBox(20, title, turn, opponent, error, boardGrid, sendMessage, continueButton);
		scene3v.setStyle("-fx-background-color: #87ceeb");
		scene3v.setPadding(new Insets(25));

		ScrollPane s3 = new ScrollPane();
		s3.setContent(scene3v);
		s3.setFitToWidth(true);
		s3bp = new BorderPane();
		s3bp.setStyle("-fx-background-color: #add8e6");
		s3bp.setPadding(new Insets(25));
		s3bp.setCenter(s3);

		return new Scene(s3bp, 700, 700);	
	}

	private void drawState(){
		boardGrid.getChildren().clear();

		if(gameState.playerRedID == clientId){
			turn.setText("You are RED piece.");
			turn.setFont(Font.font("Times New Roman", 20));

			if(gameState.blackUsername == null){
				opponent.setText("Your opponent: waiting for opponent...");
				opponent.setFont(Font.font("Times New Roman", 20));
			}
			else{
				opponent.setText("Your opponent: " + gameState.blackUsername);
				opponent.setFont(Font.font("Times New Roman", 20));

			}
		}

		else{
			turn.setText("You are BLACK piece.");
			turn.setFont(Font.font("Times New Roman", 20));

			if(gameState.redUsername == null){
				opponent.setText("Your opponent: waiting for opponent...");
				opponent.setFont(Font.font("Times New Roman", 20));
			}
			else{
				opponent.setText("Your opponent: " + gameState.redUsername);
				opponent.setFont(Font.font("Times New Roman", 20));
			}
		}

		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				StackPane cell = new StackPane();
				cell.setPrefSize(45,45);
				final int row_  = row;
				final int col_ = col;
			
				if(row == clickRow && col == clickCol){
					cell.setStyle("-fx-background-color: #43e61a");
				}

				else if((row+col)%2 ==1){
					cell.setStyle("-fx-background-color: #afb4b4");
				}
				else{
					cell.setStyle("-fx-background-color: #d1d4d4");
				}

				if(gameState != null && gameState.board[row][col]!= null){
					GameStateDTO.Piece piece = gameState.board[row][col];
					Circle circle = new Circle(20);
					if(piece.isRed){
						circle.setFill(Color.RED);
					}
					else{
						circle.setFill(Color.BLACK);
					}

					if(piece.isKing){
						circle.setStroke(Color.GOLD);
						circle.setStrokeWidth(5);
					}
					cell.getChildren().add(circle);
				}

				cell.setOnMouseClicked(e->{
					if(clickRow == -1){
						if(gameState.board[row_][col_] != null){
							clickRow = row_;
							clickCol = col_;
							drawState();
						}
					}
					else{
						Message move = new Message(Message.MessageType.MovePieceReq, clickRow, clickCol, row_, col_);
						clientConnection.send(move);
						clickRow = -1;
						clickCol = -1;
					}
				});
			
				boardGrid.add(cell, col, row);
			}
		}
	}

	private Scene createScene4GameEnd(){
		Button playAgain, quitButton;
		VBox scene4v;
		BorderPane s4bp;
		Label option;


		win.setFont(Font.font("Times New Roman", FontWeight.BOLD, 15));

		option = new Label();
		option.setText("Press first button to play with same opponent of second button to quit: ");
		option.setFont(Font.font("Times New Roman", 15));


		quitButton = new Button("Quit");
		quitButton.setStyle("-fx-font-family: Times New Roman; -fx-text-fill: #ffffff; -fx-background-color: #a119b9");


		quitButton.setOnAction(e->{
            clientConnection.send(new Message("", Message.MessageType.LeaveGameReq));
		});

		playAgain = new Button("Play again");
		playAgain.setStyle("-fx-font-family: Times New Roman; -fx-text-fill: #ffffff; -fx-background-color: #a119b9");

		playAgain.setOnAction(e->{
            clientConnection.send(new Message("", Message.MessageType.PlayAgainReq));
		});

		scene4v = new VBox(30, win, option, playAgain, quitButton);
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
			Platform.runLater(()->{
				Label clientMessage = new Label();
				clientMessage.setText(message.body);
				clientMessage.setFont(Font.font("Times New Roman", 15));
			    clientMessage.setStyle("-fx-background-color: #add8e6; -fx-border-color: #000000; -fx-padding: 5;");
				chat.getChildren().add(clientMessage);
			});
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
                clientConnection.send(new Message("", Message.MessageType.LeaderboardRequest));

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
				clientId = Integer.valueOf(message.body);
                primaryStage.setScene(sceneMap.get("game"));
            });
        }
        else if (message.type == Message.MessageType.LeaveGameOK) {
            Platform.runLater(() -> {
                primaryStage.setScene(sceneMap.get("lobby"));
                clientConnection.send(new Message("", Message.MessageType.LeaderboardRequest));
            });
        }
        else if (message.type == Message.MessageType.GameStateNoti) {
            Platform.runLater(() -> {
                if (message.gameState == null) {
                    System.out.println("Received null game state?");
                }
                else {
                    gameState = message.gameState;
					error.setText("");
					primaryStage.setScene(sceneMap.get("game"));
					drawState();
					if(gameState.winnerID != -1){
						if(gameState.winnerID == gameState.playerRedID){
							win.setText("Red player wins!");
							win.setFont(Font.font("Times New Roman", FontWeight.BOLD, 30));
						}
						else if(gameState.winnerID == 0){
							win.setText("It's a draw");
							win.setFont(Font.font("Times New Roman", FontWeight.BOLD, 30));
						}
						else{
							win.setText("Black player wins!");
							win.setFont(Font.font("Times New Roman", FontWeight.BOLD, 30));
						}
						primaryStage.setScene(sceneMap.get("gameEnd"));
					}

                }
            });
        }
        else if (message.type == Message.MessageType.GameChatNoti) {
            Platform.runLater(() -> {
                Label clientMessage = new Label(message.body);
                clientMessage.setFont(Font.font("Times New Roman", 15));
                clientMessage.setStyle("-fx-background-color: #add8e6; -fx-border-color: #000000; -fx-padding: 5;");

                chat.getChildren().add(clientMessage);
            });
        }
        else if (message.type == Message.MessageType.MovePieceRejected) {
            Platform.runLater(() -> {
                String errorReason = message.body;
                error.setText("Invalid move! Reason: " + errorReason);
				error.setFont(Font.font("Times New Roman", 20));
                error.setStyle("-fx-text-fill: #ff1111");
            });
        }

		else if (message.type == Message.MessageType.PlayerLeftGameNoti) {
            Platform.runLater(() -> {
                System.out.println("Opponent left.");
                String errorReason = message.body;
                error.setText("Your opponent left the game");
				error.setFont(Font.font("Times New Roman", 20));
                error.setStyle("-fx-text-fill: #ff1111");
            });
        }
        else if (message.type == Message.MessageType.LeaderboardResponse) {
            Platform.runLater(() -> {
                System.out.println("Recevied leaderboard data");
                leaderboardData.getChildren().clear();
                message.list.forEach(s -> {
                    System.out.println(s);
                    leaderboardData.getChildren().add(new Label(s));
                });
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
