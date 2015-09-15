package net.thechunk.playpen.visual.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.thechunk.playpen.networking.TransactionInfo;
import net.thechunk.playpen.protocol.Coordinator;
import net.thechunk.playpen.visual.PVIApplication;
import net.thechunk.playpen.visual.PVIClient;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public class CoordinatorTabController implements Initializable {
    @FXML
    TextField uuidField;

    @FXML
    TextField nameField;

    @FXML
    CheckBox enabledField;

    @FXML
    TableView<ResourceValue> resourcesTable;

    @FXML
    TableColumn<ResourceValue, String> resourceColumn;

    @FXML
    TableColumn<ResourceValue, Integer> valueColumn;

    @FXML
    ListView<String> attributesList;

    @FXML
    Text serversText;

    @Getter
    @Setter
    private Tab tab;

    @Getter
    private Coordinator.LocalCoordinator coordinator;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        resourceColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
    }

    public void setCoordinator(Coordinator.LocalCoordinator coordinator) {
        this.coordinator = coordinator;

        uuidField.setText(coordinator.getUuid());
        nameField.setText(coordinator.hasName() ? coordinator.getName() : "");
        enabledField.setSelected(coordinator.getEnabled());

        final ObservableList<ResourceValue> resources = FXCollections.observableArrayList();
        for (Coordinator.Resource resource : coordinator.getResourcesList()) {
            resources.add(new ResourceValue(resource.getName(), resource.getValue()));
        }

        resourcesTable.setItems(resources);

        final ObservableList<String> attributes = FXCollections.observableArrayList(coordinator.getAttributesList());
        attributesList.setItems(attributes);

        serversText.setText(coordinator.getServersList().size() + " servers");
    }

    @FXML
    protected void handleShutdownButtonPressed(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Shutdown Coordinator");
        alert.setHeaderText("Are you sure you want to shutdown coordinator " + (coordinator.hasName() ? coordinator.getName() : coordinator.getUuid()) + "?");
        alert.setContentText("Shutting down a coordinator will tell it to disconnect from the network and safely " +
                "shutdown all servers, before stopping itself.");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                if (PVIClient.get().sendShutdown(coordinator.getUuid())) {
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Success");
                    info.setHeaderText(null);
                    info.setContentText("Sent shutdown of " + coordinator.getUuid() + " to network.");
                    info.showAndWait();

                    PVIClient.get().getScheduler().schedule(() -> PVIClient.get().sendListRequest(), 5, TimeUnit.SECONDS);
                }
                else {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Error");
                    err.setHeaderText(null);
                    err.setContentText("Unable to send shutdown to network. Check log for details.");
                    err.showAndWait();
                }
            }
        });
    }

    public static class ResourceValue {
        private final SimpleStringProperty name;
        private final SimpleIntegerProperty value;

        public ResourceValue(String name, int value) {
            this.name = new SimpleStringProperty(name);
            this.value = new SimpleIntegerProperty(value);
        }

        public String getName() {
            return name.get();
        }

        public int getValue() {
            return value.get();
        }
    }
}