import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.ListView;

public class Server{

	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	TheServer server;
	private Consumer<Serializable> callback;


	Server(Consumer<Serializable> call){

		callback = call;
		server = new TheServer();
		server.start();
	}


	public class TheServer extends Thread{

		public void run() {

			try(ServerSocket mysocket = new ServerSocket(5555);){
				System.out.println("Server is waiting for a client!");

				while(true) {

					ClientThread c = new ClientThread(mysocket.accept(), count);
					callback.accept("client has connected to server: " + "client #" + count);
					clients.add(c);
					c.start();

					count++;

				}
			}//end of try
			catch(Exception e) {
				callback.accept("Server socket did not launch");
			}
		}//end of while
	}


	class ClientThread extends Thread{

		String clientName = "placeholderName";
		Socket connection;
		int count;
		ObjectInputStream in;
		ObjectOutputStream out;

		ClientThread(Socket s, int count){
			this.connection = s;
			this.count = count;
		}

		String getClientName () {return clientName;}
		void setClientName (String clientName) {
			System.out.println("Setting Client Name");

			for (ClientThread client: clients) {
				if (client.getClientName().equals(clientName)) {
					// Username already exists
					System.out.println("Username already existed");

					List<String> recipient = new ArrayList<>();
					recipient.add("placeholderName");
					Message message  = new Message("JOIN", "SERVER", "USERNAME ALREADY EXISTS", recipient);

					updateSpecificClients(message);
					return;
				}
			}
			this.clientName = clientName;
			System.out.println("Client name set");

		}

		public void updateSpecificClients (Message message) {

			// find all recipient's index from clients' list
			List <Integer> indexes = new ArrayList<>();

			for (int i = 0; i < clients.size(); i++) {
				for (String t : message.getRecipients()) {
					System.out.println("t = " + t + ", client fetched = " +  clients.get(i).getClientName());
					if (t.equals(clients.get(i).getClientName())) {
						indexes.add(i);
					}
				}
			}

			for (int j = 0; j < indexes.size(); j++) {
				System.out.println("Client chosen: " + clients.get(indexes.get(j)).getClientName());
			}

			// send messages to specific recipients only
			for (int i = 0; i < indexes.size(); i++) {
				ClientThread t = clients.get(indexes.get(i));
				try {
					t.out.writeObject(message);
					System.out.println("MESSAGE: " + message.getContent() + " sent to: " + clients.get(indexes.get(i)).getClientName());
				}
				catch(Exception e) {}
			}
		}
		
		public void updateAllClients(String message) {
			for(int i = 0; i < clients.size(); i++) {
				ClientThread t = clients.get(i);
				try {
					List<String> recipient = new ArrayList<>();

					for (int j = 0; j < clients.size(); j++) {
						recipient.add(clients.get(j).getClientName());
					}
					Message toSend = new Message("JOIN", "SERVER", message, recipient);

					t.out.writeObject(toSend);
					System.out.println("MESSAGE: " + message + " sent to all.");
				}
				catch(Exception e) {}
			}
		}

		public void announceToEveryone (String message) {
			for(int i = 0; i < clients.size(); i++) {
				ClientThread t = clients.get(i);
				try {
					List<String> recipient = new ArrayList<>();

					for (int j = 0; j < clients.size(); j++) {
						recipient.add(clients.get(j).getClientName());
					}
					Message toSend = new Message("GROUP", "SERVER", message, recipient);

					t.out.writeObject(toSend);
					System.out.println("MESSAGE: " + message + " announced to all.");
				}
				catch(Exception e) {}
			}
		}

		public void run(){

			try {
				in = new ObjectInputStream(connection.getInputStream());
				out = new ObjectOutputStream(connection.getOutputStream());
				connection.setTcpNoDelay(true);
			}
			catch(Exception e) {
				System.out.println("Streams not open");
			}

//			updateAllClients("NEW USER JOINED THE SERVER");

			while(true) {
				try {
					Message data = (Message) in.readObject();

					System.out.println("Message received of type: " + data.getType());

					if (data.getType().equals("MESSAGE")) {

						// Needs to send a message
						String dataContent = data.getContent();
						String MessageSender = data.getSender();
						List<String> MessageRecipientNames = data.getRecipients();

						callback.accept("client: " + count + " sent: " + dataContent);
						Message message = new Message("MESSAGE", MessageSender, dataContent, MessageRecipientNames);
						System.out.println("Sender name received from client: " + MessageSender);
						updateSpecificClients(message);
					}
					else if (data.getType().equals("JOIN")) {
						System.out.println("JOIN ENTERED");

						// New user joined
						setClientName(data.getSender());
						if (!clientName.equals("placeholderName")) {
							callback.accept("client: " + count + " named themselves: " + data.getSender());
							updateAllClients("NEW USER JOINED THE SERVER");
							updateAllClients(data.getSender());
						}
					} else if (data.getType().equals("GROUP")) {
						System.out.println("GROUP ENTERED");

						String dataContent = data.getContent();
						String MessageSender = data.getSender();

						callback.accept("client: " + count + " announced: " + dataContent + "to all");
						String messageToSend = MessageSender + " announced: " + dataContent;
						announceToEveryone(messageToSend);
					}

				}
				catch(Exception e) {
					callback.accept("OOOOPPs...Something wrong with the socket from client: " + count + "....closing down!");
					e.printStackTrace();
					updateAllClients("Client #"+count+" has left the server!");
					clients.remove(this);
					break;
				}
			}
		}//end of run

	}//end of client thread
}
