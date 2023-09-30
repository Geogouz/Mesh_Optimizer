// remember to include: --module-path ${PATH_TO_FX} --add-modules javafx.controls,javafx.fxml
package Core;

import Core.Controllers.Main_Controller;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryWindow) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/Views/Home.fxml"));
        Parent root = fxmlLoader.load();

        Main_Controller main_controller = fxmlLoader.getController();
        main_controller.primaryWindow = primaryWindow;

        primaryWindow.initStyle(StageStyle.UNDECORATED); // Set the primaryWindow to be borderless

        primaryWindow.getIcons().add(new Image("/Graphics/mo_icon.png"));

        Scene scene = new Scene(root, Color.WHITE);
        primaryWindow.setScene(scene); // Load the scene
        primaryWindow.show();

        primaryWindow.setOnCloseRequest(e -> {
            // Override the exit functionality completely..
            e.consume();
            main_controller.closeProgram();
        });
    }
}
