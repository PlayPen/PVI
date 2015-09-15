package net.thechunk.playpen.visual.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import lombok.Getter;
import lombok.Setter;
import net.thechunk.playpen.networking.TransactionInfo;
import net.thechunk.playpen.protocol.Commands;
import net.thechunk.playpen.protocol.P3;
import net.thechunk.playpen.visual.PVIApplication;
import net.thechunk.playpen.visual.PVIClient;

import java.net.URL;
import java.util.ResourceBundle;

public class PackagesTabController implements Initializable {
    @FXML
    ListView<String> listView;

    @Getter
    @Setter
    private Tab tab = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void updatePackages(Commands.C_PackageList list) {
        final ObservableList<String> packages = FXCollections.observableArrayList();
        for (P3.P3Meta p3 : list.getPackagesList()) {
            packages.add(p3.getId() + " @ " + p3.getVersion());
        }

        packages.sort(String::compareTo);
        listView.setItems(packages);
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
}
