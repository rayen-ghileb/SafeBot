package sf.project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sf.project.controllers.ChatController;

import java.io.IOException;

public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    private ChatController chatController;

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/sf/project/views/chat.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);
        chatController = loader.getController();

        scene.getStylesheets().add(getClass().getResource("/sf/project/views/style.css").toExternalForm());

        primaryStage.setTitle("SafeBot - Mental Health Support");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            if (chatController != null) {
                chatController.shutdown();
            }
        });
        primaryStage.show();
        logger.info("SafeBot application started");
    }

    @Override
    public void stop() {
        logger.info("SafeBot application stopping");
        if (chatController != null) {
            chatController.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
