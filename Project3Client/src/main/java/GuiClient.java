import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.compare;

public class GuiClient extends Application {
	ListView<String> messageList = new ListView<>();
	ListView<String> clientList = new ListView<>();
	String clientName;
	Client clientConnection;
	ObservableList<String> messages = FXCollections.observableArrayList();
	ObservableList<String> dropdownElements = FXCollections.observableArrayList();


	int usernameAlreadyExists = 0;

	private Scene welcomeScreen, messagingScreen;

	@Override
	public void start(Stage primaryStage) throws Exception {

		// Prep

		clientConnection = new Client(data -> {
			Platform.runLater(() -> {

				Message messageReceived = (Message) data;

				if (messageReceived.getType().equals("JOIN")) {

					// Username already exists
					if (messageReceived.getContent().equals("USERNAME ALREADY EXISTS")) {

						Alert alert = new Alert(Alert.AlertType.ERROR);
						alert.setTitle("Error");
						alert.setHeaderText("Username already exists");
						alert.setContentText("Please pick a new name before proceeding.");
						alert.showAndWait();
					}
					// New User Created
					else {
						messageList.getItems().add(messageReceived.getSender() + ": " + messageReceived.getContent());
						clientList.getItems().add((messageReceived.getContent()).toString());

						if (!(messageReceived.getContent()).toString().equals("NEW USER JOINED THE SERVER")) {
							dropdownElements.add((messageReceived.getContent()).toString());

							// fetch all old clients
							dropdownElements.clear();
							for (String t : messageReceived.getRecipients()) {
								dropdownElements.add(t);
								System.out.println(t + "added to the dropdown box");
							}

							primaryStage.setScene(messagingScreen);
						}
						else {
							System.out.println(messageReceived.getContent());

						}
					}
				}
				else if (messageReceived.getType().equals("MESSAGE")) {

					System.out.println("Message received from: " + messageReceived.getSender() + " which is: " + messageReceived.getContent());

					messageList.getItems().add(messageReceived.getSender() + ": " + messageReceived.getContent());
					messages.add(messageReceived.getSender() + ": " + messageReceived.getContent());
					System.out.println(messages.getFirst());

				}
				else if (messageReceived.getType().equals("GROUP")) {

					System.out.println("Message received from: " + messageReceived.getSender() + " which is: " + messageReceived.getContent());

					messageList.getItems().add(messageReceived.getContent().toString());
					messages.add(messageReceived.getContent().toString());
					System.out.println(messages.getFirst());
				}
			});
		});

		clientConnection.start();
		primaryStage.setTitle("Chat Client");

		// -----------------------------------------------------------

		// Welcome Screen

		Label welcomeLabel = new Label("Welcome to CHAT-BOX!");
		Label promptLabel = new Label("Set your username before entering");
		TextField usernameTextField = new TextField();
		Button submitButton = new Button("Submit");

		welcomeLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #CD5C5C;");
		promptLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #F08080;");
		usernameTextField.setStyle("-fx-background-color: #D6DBDF; -fx-text-fill: #1C2833;");
		submitButton.setStyle("-fx-background-color: #CD5C5C; -fx-text-fill: #FFFFFF;");

		submitButton.setOnAction(e -> {

			System.out.println("variable name = " + usernameAlreadyExists);
			if (usernameAlreadyExists == 1) {
				usernameTextField.clear();
			}

			if (usernameTextField.getText().isEmpty()) {

				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Username Required");
				alert.setContentText("Please enter a username before proceeding.");
				alert.showAndWait();
			}
			else {

				clientConnection.giveUsername(usernameTextField.getText());
				clientName = usernameTextField.getText();
			}
		});

		HBox inputBox = new HBox(10, usernameTextField, submitButton);
		inputBox.setAlignment(Pos.CENTER);


		VBox rootWelcomeScreen = new VBox(20, welcomeLabel, promptLabel, inputBox);
		rootWelcomeScreen.setAlignment(Pos.CENTER);
		rootWelcomeScreen.setStyle("-fx-background-color: #1C2833;");

		welcomeScreen = new Scene(rootWelcomeScreen, 500, 700);

		// -----------------------------------------------------------

		// Messaging Screen

		BorderPane rootMessagingScreen = new BorderPane();

		// Welcome message
		Label welcomeTextField = new Label("Your CHAT-BOX");
		rootMessagingScreen.setTop(welcomeTextField);

		// Message textField, recipient selection and send button
		HBox messageBox = new HBox(20);
		TextField messageArea = new TextField();
		messageArea.setPromptText("Enter your message here...");
		Button sendButton = new Button("Send");
		ComboBox<String> dropdownBox = new ComboBox<>(dropdownElements);
		dropdownBox.setPromptText("Select recipient...");
		Button sendToAll = new Button("announce"); // announces the message to every online user
		messageBox.getChildren().addAll(messageArea, sendButton, dropdownBox, sendToAll);
		rootMessagingScreen.setBottom(messageBox);

		sendButton.setOnAction(e -> {

			List<String> recipients = new ArrayList<>();

			if (dropdownBox.getValue() != null) {  // Check if a recipient is selected
				recipients.add(dropdownBox.getValue());
				Message messageToSend = new Message("MESSAGE", clientName, messageArea.getText(), recipients);
				clientConnection.send(messageToSend);
				messageArea.clear();
			}
			else {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Select a recipient");
				alert.setContentText("Please select one before proceeding.");
				alert.showAndWait();
			}
		});

		sendToAll.setOnAction(e -> {

			List<String> recipients = new ArrayList<>();
			recipients.add("ALL");
			Message messageToSend = new Message("GROUP", clientName, messageArea.getText(), recipients);
			clientConnection.send(messageToSend);
			messageArea.clear();

		});


		// Message History section
		ListView<String> messageListView = new ListView<>(messages);
		BorderPane messageListPane = new BorderPane();
		messageListPane.setCenter(messageList);
		rootMessagingScreen.setCenter(messageListPane);


		welcomeTextField.setStyle("-fx-font-size: 18px; -fx-text-fill: #F08080; -fx-background-color: #D6DBDF;");
		messageArea.setStyle("-fx-background-color: #D6DBDF; -fx-text-fill: #1C2833;");
		sendButton.setStyle("-fx-background-color: #CD5C5C; -fx-text-fill: #FFFFFF;");
		sendToAll.setStyle("-fx-background-color: #CD5C5C; -fx-text-fill: #FFFFFF;");
		dropdownBox.setStyle("-fx-background-color: #F08080; -fx-text-fill: #1C2833;");
		messageListPane.setStyle("-fx-background-color: #E5E5E5; -fx-text-fill: #444343;");
		messageBox.setStyle("-fx-background-color: #1C2833;");
		rootMessagingScreen.setStyle("-fx-background-color: #1C2833;");
		messageListView.setStyle("-fx-background-color: #D6DBDF; -fx-text-fill: #1C2833;");


		messagingScreen = new Scene(rootMessagingScreen, 500, 700);

		// -----------------------------------------------------------

		primaryStage.setTitle("Chatbox");
		primaryStage.setScene(welcomeScreen);
		primaryStage.show();

		primaryStage.setOnCloseRequest(event -> {
			Platform.exit();
			System.exit(0);
		});
	}

	public static void main(String[] args) {
		launch(args);
	}
}
