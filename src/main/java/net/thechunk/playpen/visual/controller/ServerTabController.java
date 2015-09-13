package net.thechunk.playpen.visual.controller;

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
import net.thechunk.playpen.networking.TransactionInfo;
import net.thechunk.playpen.protocol.Coordinator;
import net.thechunk.playpen.visual.PVIApplication;
import net.thechunk.playpen.visual.PVIClient;

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
        consoleArea.setText(consoleArea.getText() + str + "\n");
        consoleArea.setScrollTop(Double.MAX_VALUE);
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
                PVIApplication.get().showTransactionDialog("Attach Console", info);
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
