import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Consumer;

public class Client extends Thread {

	Client(Consumer<Message> call) {
		callback = call;
	}

	public void run() {
		try {
			socketClient = new Socket("127.0.0.1",5555);
			out = new ObjectOutputStream(socketClient.getOutputStream());
			in = new ObjectInputStream(socketClient.getInputStream());
			socketClient.setTcpNoDelay(true);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		while (true) {
			try {
				Message message = (Message)in.readObject();
				callback.accept(message); // tell our callback we've received data
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
    }
	
	public void send(Message message) {
		try {
			out.writeObject(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	Socket socketClient;
	
	ObjectOutputStream out;
	ObjectInputStream in;
	
	private Consumer<Message> callback;
}
