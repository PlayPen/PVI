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
import net.thechunk.playpen.protocol.Coordinator;
import net.thechunk.playpen.visual.PVIApplication;
import net.thechunk.playpen.visual.PVIClient;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

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
        if (coordinator.hasName())
            nameField.setText(coordinator.getName());

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

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Shutdown is not currently implemented.");
            alert.showAndWait();
        }
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