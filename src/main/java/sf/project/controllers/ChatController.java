package sf.project.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sf.project.services.MentalHealthChatService;

import java.net.URL;
import java.util.ResourceBundle;

public class ChatController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @FXML private VBox chatContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private HBox typingIndicator;
    @FXML private Label typingLabel;

    private MentalHealthChatService chatService;
    private Timeline typingTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chatService = new MentalHealthChatService();
        chatService.startConversation();

        typingIndicator.setVisible(false);
        typingIndicator.setManaged(false);

        messageInput.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                handleSendMessage();
            }
        });

        chatContainer.heightProperty().addListener((obs, oldVal, newVal) ->
                chatScrollPane.setVvalue(1.0));

        addBotMessage("Hi! I'm Connor, your mental health support companion. How are you feeling today?");
    }

    @FXML
    private void handleSendMessage() {
        String userMessage = messageInput.getText().trim();
        if (userMessage.isEmpty()) {
            return;
        }

        messageInput.clear();
        sendButton.setDisable(true);
        addUserMessage(userMessage);
        showTypingIndicator(true);

        chatService.sendMessage(
                userMessage,
                response -> Platform.runLater(() -> {
                    showTypingIndicator(false);
                    addBotMessage(response);
                    sendButton.setDisable(false);
                }),
                throwable -> Platform.runLater(() -> {
                    logger.error("Error sending message", throwable);
                    showTypingIndicator(false);
                    addBotMessage("I'm sorry, I encountered an error. Please try again.");
                    sendButton.setDisable(false);
                })
        );
    }

    private void addUserMessage(String text) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setPadding(new Insets(5, 10, 5, 50));

        Label messageLabel = new Label(text);
        messageLabel.getStyleClass().add("user-message");
        messageLabel.setWrapText(true);
        messageBox.getChildren().add(messageLabel);

        chatContainer.getChildren().add(messageBox);
    }

    private void addBotMessage(String text) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 50, 5, 10));

        Label messageLabel = new Label(text);
        messageLabel.getStyleClass().add("bot-message");
        messageLabel.setWrapText(true);
        messageBox.getChildren().add(messageLabel);

        chatContainer.getChildren().add(messageBox);
    }

    private void showTypingIndicator(boolean show) {
        typingIndicator.setVisible(show);
        typingIndicator.setManaged(show);

        if (show) {
            if (typingTimeline != null) {
                typingTimeline.stop();
            }
            String[] dots = {"", ".", "..", "..."};
            final int[] counter = {0};
            typingTimeline = new Timeline(new KeyFrame(Duration.millis(400), e -> {
                typingLabel.setText("Connor is typing" + dots[counter[0] % 4]);
                counter[0]++;
            }));
            typingTimeline.setCycleCount(Timeline.INDEFINITE);
            typingTimeline.play();
        } else {
            if (typingTimeline != null) {
                typingTimeline.stop();
                typingTimeline = null;
            }
        }
    }

    public void shutdown() {
        if (chatService != null) {
            chatService.shutdown();
        }
    }
}
