package Core;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ConfirmBox {

    static boolean answer;

    public static boolean display(String title, int width, int height, boolean show_btn_1, boolean show_btn_2, String message) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);
        window.setMinWidth(width);
        window.setMinHeight(height);

        Label label_1 = new Label();
        label_1.setText(message);
        label_1.setTextAlignment(TextAlignment.CENTER);

        // Create two buttons
        Button yesButton = new Button("Ok");
        yesButton.setVisible(show_btn_1);
        Button noButton = new Button("Cancel");
        noButton.setVisible(show_btn_2);

        yesButton.setOnAction(e -> {
            answer = true;
            window.close();
        });

        noButton.setOnAction(e -> {
            answer = false;
            window.close();
        });

        HBox buttons_layout = new HBox(10);
        buttons_layout.getChildren().addAll(yesButton, noButton);
        buttons_layout.setAlignment(Pos.CENTER);

        VBox main_layout = new VBox(10);
        main_layout.getChildren().addAll(label_1, buttons_layout);
        main_layout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(main_layout);
        window.setScene(scene);
        window.showAndWait();

        return answer;
    }

}
