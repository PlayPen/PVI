package net.thechunk.playpen.visual;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.Getter;
import net.thechunk.playpen.coordinator.VMShutdownThread;

import java.io.PrintWriter;
import java.io.StringWriter;

public class PVIApplication extends Application {
    private static PVIApplication instance = null;

    public static void main(String[] args) {
        launch(args);
    }

    public static PVIApplication get() {
        return instance;
    }

    private Stage primaryStage;

    @Getter
    boolean closing = false;

    public PVIApplication() {
        super();
        instance = this;
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            this.primaryStage = primaryStage;

            Runtime.getRuntime().addShutdownHook(new VMShutdownThread());

            Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("ui/Connect.fxml"));
            Scene scene = new Scene(root);

            primaryStage.setOnCloseRequest(event -> quit());

            primaryStage.setTitle("PVI Client");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            showExceptionDialog("Exception Encountered", "Unable to start PVI.", e);
            quit();
        }
    }

    public void openWorkspace() {
        try {
            Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("ui/Workspace.fxml"));
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
        } catch (Exception e) {
            showExceptionDialog("Exception Encountered", "Unable to open main workspace", e);
            quit();
        }
    }

    public void quit() {
        closing = true;

        if (PVIClient.get() != null)
            PVIClient.get().stop();

        System.exit(0);
    }

    public void showExceptionDialog(String title, String text, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(new Label("The exception stacktrace was:"), 0, 0);
        expContent.add(textArea, 0, 1);
        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }
}
