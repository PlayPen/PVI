package io.playpen.visual.controller;

import io.playpen.core.networking.TransactionInfo;
import io.playpen.core.networking.TransactionManager;
import io.playpen.core.protocol.Commands;
import io.playpen.core.protocol.Coordinator;
import io.playpen.visual.PPEventListener;
import io.playpen.visual.PVIApplication;
import io.playpen.visual.PVIClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

@Log4j2
public class WorkspaceController implements Initializable, PPEventListener {
    @FXML
    private TabPane tabPane;

    @FXML
    private TreeView<String> coordinatorTree;

    @FXML
    private TextField searchField;

    private TreeItem<String> rootNode = new TreeItem<>("Network");

    private PackagesTabController packagesTab;
    private UploadPackageTab uploadPackageTab;

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
            assert url != null;
            Tab consoleTab = FXMLLoader.load(url);
            consoleTab.setClosable(false);
            tabPane.getTabs().add(consoleTab);

            FXMLLoader loader = new FXMLLoader();
            FXMLLoader loaderTwo = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("ui/PackagesTab.fxml"));
            loader.setBuilderFactory(new JavaFXBuilderFactory());
            Tab tab = loader.load();
            tab.setClosable(false);
            loaderTwo.setLocation(getClass().getClassLoader().getResource("ui/UploadPackageTab.fxml"));
            Tab uploadTab = loaderTwo.load();
            uploadTab.setClosable(false);

            tabPane.getTabs().add(tab);
            tabPane.getTabs().addAll(uploadTab);

            packagesTab = loader.getController();
            packagesTab.setTab(tab);

            uploadPackageTab = loaderTwo.getController();
            uploadPackageTab.setTab(uploadTab);

            tabPane.getSelectionModel().select(consoleTab);

            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                coordinatorTree.getSelectionModel().clearSelection();
                if (newValue == null || newValue.trim().isEmpty()) {
                    for (TreeItem<String> node : rootNode.getChildren()) {
                        node.setExpanded(false);
                    }
                } else {
                    String searchStr = newValue.trim().toLowerCase();
                    for (TreeItem<String> node : rootNode.getChildren()) {
                        node.setExpanded(false);
                        for (TreeItem<String> child : node.getChildren()) {
                            if (child.getValue().toLowerCase().contains(searchStr)) {
                                node.setExpanded(true);
                                coordinatorTree.getSelectionModel().select(child);
                            }
                        }
                    }
                }
            });
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
        } else {
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
        } else if (item instanceof CoordinatorTreeItem) {
            Coordinator.LocalCoordinator coordinator = ((CoordinatorTreeItem) item).getCoordinator();
            if (coordinatorTabs.containsKey(coordinator.getUuid())) {
                tabPane.getSelectionModel().select(coordinatorTabs.get(coordinator.getUuid()).getTab());
            } else {
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
        } else if (item instanceof ServerTreeItem) {
            ServerTreeItem serverTreeItem = (ServerTreeItem) item;
            Coordinator.LocalCoordinator coordinator = serverTreeItem.getCoordinator();
            Coordinator.Server server = serverTreeItem.getServer();
            if (serverTabs.containsKey(server.getUuid())) {
                tabPane.getSelectionModel().select(serverTabs.get(server.getUuid()).getTab());
            } else {
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
                } catch (IOException e) {
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
        } else {
            PVIApplication.get().showTransactionDialog("Network Refresh", info, null);
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

            rootNode.getChildren().sort(Comparator.comparing(TreeItem::getValue));
            rootNode.setExpanded(true);

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
            while (itr.hasNext()) {
                String uuid = itr.next();
                if (!serverIds.contains(uuid)) {
                    ServerTabController controller = serverTabs.get(uuid);
                    tabPane.getTabs().remove(controller.getTab());
                    itr.remove();
                }
            }
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
    public void receivedConsoleAttachFail(TransactionInfo info) {
        for (ServerTabController controller : serverTabs.values()) {
            if (Objects.equals(controller.getTransactionId(), info.getId())) {
                controller.setTransactionId(null);
                log.info("Attach failed for " + controller.getServer().getName());
                controller.failAttach();
                return;
            }
        }
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
        Platform.runLater(() -> packagesTab.updatePackages(list));
    }

    @Override
    public void receivedProvisionResponse(Commands.C_ProvisionResponse response, TransactionInfo info) {
        if (response.getOk()) {
            // uuid -> name resolution for coordinator
            String coord = response.getCoordinatorId();
            for (TreeItem<String> item : coordinatorTree.getRoot().getChildren()) {
                if (item instanceof CoordinatorTreeItem) {
                    CoordinatorTreeItem coordItem = (CoordinatorTreeItem) item;
                    if (coordItem.getCoordinator().getUuid().equals(response.getCoordinatorId())) {
                        coord = coordItem.getCoordinator().hasName() ? coordItem.getCoordinator().getName() : coord;
                        break;
                    }
                }
            }

            final String c = coord;

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Server Provisioned");
                alert.setHeaderText(null);
                alert.setContentText("Successfully provisioned server with uuid " + response.getServerId() + " on coordinator " + c);
                alert.showAndWait();
            });
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Unable to provision server.");
                alert.setContentText("This could be for numerous reasons. Check the NC console for more information.");
                alert.showAndWait();
            });
        }
    }

    @Override
    public void receivedAccessDenied(Commands.C_AccessDenied message, TransactionInfo info) {
        TransactionManager.get().cancel(message.getTid(), true);
        Platform.runLater(() -> PVIApplication.get().showExceptionDialog("Access Denied",
                "Access was denied by the network for an operation",
                new Exception(message.getResult())));
    }

    private static final class CoordinatorTreeItem extends TreeItem<String> {
        @Getter
        private Coordinator.LocalCoordinator coordinator;

        CoordinatorTreeItem(Coordinator.LocalCoordinator coordinator) {
            super(coordinator.hasName() ? coordinator.getName() : coordinator.getUuid());
            this.coordinator = coordinator;

            for (Coordinator.Server server : coordinator.getServersList()) {
                this.getChildren().add(new ServerTreeItem(coordinator, server));
            }

            this.getChildren().sort(Comparator.comparing(TreeItem::getValue));
        }
    }

    private static final class ServerTreeItem extends TreeItem<String> {
        @Getter
        private Coordinator.Server server;

        @Getter
        private Coordinator.LocalCoordinator coordinator;

        ServerTreeItem(Coordinator.LocalCoordinator coordinator, Coordinator.Server server) {
            super((server.hasName() ? server.getName() : server.getUuid()) + " (" + server.getP3().getId() + " @ " + server.getP3().getVersion() + ")");
            this.server = server;
            this.coordinator = coordinator;
        }
    }
}
