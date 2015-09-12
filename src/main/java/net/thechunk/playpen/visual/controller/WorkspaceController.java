package net.thechunk.playpen.visual.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import net.thechunk.playpen.visual.PVIApplication;
import net.thechunk.playpen.visual.PVIClient;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class WorkspaceController implements Initializable {
    @FXML
    TabPane tabPane;

    private Tab consoleTab;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (PVIClient.get() == null) {
            PVIApplication.get().showExceptionDialog("Exception Encountered", "PVIClient was not initialized when entering the workspace.", new Exception("PVIClient.get() returned null"));
            PVIApplication.get().quit();
        }

        try {
            consoleTab = (Tab)FXMLLoader.load(getClass().getClassLoader().getResource("ui/Log.fxml"));
            tabPane.getTabs().add(consoleTab);
            tabPane.getSelectionModel().select(consoleTab);
        } catch (IOException e) {
            PVIApplication.get().showExceptionDialog("Exception Encountered", "Unable to setup workspace", e);
            PVIApplication.get().quit();
            return;
        }

        if (!PVIClient.get().sendSync()) {
            PVIApplication.get().showExceptionDialog("Exception Encountered", "Unable to send SYNC to network", new Exception());
            PVIApplication.get().quit();
            return;
        }
    }
}
