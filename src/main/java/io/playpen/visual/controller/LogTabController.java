package io.playpen.visual.controller;

import io.playpen.visual.util.WorkspaceLogAppender;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.util.ResourceBundle;

public class LogTabController implements Initializable {
    @FXML
    private TextArea consoleArea;

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
