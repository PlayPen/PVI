package net.thechunk.playpen.visual.controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import net.thechunk.playpen.visual.util.WorkspaceLogAppender;

import java.net.URL;
import java.util.ResourceBundle;

public class LogTabController implements Initializable {
    @FXML
    TextArea consoleArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        consoleArea.setText("");
        consoleArea.textProperty().addListener((observable, oldValue, newValue) -> {
            consoleArea.setScrollTop(Double.MAX_VALUE);
        });

        WorkspaceLogAppender.get().setLogTabController(this);
        WorkspaceLogAppender.get().pumpQueue();
    }

    public void log(String message) {
        consoleArea.appendText(message);
    }
}
