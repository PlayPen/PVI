package net.thechunk.playpen.visual.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.Getter;
import lombok.Setter;
import net.thechunk.playpen.networking.TransactionInfo;
import net.thechunk.playpen.protocol.Commands;
import net.thechunk.playpen.protocol.P3;
import net.thechunk.playpen.visual.PVIApplication;
import net.thechunk.playpen.visual.PVIClient;

import java.net.URL;
import java.util.*;

public class PackagesTabController implements Initializable {
    @FXML
    TreeView<String> packageTree;

    @Getter
    @Setter
    private Tab tab = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void updatePackages(Commands.C_PackageList list) {
        Map<String, List<String>> packages = new HashMap<>();
        list.getPackagesList().forEach(p3 -> {
            if (packages.containsKey(p3.getId())) {
                packages.get(p3.getId()).add(p3.getVersion());
            }
            else {
                List<String> versions = new LinkedList<String>();
                versions.add(p3.getVersion());
                packages.put(p3.getId(), versions);
            }
        });

        TreeItem<String> rootNode = new TreeItem<>("Packages");
        packageTree.setRoot(rootNode);
        packageTree.setShowRoot(false);

        packages.forEach((name, versions) -> {
            rootNode.getChildren().add(new PackageTreeItem(name, versions));
        });

        rootNode.getChildren().sort((a, b) -> a.getValue().compareTo(b.getValue()));
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
        }
        else {
            PVIApplication.get().showTransactionDialog("Package List", info, null);
        }
    }

    private static final class PackageTreeItem extends TreeItem<String> {
        @Getter
        private String name;

        public PackageTreeItem(String name, List<String> versions) {
            super(name);
            this.name = name;

            versions.forEach(v -> this.getChildren().add(new VersionTreeItem(v)));
            this.getChildren().sort((a, b) -> b.getValue().compareTo(a.getValue()));
        }
    }

    private static final class VersionTreeItem extends TreeItem<String> {
        @Getter
        private String version;

        public VersionTreeItem(String version) {
            super(version);
            this.version = version;
        }
    }
}
