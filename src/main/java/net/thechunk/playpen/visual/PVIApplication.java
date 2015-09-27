package net.thechunk.playpen.visual;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.thechunk.playpen.coordinator.VMShutdownThread;
import net.thechunk.playpen.networking.AbstractTransactionListener;
import net.thechunk.playpen.networking.TransactionInfo;
import net.thechunk.playpen.networking.TransactionManager;
import net.thechunk.playpen.visual.controller.ProvisionDialogController;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@Log4j2
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
    private PropertiesConfiguration config;

    @Getter
    boolean closing = false;

    public PVIApplication() {
        super();
        instance = this;
    }

    @Override
    public void start(Stage primaryStage) {
        File configFile = new File(System.getProperty("user.home") + "/.playpenvi");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                showExceptionDialog("Encountered Exception", "Unable to create new configuration file, exiting!", e);
                quit();
            }
        }

        setUserAgentStylesheet(STYLESHEET_MODENA);

        try {
            config = new PropertiesConfiguration(configFile);
        } catch (ConfigurationException e) {
            showExceptionDialog("Encountered Exception", "Unable to load preferences, exiting!", e);
            quit();
        }

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

    public void showTransactionDialog(String processName, TransactionInfo info, Runnable complete) {
        if (TransactionManager.get().getInfo(info.getId()) == null) {
            log.warn("Tried to show processing dialog for invalid transaction " + info.getId());
            return;
        }

        Parent root;
        try {
            root = FXMLLoader.load(getClass().getClassLoader().getResource("ui/ProcessDialog.fxml"));
        } catch (IOException e) {
            showExceptionDialog("Exception Encountered", "Unable to display process dialog", e);
            return;
        }

        Scene scene = new Scene(root);
        Stage stage = new Stage(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.setTitle("Waiting for Process");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnCloseRequest(event -> event.consume());
        stage.setResizable(false);
        stage.setX(primaryStage.getX() + primaryStage.getWidth() / 2d);
        stage.setY(primaryStage.getY() + primaryStage.getHeight() / 2d);

        Text processText = (Text) scene.lookup("#processText");
        Text idText = (Text) scene.lookup("#idText");
        ProgressIndicator progress = (ProgressIndicator) scene.lookup("#progressIndicator");

        processText.setText(processName != null ? processName : "Unknown Process");
        idText.setText(info.getId());
        progress.setProgress(-1);

        info.setHandler(new AbstractTransactionListener() {
            @Override
            public void onTransactionComplete(TransactionManager tm, TransactionInfo info) {
                log.info("Transaction " + info.getId() + " completed.");
                Platform.runLater(() -> {
                    progress.setProgress(1);
                    PVIClient.get().getScheduler().schedule(() -> Platform.runLater(() -> {
                        stage.close();
                        if (complete != null)
                            complete.run();
                    }), 800, TimeUnit.MILLISECONDS);
                });
            }

            @Override
            public void onTransactionCancel(TransactionManager tm, TransactionInfo info) {
                log.warn("Transaction " + info.getId() + " was canceled.");
                Platform.runLater(() -> {
                    stage.close();
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Process Canceled");
                    alert.setHeaderText("Tranasction " + info.getId() + " was canceled.");
                    alert.setContentText("This may be due to the transaction timing out or simply not succeeding.");
                    alert.showAndWait();
                });
            }
        });

        stage.showAndWait();
    }

    public void showProvisionDialog(String packageName, String packageVersion) {
        Parent root = null;
        ProvisionDialogController controller = null;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("ui/ProvisionDialog.fxml"));
            loader.setBuilderFactory(new JavaFXBuilderFactory());
            root = loader.load();

            controller = loader.getController();
        } catch (IOException e) {
            PVIApplication.get().showExceptionDialog("Exception Encountered", "Unable to setup workspace", e);
            PVIApplication.get().quit();
            return;
        }

        Scene scene = new Scene(root);
        Stage stage = new Stage(StageStyle.DECORATED);
        controller.setStage(stage);

        stage.setScene(scene);
        stage.setTitle("Provision");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(true);
        stage.setX(primaryStage.getX() + primaryStage.getWidth() / 2d);
        stage.setY(primaryStage.getY() + primaryStage.getHeight() / 2d);

        controller.setPackage(packageName, packageVersion);
        stage.showAndWait();
    }
}
