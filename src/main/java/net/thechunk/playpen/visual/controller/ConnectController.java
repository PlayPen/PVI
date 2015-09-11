package net.thechunk.playpen.visual.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import net.thechunk.playpen.visual.PVIApplication;
import net.thechunk.playpen.visual.PVIClient;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.ResourceBundle;

public class ConnectController implements Initializable {
    @FXML
    TextField nameInput;

    @FXML
    TextField uuidInput;

    @FXML
    TextField keyInput;

    @FXML
    TextField ipInput;

    @FXML
    TextField portInput;

    @FXML
    Button connectButton;

    @FXML
    Text connectText;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // try to get the hostname
        try {
            nameInput.setText("PVI-" + InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            nameInput.setText("PVI-" + new Random().nextInt(9999));
        }
    }

    @FXML
    protected void handleConnectButtonPressed(ActionEvent event) {
        setFormDisable(true);

        if (nameInput.getText().trim().length() == 0) {
            showWarningDialog("Missing name for client.");
            return;
        }

        if (uuidInput.getText().trim().length() == 0) {
            showWarningDialog("Missing UUID for client.");
            return;
        }

        if (keyInput.getText().trim().length() == 0) {
            showWarningDialog("Missing secret key for client.");
            return;
        }

        if (ipInput.getText().trim().length() == 0) {
            showWarningDialog("Missing network ip.");
            return;
        }

        if (portInput.getText().trim().length() == 0) {
            showWarningDialog("Missing network port.");
            return;
        }

        InetAddress ip;
        try {
            ip = InetAddress.getByName(ipInput.getText());
        } catch (UnknownHostException e) {
            PVIApplication.get().showExceptionDialog("Unable to Connect", "Invalid ip address.", e);
            connectText.setText("");
            connectText.setVisible(false);

            setFormDisable(false);
            return;
        }

        int port;
        try {
            port = Integer.parseUnsignedInt(portInput.getText());
        }
        catch(NumberFormatException e) {
            PVIApplication.get().showExceptionDialog("Unable to Connect", "Invalid port number.", e);
            connectText.setText("");
            connectText.setVisible(false);

            setFormDisable(false);
            return;
        }

        connectText.setText("Connecting to " + ip + ":" + port + "...");
        connectText.setVisible(true);

        final PVIClient client = new PVIClient(nameInput.getText(), uuidInput.getText(), keyInput.getText(), ip, port);
        new Thread(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (client.start()) {
                    Platform.runLater(() -> PVIApplication.get().openWorkspace());
                }
                else {
                    client.stop();
                    Platform.runLater(() -> {
                        showErrorDialog("Connection failed. Check ip address and port.");
                    });
                }
                return null;
            }
        }).start();
    }

    private void setFormDisable(boolean disable) {
        nameInput.setDisable(disable);
        uuidInput.setDisable(disable);
        keyInput.setDisable(disable);
        ipInput.setDisable(disable);
        portInput.setDisable(disable);
        connectButton.setDisable(disable);
    }

    private void showWarningDialog(String text) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Unable to Connect");
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();

        connectText.setText("");
        connectText.setVisible(false);

        setFormDisable(false);
    }

    private void showErrorDialog(String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Unable to Connect");
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();

        connectText.setText("");
        connectText.setVisible(false);

        setFormDisable(false);
    }
}
