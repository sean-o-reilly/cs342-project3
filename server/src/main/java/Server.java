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

					clientMap.put(clientsLazyGUID++, c);
				}
			}
			catch(Exception e) {
				Log("Server socket did not launch");
			}
		}
	}

	private boolean IsValidUsername(String username) {
		boolean res = clientMap.values().stream().noneMatch(clientThread -> username.equals(clientThread.username));
		return res && !username.equals("All");
	}

	class ClientThread extends Thread {
		Socket connection;
		ObjectInputStream in;
		ObjectOutputStream out;

		final int id;
		String username;
		
		ClientThread(Socket s, int id){
			this.connection = s;
			this.id = id;	
		}
		
		public void notifyClients(Message message) {
			clientMap.forEach((id, t)->{
				try {
					t.out.writeObject(message);
				}
				catch(Exception e) {}
			});
		}

		// terrible time complexity but whatever
		public void notifySomeClients(Message message, ArrayList<String> clients) {
			clientMap.forEach((id, t)->{
				if (clients.contains(t.username)) {
					try {
						t.out.writeObject(message);
					}
					catch(Exception e) {}
				}
			});

		}

		private void removeClient() {
			Log("Socket error from client: id=" + id + " (" + username + ") - closing down!");
			clientMap.remove(id);
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

        private void handleRecvMessage() throws java.io.IOException, java.lang.ClassNotFoundException {
            Message message = (Message)in.readObject();

            String prefix = new String(username + "(id:" + id + ")");

            if (message.type == Message.MessageType.GlobalTextMessage) {
                Log(prefix + "sent: " + message.body);

                Message chatNotiMsg = new Message(username + " said: " + message.body, Message.MessageType.ChatNoti);
                chatNotiMsg.user = username;
                notifyClients(chatNotiMsg);
            }
            else if (message.type == Message.MessageType.DirectTextMessage) {
                Log(prefix + "sent a DM to " + message.list + " : " + message.body);

                Message chatNotiMsg = new Message(username + " said: " + message.body, Message.MessageType.ChatNoti);
                chatNotiMsg.user = username;
                notifySomeClients(chatNotiMsg, message.list);
            }
            else if (message.type == Message.MessageType.GetActiveUsers) {
                Log(prefix + " requested active users");
                Message usersResp = new Message("", Message.MessageType.RespActiveUsers);

                clientMap.forEach((id, client)->{
                    if (client.username != null) {
                        usersResp.list.add(client.username);
                    }
                });

                out.writeObject(usersResp);
            }
            else {
                Log("Unhandled message from client? id=" + id + "(" + username + ")");
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
