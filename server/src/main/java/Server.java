import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.control.ListView;
import java.util.HashMap;
import java.io.IOException;
import java.time.LocalDateTime;

public class Server {
	int clientsLazyGUID = 1;
	ServerThread server;

	HashMap<Integer, ClientThread> clientMap = new HashMap<Integer, ClientThread>();

	int gamesLazyGUID = 1;
    HashMap<Integer, CheckersGame> games = new HashMap<Integer, CheckersGame>();
	
	Server() {
		server = new ServerThread();
		server.start();
	}

    private void Log(String str) {
        System.out.println("[SERVER " + LocalDateTime.now() + "] " + str);
    }
	
	public class ServerThread extends Thread {
		public void run() {
			try (ServerSocket mysocket = new ServerSocket(5555);) {
				Log("Server is waiting for a client!");
			
				while (true) {
					ClientThread c = new ClientThread(mysocket.accept(), clientsLazyGUID);
					Log("client has connected to server: " + "client #" + clientsLazyGUID);
					c.start();

                    synchronized(clientMap) {
					    clientMap.put(clientsLazyGUID++, c);
                    }
				}
			}
			catch(Exception e) {
				Log("Server socket did not launch");
			}
		}
	}

	private boolean IsValidUsername(String username) {
        boolean res;
        synchronized(clientMap) {
		    res = clientMap.values().stream().noneMatch(clientThread -> username.equals(clientThread.username));
        }
		return res && !username.equals("All");
	}

	class ClientThread extends Thread {
		Socket connection;
		ObjectInputStream in;
		ObjectOutputStream out;

		final int id;
        Integer activeGameID;
		String username;
		
		ClientThread(Socket s, int id){
			this.connection = s;
			this.id = id;	
		}
		
		public void notifyClients(Message message) {
            synchronized(clientMap) {
                clientMap.forEach((id, t)->{
                    try {
                        t.out.writeObject(message);
                    }
                    catch(Exception e) {}
                });
            }
		}

		public void notifySomeClients(Message message, ArrayList<String> clients) {
            synchronized(clientMap) {
                clientMap.forEach((id, t)->{
                    if (clients.contains(t.username)) {
                        try {
                            t.out.writeObject(message);
                        }
                        catch(Exception e) {}
                    }
                });
            }
		}

        public void notifyClientByID(Message message, int id) {
            if (message == null) return;

            synchronized(clientMap) {
                try {
                    clientMap.get(id).out.writeObject(message);
                }
                catch (Exception e) {
                    Log("Error notifying client " + username + " id="+ id);
                    e.printStackTrace();
                }
            }
        }

        // Notify players of a game
        public void notifyPlayers(Message message, CheckersGame game) {
            Log("Notifying players of game id=" + game.gameID);
            
            String redUser = null;
            String blackUser = null;

            synchronized(clientMap) {
                ClientThread red = clientMap.get(game.playerRedID);
                ClientThread black = clientMap.get(game.playerBlackID);

                if (red != null) {
                    redUser = red.username;
                }

                if (black != null) {
                    blackUser = black.username;
                }
            }

            GameStateDTO gameState = game.toStateDTO(redUser, blackUser);

            Message state = new Message(Message.MessageType.GameStateNoti, gameState);

            if (state.gameState == null) {
                Log("Failed to create game state DTO for game id=" + game.gameID);
            }

            if (gameState.winnerID > 0) {
                Log("User id=" + gameState.winnerID + " won in game " + game.gameID);
                game.restart();
            }
            else if (gameState.winnerID == 0) {
                Log("Game id=" + game.gameID + " ended in a draw.");
                game.restart();
            }

            if (game.playerRedID != -1) {
                notifyClientByID(message, game.playerRedID);
                notifyClientByID(state, game.playerRedID);
            }
            if (game.playerBlackID != -1) {
                notifyClientByID(message, game.playerBlackID);
                notifyClientByID(state, game.playerBlackID);
            }
        }

		private void removeClient() {
			Log("Socket error from client: id=" + id + " (" + username + ") - closing down!");

            synchronized(games) {
                CheckersGame activeGame = games.get(activeGameID);
                if (activeGame != null) {
                    Log("Handling leaving condition for client id=" + id);
                    handlePlayerError(activeGame);
                }
            }

            synchronized(clientMap) {
			    clientMap.remove(id);
            }
		}

        private void handlePlayerLeft(CheckersGame game) {
            Log("Restarting game id=" + game.gameID);

            Message playerLeft = new Message(username + " left the game.", Message.MessageType.PlayerLeftGameNoti);
            game.restart();
            notifyPlayers(playerLeft, game);

            if (game.playerBlackID == id) {
                game.playerBlackID = -1;
            }
            else if (game.playerRedID == id) {
                game.playerRedID = -1;
            }

            String redUser = null;
            String blackUser = null;

            synchronized(clientMap) {
                ClientThread red = clientMap.get(game.playerRedID);
                ClientThread black = clientMap.get(game.playerBlackID);

                if (red != null) {
                    redUser = red.username;
                }

                if (black != null) {
                    blackUser = black.username;
                }
            }

            Message state = new Message(Message.MessageType.GameStateNoti, game.toStateDTO(redUser, blackUser));

            if (game.playerRedID != -1) {
                notifyClientByID(state, game.playerRedID);
            }

            if (game.playerBlackID != -1) {
                notifyClientByID(state, game.playerBlackID);
            }

            Log("Removed " + username + " from game id=" + game.gameID);
        }

        private void handlePlayerError(CheckersGame game) {
            synchronized(games) {
                Log(username + " was in game id=" + game.gameID + " before triggering an error.");
                handlePlayerLeft(game);
            }
        }

        private void login() {
			while (true) {
				try {
					Message message = (Message)in.readObject();
					String prefix = new String("Login request from (id=" + id + "): ");

					if (message.type == Message.MessageType.LoginRequest) {
						if (IsValidUsername(message.body)) {
							Log(prefix + "Valid username: " + message.body);
							username = message.body;

							Message loginOK = new Message(username, Message.MessageType.LoginOK);
							out.writeObject(loginOK);

							break;
						}
						Log(prefix + "Invalid username! : " + message.body);

						Message loginBad = new Message("", Message.MessageType.LoginFailed);
						out.writeObject(loginBad);
					}
					else {
						Log("Client (id=" + id + ") did not send a login request? Ignored.");
					}
				}
				catch(Exception e) {
					e.printStackTrace();
					removeClient();
					return;
				}
			}

			Message joinMsg = new Message(username + " joined the server.", Message.MessageType.UserJoinedNoti);
			joinMsg.user = username;
			notifyClients(joinMsg);
        }

        private CheckersGame getAvailableGame() {
            CheckersGame res = null;

            synchronized(games) {
                for (CheckersGame game : games.values()) {
                    if (game.hasWaitingPlayer()) {
                        return game;
                    }
                    if (res == null && game.hasNoPlayers()) {
                        res = game;
                    }
                }

                if (res != null) {
                    return res;
                }

                res = new CheckersGame(gamesLazyGUID++);
                games.put(res.gameID, res);
                return res;
            }
        }

        private void handleGlobalMessage(Message message) {
            Log(username + " sent a global message.");
            Message chatNotiMsg = new Message(username + " : " + message.body, Message.MessageType.ChatNoti);
            chatNotiMsg.user = username;
            notifyClients(chatNotiMsg);
        }

        private void handleDM(Message message) {
            Log(username + " sent a direct message.");
            Message chatNotiMsg = new Message(username + " : " + message.body, Message.MessageType.ChatNoti);
            chatNotiMsg.user = username;
            notifySomeClients(chatNotiMsg, message.list);
        }

        private void handleActiveUsers(Message message) throws IOException {
            Message usersResp = new Message("", Message.MessageType.RespActiveUsers);

            synchronized(clientMap) {
                clientMap.forEach((id, client)->{
                    if (client.username != null) {
                        usersResp.list.add(client.username);
                    }
                });
            }

            out.writeObject(usersResp);
        }

        private void handleFindGame(Message message) throws IOException {
            Log("User " + username + " requested to find a game.");

            CheckersGame game = getAvailableGame();

            if (game == null) {
                Log("Failed to find game during find game request.");
                return;
            }

            Integer id = game.gameID;

            Log("Sending " + username + " game id=" + id);
            Message gameMsg = new Message(id.toString(), Message.MessageType.FindGameResponse);

            out.writeObject(gameMsg);
        }

        private void handleMoveReq(Message message) throws IOException {
            synchronized(games) {
                CheckersGame game = games.get(activeGameID);

                if (game == null) {
                    Log("Failed to find game during move request?");
                    return;
                }

                String error = game.move(message.rowStart, message.colStart, message.rowEnd, message.colEnd, id);

                if (error == null) {
                    Log(username + " made a valid move. In game id=" + game.gameID);

                    Message notifyPlayers = new Message(username + " made a move.", Message.MessageType.MovePieceNoti);
                    notifyPlayers(notifyPlayers, game);
                }
                else {
                    Log(username + " made an invalid move in game id=" + game.gameID);

                    Message invalidMove = new Message(error, Message.MessageType.MovePieceRejected);
                    notifyClientByID(invalidMove, id);
                }
            }
        }

        private void handleJoinGame(Message message) {
            int bodyID = Integer.parseInt(message.body);
            Log("User " + username + " requested to join game id=" + bodyID);

            synchronized(games) {
                if (games.containsKey(bodyID)) {
                    CheckersGame game = games.get(bodyID);

                    try {
                        boolean joinedAsRed = game.joinGame(id);

                        if (joinedAsRed) {
                            Log(username + " joined game id=" + game.gameID + " as red.");
                        }
                        else {
                            Log(username + " joined game id=" + game.gameID + " as black.");
                        }

                        activeGameID = game.gameID;

                        notifyClientByID(new Message(String.valueOf(id), Message.MessageType.JoinGameOK), id);
                        Message notifyPlayers = new Message(username + " joined the game.", Message.MessageType.PlayerJoinedGameNoti);

                        notifyPlayers(notifyPlayers, game);
                    }
                    catch(Exception e) {
                        Log(username + " tried to join full game id=" + game.gameID);
                    }
                }
                else {
                    Log("Invalid game ID request? : " + username + " " + bodyID);
                }
            }
        }

        private void handleLeaveGame(Message message) throws IOException {
            if (activeGameID == -1) {
                Log(username + " requested to leave a null game?");
                return;
            }

            Log(username + " has requested to leave their game id=" + activeGameID);

            CheckersGame game = games.get(activeGameID);

            handlePlayerLeft(game);
            activeGameID = -1;

            Message resp = new Message("", Message.MessageType.LeaveGameOK);
            out.writeObject(resp);
        }

        private void handleGameChatMessage(Message message) throws IOException {
            synchronized(games) {
                CheckersGame game = games.get(activeGameID);

                if (game != null) {
                    Log(username + " sent a game chat message to game id=" + activeGameID);
                    notifyPlayers(new Message(username + ": " + message.body, Message.MessageType.GameChatNoti), game);
                }
                else {
                    Log(username + " failed to find game to send chat message to? game id=" + activeGameID);
                }
            }
        }

        private void handlePlayAgainReq(Message message) {
            Log("Received play again request from client id=" + id + " for game " + activeGameID);
            synchronized(games) {
                CheckersGame game = games.get(activeGameID);
                if (game == null) {
                    Log("Failed to handle play again reqeuest for game id=" + activeGameID);
                    return;
                }
                
                // HACK: pass in null since usernames dont matter here
                game.restart();
                notifyPlayers(new Message(Message.MessageType.GameStateNoti, game.toStateDTO(null, null)), game);
            }
        }

        private void handleRecvMessage() throws java.io.IOException, java.lang.ClassNotFoundException {
            Message message = (Message)in.readObject();

            if (message.type == Message.MessageType.GlobalTextMessage) {
                handleGlobalMessage(message);
            }
            else if (message.type == Message.MessageType.DirectTextMessage) {
                handleDM(message);
            }
            else if (message.type == Message.MessageType.GetActiveUsers) {
                handleActiveUsers(message);
            }
            else if (message.type == Message.MessageType.FindGameReq) {
                handleFindGame(message);
            }
            else if (message.type == Message.MessageType.JoinGameReq) {
                handleJoinGame(message);
            }
            else if (message.type == Message.MessageType.LeaveGameReq) {
                handleLeaveGame(message);
            }
            else if (message.type == Message.MessageType.MovePieceReq) {
                handleMoveReq(message);
            }
            else if (message.type == Message.MessageType.GameChatTextMessage) {
                handleGameChatMessage(message);
            }
            else if (message.type == Message.MessageType.PlayAgainReq) {
                handlePlayAgainReq(message);
            }
            else {
                Log("Unhandled message from client? id=" + id + "(" + username + ") " + " type: " + message.type);
            }
        }
		
		public void run() {
			try {
				in = new ObjectInputStream(connection.getInputStream());
				out = new ObjectOutputStream(connection.getOutputStream());
				connection.setTcpNoDelay(true);	
			}
			catch(Exception e) {
				Log("Streams not open");
			}

            login();
			
			while (true) {
				try {
                    handleRecvMessage();
				}
				catch(Exception e) {
					e.printStackTrace();

					Message leaveNotiMsg = new Message(username + " has left the server!", Message.MessageType.UserLeftNoti);
					leaveNotiMsg.user = username;
					notifyClients(leaveNotiMsg);

					removeClient();
					return;
				}
			}
		}
	}
}
