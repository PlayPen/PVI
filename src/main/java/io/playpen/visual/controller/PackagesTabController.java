package io.playpen.visual.controller;

import io.playpen.core.networking.TransactionInfo;
import io.playpen.core.protocol.Commands;
import io.playpen.visual.PVIApplication;
import io.playpen.visual.PVIClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class PackagesTabController implements Initializable {
    @FXML
    private TreeView<String> packageTree;

    @Getter
    @Setter
    private Tab tab = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    void updatePackages(Commands.C_PackageList list) {
        Map<String, List<PackageVersion>> packages = new HashMap<>();
        list.getPackagesList().forEach(p3 -> {
            if (packages.containsKey(p3.getId())) {
                packages.get(p3.getId()).add(new PackageVersion(p3.getId(), p3.getVersion(), p3.getPromoted()));
            } else {
                List<PackageVersion> versions = new LinkedList<>();
                versions.add(new PackageVersion(p3.getId(), p3.getVersion(), p3.getPromoted()));
                packages.put(p3.getId(), versions);
            }
        });

        TreeItem<String> rootNode = new TreeItem<>("Packages");
        packageTree.setRoot(rootNode);
        packageTree.setShowRoot(false);

        packages.forEach((name, versions) -> {
            rootNode.getChildren().add(new PackageTreeItem(name, versions));
        });

        rootNode.getChildren().sort(Comparator.comparing(TreeItem::getValue));
    }

    @FXML
    protected void handleRefreshButtonPressed(ActionEvent event) {
        TransactionInfo info = PVIClient.get().sendRequestPackageList();

        if (info == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Unable to send package list request to network.");
            alert.showAndWait();
        } else {
            PVIApplication.get().showTransactionDialog("Package List", info, null);
        }
    }

    @FXML
    protected void handleProvisionButtonPressed(ActionEvent event) {
        TreeItem<String> selectedItem = packageTree.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("You must select a package to provision!");
            alert.showAndWait();
            return;
        }

        PackageVersion version = null;
        if (selectedItem instanceof PackageTreeItem) {
            PackageTreeItem item = (PackageTreeItem) selectedItem;
            for (TreeItem<String> child : item.getChildren()) {
                if (!(child instanceof VersionTreeItem)) {
                    PVIApplication.get().showExceptionDialog("This shouldn't happen!", "non-VersionTreeItem found", new Exception());
                    return;
                }

                VersionTreeItem versionItem = (VersionTreeItem) child;
                if (versionItem.getVersion().isPromoted()) {
                    version = versionItem.getVersion();
                    break;
                }
            }

            if (version == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("There isn't a promoted version of that package.");
                alert.setContentText("If there is supposed to be, try clicking \"Refresh\" in the packages tab.");
                alert.showAndWait();
                return;
            }
        } else if (selectedItem instanceof VersionTreeItem) {
            VersionTreeItem item = (VersionTreeItem) selectedItem;
            version = item.getVersion();
        } else {
            PVIApplication.get().showExceptionDialog("This shouldn't happen!", "Invalid tree item found", new Exception());
            return;
        }

        PVIApplication.get().showProvisionDialog(version.getName(), version.getVersion());
    }

    @FXML
    protected void handlePromoteButtonPressed(ActionEvent event) {
        TreeItem<String> selectedItem = packageTree.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("You must select a package to promote!");
            alert.showAndWait();
            return;
        }

        if (!(selectedItem instanceof VersionTreeItem)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("You must select a version to promote!");
            alert.showAndWait();
            return;
        }

        VersionTreeItem item = (VersionTreeItem) selectedItem;

        if (PVIClient.get().sendPromote(item.getVersion().getName(), item.getVersion().getVersion())) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Promotion");
            alert.setHeaderText(null);
            alert.setContentText("Sent promotion of " + item.getVersion().getName() + " at " + item.getVersion().getVersion() + " to network.");
            alert.showAndWait()
                    .map((r) -> {
                        handleRefreshButtonPressed(null);
                        return null;
                    });
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Unable to send promotion to network.");
            alert.showAndWait();
        }
    }

    @Data()
    private static final class PackageVersion {
        private String name;
        private String version;
        private boolean promoted;

        PackageVersion(String name, String version, boolean promoted) {
            this.name = name;
            this.version = version;
            this.promoted = promoted;
        }
    }

    private static final class PackageTreeItem extends TreeItem<String> {
        @Getter
        private String name;

        PackageTreeItem(String name, List<PackageVersion> versions) {
            super(name);
            this.name = name;

            versions.forEach(v -> this.getChildren().add(new VersionTreeItem(v)));
            this.getChildren().sort((a, b) -> b.getValue().compareTo(a.getValue()));
        }
    }

    private static final class VersionTreeItem extends TreeItem<String> {
        @Getter
        private PackageVersion version;

        VersionTreeItem(PackageVersion version) {
            super(version.getVersion() + (version.isPromoted() ? " [promoted]" : ""));
            this.version = version;
        }
    }
}
