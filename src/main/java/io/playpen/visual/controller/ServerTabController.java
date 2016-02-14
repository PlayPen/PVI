package io.playpen.visual.controller;

import io.playpen.core.networking.TransactionInfo;
import io.playpen.core.protocol.Coordinator;
import io.playpen.visual.PVIApplication;
import io.playpen.visual.PVIClient;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.net.URL;
import java.util.ResourceBundle;

@Log4j2
public class ServerTabController implements Initializable {
    @FXML
    TextField uuidField;

    @FXML
    TextField nameField;

    @FXML
    TextField packageField;

    @FXML
    TextField coordinatorField;

    @FXML
    TableView<PropertyValue> propertyTable;

    @FXML
    TableColumn<PropertyValue, String> propertyColumn;

    @FXML
    TableColumn<PropertyValue, String> valueColumn;

    @FXML
    TextArea consoleArea;

    @FXML
    TextField inputField;

    @FXML
    Button sendButton;

    @FXML
    Button attachButton;

    @Getter
    @Setter
    private Tab tab;

    @Getter
    private Coordinator.Server server;

    @Getter
    private Coordinator.LocalCoordinator coordinator;

    @Getter
    private String consoleId = null;

    @Getter
    @Setter
    private String transactionId = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        propertyColumn.setCellValueFactory(new PropertyValueFactory<PropertyValue, String>("name"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<PropertyValue, String>("value"));

        writeToConsole("Click \"Attach\" to attach to this console.");

        consoleArea.textProperty().addListener((observable, oldValue, newValue) -> {
            consoleArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void setServer(Coordinator.LocalCoordinator coordinator, Coordinator.Server server) {
        this.server = server;
        this.coordinator = coordinator;

        uuidField.setText(server.getUuid());
        nameField.setText(server.hasName() ? server.getName() : "");
        packageField.setText(server.getP3().getId() + " @ " + server.getP3().getVersion());
        coordinatorField.setText(coordinator.hasName() ? coordinator.getName() : coordinator.getUuid());

        final ObservableList<PropertyValue> properties = FXCollections.observableArrayList();
        for (Coordinator.Property property : server.getPropertiesList()) {
            properties.add(new PropertyValue(property.getName(), property.getValue()));
        }

        propertyTable.setItems(properties);
    }

    // Doesn't actually attach the console, it just sets up the UI and id stuff
    public void attach(String consoleId) throws Exception {
        if (this.consoleId != null) {
            throw new Exception("Trying to call attach() when we already have a console id!");
        }

        this.consoleId = consoleId;

        Platform.runLater(() -> {
            inputField.setDisable(false);
            sendButton.setDisable(false);
            attachButton.setText("Detach");
            attachButton.setDisable(false);

            writeToConsole("Attached to console " + consoleId);
        });
    }

    public void failAttach() {
        this.consoleId = null;

        Platform.runLater(() -> {
            inputField.setDisable(true);
            sendButton.setDisable(true);
            attachButton.setText("Attach");
            attachButton.setDisable(false);

            writeToConsole("Failed to attach to console");
        });
    }

    // Doesn't actually detach, just does ui and id stuff
    public void detach() {
        this.consoleId = null;

        Platform.runLater(() -> {
            inputField.setDisable(true);
            sendButton.setDisable(true);
            attachButton.setText("Attach");
            attachButton.setDisable(false);

            writeToConsole("Detached!");
        });
    }

    public void writeToConsole(String str) {
        consoleArea.appendText(str + "\n");
    }

    @FXML
    protected void handleAttachButtonPressed(ActionEvent event) {
        attachButton.setDisable(true);
        if (this.consoleId == null) {
            TransactionInfo info = PVIClient.get().sendAttachConsole(coordinator.getUuid(), server.getUuid());
            if (info == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Unable to send console attach request.");
                alert.showAndWait();
            }
            else {
                transactionId = info.getId();
                PVIApplication.get().showTransactionDialog("Attach Console", info, null);
            }
        }
        else {
            PVIClient.get().sendDetachConsole(this.consoleId);
            detach();
        }
    }

    @FXML
    protected void handleSubmitInput(ActionEvent event) {
        if (inputField.getText().trim().length() == 0)
            return;

        String input = inputField.getText();
        inputField.setText("");

        if (PVIClient.get().sendInput(coordinator.getUuid(), server.getUuid(), input + "\n")) {
            writeToConsole(">> " + input);
        }
        else {
            log.error("Unable to send input to " + coordinator.getUuid() + " server " + server.getUuid());
            writeToConsole("[[ ERROR SENDING INPUT ]]");
        }
    }

    @FXML
    protected void handleDeprovisionButtonPressed(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Are you sure you want to deprovision " + (server.hasName() ? server.getName() : server.getUuid()) + "?");
        alert.setContentText("Forcing will cause the server process to immediately exit.");

        ButtonType deprovision = new ButtonType("Deprovision");
        ButtonType force = new ButtonType("Force");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(deprovision, force, cancel);

        alert.showAndWait().ifPresent(result -> {
            if (result == deprovision) {
                if (PVIClient.get().sendDeprovision(coordinator.getUuid(), server.getUuid(), false)) {
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setHeaderText(null);
                    success.setContentText("Sent deprovision of " + (server.hasName() ? server.getName() : server.getUuid()) + " to network.");
                    success.showAndWait();
                }
                else {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setHeaderText(null);
                    err.setContentText("Unable to send deprovision to network.");
                    err.showAndWait();
                }
            }
            else if(result == force) {
                if (PVIClient.get().sendDeprovision(coordinator.getUuid(), server.getUuid(), true)) {
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setHeaderText(null);
                    success.setContentText("Sent force deprovision of " + (server.hasName() ? server.getName() : server.getUuid()) + " to network.");
                    success.showAndWait();
                }
                else {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setHeaderText(null);
                    err.setContentText("Unable to send force deprovision to network.");
                    err.showAndWait();
                }
            }
        });
    }

    @FXML
    protected void handleFreezeButtonPressed(ActionEvent event) {
        if (PVIClient.get().sendFreezeServer(coordinator.getUuid(), server.getUuid())) {
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setHeaderText(null);
            success.setContentText("Sent freeze of " + (server.hasName() ? server.getName() : server.getUuid()) + " to network.");
            success.showAndWait();
        }
        else {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setHeaderText(null);
            err.setContentText("Unable to send freeze to network.");
            err.showAndWait();
        }
    }

    public static class PropertyValue {
        private final SimpleStringProperty name;
        private final SimpleStringProperty value;

        public PropertyValue(String name, String value) {
            this.name = new SimpleStringProperty(name);
            this.value = new SimpleStringProperty(value);
        }

        public String getName() {
            return name.get();
        }

        public String getValue() {
            return value.get();
        }
    }
}
