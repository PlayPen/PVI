package net.thechunk.playpen.visual.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.thechunk.playpen.networking.TransactionInfo;
import net.thechunk.playpen.protocol.Commands;
import net.thechunk.playpen.protocol.Coordinator;
import net.thechunk.playpen.visual.PPEventListener;
import net.thechunk.playpen.visual.PVIApplication;
import net.thechunk.playpen.visual.PVIClient;

import java.io.IOException;
import java.net.URL;
import java.util.*;

@Log4j2
public class WorkspaceController implements Initializable, PPEventListener {
    @FXML
    TabPane tabPane;

    @FXML
    TreeView<String> coordinatorTree;

    private TreeItem<String> rootNode = new TreeItem<>("Network");

    private Tab consoleTab;

    private Map<String, CoordinatorTabController> coordinatorTabs = new HashMap<>();
    private Map<String, ServerTabController> serverTabs = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (PVIClient.get() == null) {
            PVIApplication.get().showExceptionDialog("Exception Encountered", "PVIClient was not initialized when entering the workspace.", new Exception("PVIClient.get() returned null"));
            PVIApplication.get().quit();
        }

        coordinatorTree.setRoot(rootNode);
        coordinatorTree.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                handleTreeItemDoubleClick(coordinatorTree.getSelectionModel().getSelectedItem());
            }
        });

        try {
            URL url = getClass().getClassLoader().getResource("ui/LogTab.fxml");
            consoleTab = FXMLLoader.load(url);
            consoleTab.setClosable(false);
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

        TransactionInfo info = PVIClient.get().sendListRequest();
        if (info == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Unable to send coordinator list request to network.");
            alert.showAndWait();
        }
        else {
            PVIApplication.get().showTransactionDialog("Network Refresh", info, null);
        }
    }

    private void handleTreeItemDoubleClick(TreeItem<String> item) {
        if (item == null)
            return;

        item.setExpanded(true);

        if (item == rootNode) {
            // TODO: Make this into a tab instead of an alert
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Network Information");
            alert.setHeaderText(null);
            alert.setContentText("Connected to PlayPen Network at " + PVIClient.get().getNetworkIP() + ":"
                    + PVIClient.get().getNetworkPort() + " as " + PVIClient.get().getName());
            alert.showAndWait();
        }
        else if (item instanceof CoordinatorTreeItem) {
            Coordinator.LocalCoordinator coordinator = ((CoordinatorTreeItem)item).getCoordinator();
            if (coordinatorTabs.containsKey(coordinator.getUuid())) {
                tabPane.getSelectionModel().select(coordinatorTabs.get(coordinator.getUuid()).getTab());
            }
            else {
                try {
                    FXMLLoader loader = new FXMLLoader();
                    loader.setLocation(getClass().getClassLoader().getResource("ui/CoordinatorTab.fxml"));
                    loader.setBuilderFactory(new JavaFXBuilderFactory());
                    Tab tab = loader.load();
                    tab.setText(coordinator.hasName() ? coordinator.getName() : coordinator.getUuid());
                    tab.setClosable(true);
                    tab.setOnClosed(event -> {
                        coordinatorTabs.remove(coordinator.getUuid());
                    });
                    tabPane.getTabs().add(tab);
                    tabPane.getSelectionModel().select(tab);

                    CoordinatorTabController controller = loader.getController();
                    controller.setTab(tab);
                    controller.setCoordinator(coordinator);
                    coordinatorTabs.put(coordinator.getUuid(), controller);
                } catch (IOException e) {
                    PVIApplication.get().showExceptionDialog("Exception Encountered", "Unable to open tab for coordinator", e);
                }
            }
        }
        else if(item instanceof ServerTreeItem) {
            ServerTreeItem serverTreeItem = (ServerTreeItem) item;
            Coordinator.LocalCoordinator coordinator = serverTreeItem.getCoordinator();
            Coordinator.Server server = serverTreeItem.getServer();
            if (serverTabs.containsKey(server.getUuid())) {
                tabPane.getSelectionModel().select(serverTabs.get(server.getUuid()).getTab());
            }
            else {
                try {
                    FXMLLoader loader = new FXMLLoader();
                    loader.setLocation(getClass().getClassLoader().getResource("ui/ServerTab.fxml"));
                    loader.setBuilderFactory(new JavaFXBuilderFactory());
                    Tab tab = loader.load();
                    ServerTabController controller = loader.getController();

                    tab.setText(server.hasName() ? server.getName() : server.getUuid());
                    tab.setClosable(true);
                    tab.setOnClosed(event -> {
                        serverTabs.remove(server.getUuid());
                        if (controller.getConsoleId() != null) {
                            PVIClient.get().sendDetachConsole(controller.getConsoleId());
                        }
                    });
                    tabPane.getTabs().add(tab);
                    tabPane.getSelectionModel().select(tab);

                    controller.setTab(tab);
                    controller.setServer(coordinator, server);
                    serverTabs.put(server.getUuid(), controller);
                }
                catch (IOException e) {
                    PVIApplication.get().showExceptionDialog("Exception encountered", "Unable to open tab for server", e);
                }
            }
        }
    }

    @FXML
    protected void handleRefreshButtonPressed(ActionEvent event) {
        TransactionInfo info = PVIClient.get().sendListRequest();

        if (info == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Unable to send coordinator list request to network.");
            alert.showAndWait();
        }
        else {
            PVIApplication.get().showTransactionDialog("Network Refresh", info, null);
        }
    }

    @FXML
    protected void handlePackagesButtonPressed(ActionEvent event) {
        TransactionInfo info = PVIClient.get().sendRequestPackageList();

        if (info == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Unable to send package list request to network.");
            alert.showAndWait();
        }
        else {
            PVIApplication.get().showTransactionDialog("Package List", info, null);
        }
    }

    @Override
    public void receivedListResponse(Commands.C_CoordinatorListResponse response, TransactionInfo info) {
        Platform.runLater(() -> {
            rootNode.getChildren().clear();
            Set<String> coordIds = new HashSet<>();
            Set<String> serverIds = new HashSet<>();
            for (Coordinator.LocalCoordinator coordinator : response.getCoordinatorsList()) {
                rootNode.getChildren().add(new CoordinatorTreeItem(coordinator));
                if (coordinatorTabs.containsKey(coordinator.getUuid())) {
                    CoordinatorTabController controller = coordinatorTabs.get(coordinator.getUuid());
                    controller.getTab().setText(coordinator.hasName() ? coordinator.getName() : coordinator.getUuid());
                    controller.setCoordinator(coordinator);
                }

                coordIds.add(coordinator.getUuid());

                for (Coordinator.Server server : coordinator.getServersList()) {
                    if (serverTabs.containsKey(server.getUuid())) {
                        ServerTabController controller = serverTabs.get(server.getUuid());
                        controller.getTab().setText(server.hasName() ? server.getName() : server.getUuid());
                        controller.setServer(coordinator, server);
                    }

                    serverIds.add(server.getUuid());
                }
            }

            Iterator<String> itr = coordinatorTabs.keySet().iterator();
            while (itr.hasNext()) {
                String uuid = itr.next();
                if (!coordIds.contains(uuid)) {
                    CoordinatorTabController controller = coordinatorTabs.get(uuid);
                    tabPane.getTabs().remove(controller.getTab());
                    itr.remove();
                }
            }

            itr = serverTabs.keySet().iterator();
            while(itr.hasNext()) {
                String uuid = itr.next();
                if (!serverIds.contains(uuid)) {
                    ServerTabController controller = serverTabs.get(uuid);
                    tabPane.getTabs().remove(controller.getTab());
                    itr.remove();
                }
            }

            rootNode.getChildren().sort((a, b) -> a.getValue().compareTo(b.getValue()));
            rootNode.setExpanded(true);
        });
    }

    @Override
    public void receivedConsoleAttach(String consoleId, TransactionInfo info) {
        for (ServerTabController controller : serverTabs.values()) {
            if (Objects.equals(controller.getTransactionId(), info.getId())) {
                controller.setTransactionId(null);
                log.info("Attaching " + consoleId);
                try {
                    controller.attach(consoleId);
                } catch (Exception e) {
                    PVIApplication.get().showExceptionDialog("Exception Encountered", "Unable to attach to console " + consoleId, e);
                }

                return;
            }
        }

        log.warn("Received attach for console we aren't waiting for: " + consoleId);
        PVIClient.get().sendDetachConsole(consoleId);
    }

    @Override
    public void receivedDetachConsole(String consoleId, TransactionInfo info) {
        for (ServerTabController controller : serverTabs.values()) {
            if (Objects.equals(controller.getConsoleId(), consoleId)) {
                controller.detach();
                return;
            }
        }

        log.warn("We aren't listening to console " + consoleId);
    }

    @Override
    public void receivedConsoleMessage(String consoleId, String value, TransactionInfo info) {
        for (ServerTabController controller : serverTabs.values()) {
            if (Objects.equals(controller.getConsoleId(), consoleId)) {
                Platform.runLater(() -> controller.writeToConsole(value));
                return;
            }
        }

        log.warn("We aren't listening to console " + consoleId + ", sending detach");
        PVIClient.get().sendDetachConsole(consoleId);
    }

    @Override
    public void receivedPackageList(Commands.C_PackageList list, TransactionInfo info) {

    }

    private static final class CoordinatorTreeItem extends TreeItem<String> {
        @Getter
        private Coordinator.LocalCoordinator coordinator;

        public CoordinatorTreeItem(Coordinator.LocalCoordinator coordinator) {
            super(coordinator.hasName() ? coordinator.getName() : coordinator.getUuid());
            this.coordinator = coordinator;

            for (Coordinator.Server server : coordinator.getServersList()) {
                this.getChildren().add(new ServerTreeItem(coordinator, server));
            }

            this.getChildren().sort((a, b) -> a.getValue().compareTo(b.getValue()));
        }
    }

    private static final class ServerTreeItem extends TreeItem<String> {
        @Getter
        private Coordinator.Server server;

        @Getter
        private Coordinator.LocalCoordinator coordinator;

        public ServerTreeItem(Coordinator.LocalCoordinator coordinator, Coordinator.Server server) {
            super((server.hasName() ? server.getName() : server.getUuid()) + " (" + server.getP3().getId() + " @ " + server.getP3().getVersion() + ")");
            this.server = server;
            this.coordinator = coordinator;
        }
    }
}
