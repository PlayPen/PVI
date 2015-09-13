package net.thechunk.playpen.visual.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import lombok.Getter;
import lombok.Setter;
import net.thechunk.playpen.protocol.Coordinator;

import java.net.URL;
import java.util.ResourceBundle;

public class ServerTabController implements Initializable {
    @FXML
    TextField uuidField;

    @FXML
    TextField nameField;

    @FXML
    TextField packageField;

    @FXML
    TextField coordinatorField;

    @Getter
    @Setter
    private Tab tab;

    @Getter
    private Coordinator.Server server;

    @Getter
    private Coordinator.LocalCoordinator coordinator;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TODO: property table initialization
    }

    public void setServer(Coordinator.LocalCoordinator coordinator, Coordinator.Server server) {
        this.server = server;
        this.coordinator = coordinator;

        uuidField.setText(server.getUuid());
        nameField.setText(server.hasName() ? server.getName() : "");
        packageField.setText(server.getP3().getId() + " @ " + server.getP3().getVersion());
        coordinatorField.setText(coordinator.getUuid());
    }
}
