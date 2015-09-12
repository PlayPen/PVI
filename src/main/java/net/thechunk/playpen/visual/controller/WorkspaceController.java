package net.thechunk.playpen.visual.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import lombok.Getter;
import lombok.Setter;
import net.thechunk.playpen.networking.TransactionInfo;
import net.thechunk.playpen.protocol.Commands;
import net.thechunk.playpen.protocol.Coordinator;
import net.thechunk.playpen.visual.PPEventListener;
import net.thechunk.playpen.visual.PVIApplication;
import net.thechunk.playpen.visual.PVIClient;

import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.ResourceBundle;

public class WorkspaceController implements Initializable, PPEventListener {
    @FXML
    TabPane tabPane;

    @FXML
    TreeView<String> coordinatorTree;

    private Tab consoleTab;

    private TreeItem<String> rootNode = new TreeItem<>("Network");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (PVIClient.get() == null) {
            PVIApplication.get().showExceptionDialog("Exception Encountered", "PVIClient was not initialized when entering the workspace.", new Exception("PVIClient.get() returned null"));
            PVIApplication.get().quit();
        }

        coordinatorTree.setRoot(rootNode);

        try {
            consoleTab = (Tab) FXMLLoader.load(getClass().getClassLoader().getResource("ui/Log.fxml"));
            tabPane.getTabs().add(consoleTab);
            tabPane.getSelectionModel().select(consoleTab);
        } catch (IOException e) {
            PVIApplication.get().showExceptionDialog("Exception Encountered", "Unable to setup workspace", e);
            PVIApplication.get().quit();
            return;
        }

        PVIClient.get().addEventListener(this);

        if (!PVIClient.get().sendSync()) {
            PVIApplication.get().showExceptionDialog("Exception Encountered", "Unable to send SYNC to network", new Exception());
            PVIApplication.get().quit();
            return;
        }

        PVIClient.get().sendListRequest();
        //PVIClient.get().getScheduler().scheduleAtFixedRate(() -> PVIClient.get().sendListRequest(), 1, 30, TimeUnit.SECONDS);
    }

    @FXML
    protected void handleRefreshButtonPressed(ActionEvent event) {
        PVIClient.get().sendListRequest();
    }

    @Override
    public void receivedListResponse(Commands.C_CoordinatorListResponse response, TransactionInfo info) {
        rootNode.getChildren().clear();
        for (Coordinator.LocalCoordinator coordinator : response.getCoordinatorsList()) {
            rootNode.getChildren().add(new CoordinatorTreeItem(coordinator));
        }

        rootNode.getChildren().sort((a, b) -> a.getValue().compareTo(b.getValue()));
        rootNode.setExpanded(true);
    }

    private static final class CoordinatorTreeItem extends TreeItem<String> {
        @Getter
        private Coordinator.LocalCoordinator coordinator;

        public CoordinatorTreeItem(Coordinator.LocalCoordinator coordinator) {
            super(coordinator.hasName() ? coordinator.getName() : coordinator.getUuid());
            this.coordinator = coordinator;

            for (Coordinator.Server server : coordinator.getServersList()) {
                this.getChildren().add(new ServerTreeItem(server));
            }

            this.getChildren().sort((a, b) -> a.getValue().compareTo(b.getValue()));
        }
    }

    private static final class ServerTreeItem extends TreeItem<String> {
        @Getter
        private Coordinator.Server server;

        public ServerTreeItem(Coordinator.Server server) {
            super((server.hasName() ? server.getName() : server.getUuid()) + " (" + server.getP3().getId() + " @ " + server.getP3().getVersion() + ")");
            this.server = server;
        }
    }
}
