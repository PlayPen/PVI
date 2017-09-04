package io.playpen.visual.controller;

import com.google.common.collect.Maps;
import io.playpen.core.p3.P3Package;
import io.playpen.core.p3.PackageManager;
import io.playpen.core.p3.resolver.PromotedResolver;
import io.playpen.core.p3.step.CopyStep;
import io.playpen.core.p3.step.ExecuteStep;
import io.playpen.core.p3.step.ExpandAssetsStep;
import io.playpen.core.p3.step.ExpandStep;
import io.playpen.core.p3.step.PipeStep;
import io.playpen.core.p3.step.StringTemplateStep;
import io.playpen.core.protocol.Commands;
import io.playpen.visual.PPEventListener;
import io.playpen.visual.PVIApplication;
import io.playpen.visual.PVIClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @author Erik Rosemberg
 * @since 03/09/2017
 */
@Log4j2
public class UploadPackageTab implements Initializable {

    @FXML
    private TreeView<String> packagesTree;

    @FXML
    private Text updateText;

    @Getter
    @Setter
    private Tab tab = null;

    private PackageManager packageManager;

    private Map<String, P3Package> packageMap = Maps.newHashMap();

    private int acks;
    private CountDownLatch latch;

    private boolean uploading = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PVIClient.get().addEventListener(new PPEventListener() {
            @Override
            public void receivedAck(Commands.C_Ack c_ack) {
                log.info("ACK: " + c_ack.getResult());
                acks++;

                if (latch == null) return;
                latch.countDown();

                updateText.setText(c_ack.getResult());
                if (latch.getCount() <= 0) {
                    uploading = false;
                    PVIClient.get().setAwaitingAckResponse(false);
                }
            }
        });
    }

    @FXML
    public void handleSelectFilePress(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Please choose PlayPen packages");
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Playpen Package", "*.p3");
        chooser.getExtensionFilters().add(filter);
        chooser.setInitialDirectory(Paths.get(System.getProperty("user.dir")).toFile());

        Node node = (Node) event.getSource();
        List<File> files = chooser.showOpenMultipleDialog(node.getScene().getWindow());
        if (files != null) {
            if (packageManager == null) {
                try {
                    packageManager = (PackageManager) PVIClient.get().getUnsafe().allocateInstance(PackageManager.class);

                    Field resolvers = PackageManager.class.getDeclaredField("resolvers");
                    resolvers.setAccessible(true);
                    resolvers.set(packageManager, new ArrayList<>());
                    resolvers.setAccessible(false);

                    Field packageSteps = PackageManager.class.getDeclaredField("packageSteps");
                    packageSteps.setAccessible(true);
                    packageSteps.set(packageManager, new ConcurrentHashMap<>());
                    packageSteps.setAccessible(false);

                    Field promoted = PackageManager.class.getDeclaredField("promoted");
                    promoted.setAccessible(true);
                    promoted.set(packageManager, new ConcurrentHashMap<>());
                    promoted.setAccessible(false);

                    packageManager.addPackageResolver(new PromotedResolver());

                    packageManager.addPackageStep(new ExpandStep());
                    packageManager.addPackageStep(new StringTemplateStep());
                    packageManager.addPackageStep(new ExecuteStep());
                    packageManager.addPackageStep(new PipeStep());
                    packageManager.addPackageStep(new ExpandAssetsStep());
                    packageManager.addPackageStep(new CopyStep());
                } catch (Exception e) {
                    PVIApplication.get().showExceptionDialog("Exception Encountered", "Failed to instantiate PackageManager", e);
                }
            }

            TreeItem<String> rootNode = new TreeItem<>("Packages");
            packagesTree.setRoot(rootNode);
            packagesTree.setShowRoot(false);

            files.forEach(file -> {
                try {
                    P3Package p3Package = packageManager.readPackage(file);
                    log.info("Read P3Package " + p3Package.getId() + " successfully.");
                    rootNode.getChildren().add(new PackageTreeItem(p3Package));
                } catch (Exception e) {
                    PVIApplication.get().showExceptionDialog("Exception Encountered", "Unable to read package", e);
                }
            });

            rootNode.getChildren().sort(Comparator.comparing(TreeItem::getValue));
        }
    }

    @FXML
    public void handleUploadButtonPress(ActionEvent event) {
        if (!verifyCanUpload()) return;
        TreeItem<String> selected = packagesTree.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("You must select a package to upload!");
            alert.show();
            return;
        }

        log.info(selected.getValue());
        P3Package p3Package = packageMap.getOrDefault(selected.getValue(), null);
        if (p3Package == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("That item did not have a mapped P3Package.");
            alert.show();
            return;
        }

        packagesTree.getRoot().getChildren().remove(selected);
        PVIClient.get().setAwaitingAckResponse(true);
        uploading = true;
        updateText.setText("Sending package " + p3Package.getId() + " (" + p3Package.getVersion() + ") to network...");
        if (!PVIClient.get().sendPackage(p3Package)) {
            updateText.setText("Failed to upload package.");
            uploading = false;
            PVIClient.get().setAwaitingAckResponse(false);
            return;
        }
        updateText.setText("Sent package, waiting for Ack...");
        latch = new CountDownLatch(1 - acks);
        try {
            latch.wait();
        } catch (InterruptedException ignored) {
        }
    }

    @FXML
    public void handleUploadAllButtonPress(ActionEvent event) {
        if (!verifyCanUpload()) return;
        if (packageMap.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("No packages");
            alert.setHeaderText(null);
            alert.setContentText("There are no files to upload.");
            alert.showAndWait();
            return;
        }

        int count = 0;
        PVIClient.get().setAwaitingAckResponse(true);
        uploading = true;
        packagesTree.getRoot().getChildren().clear();
        for (P3Package p3Package : packageMap.values()) {
            if (!PVIClient.get().sendPackage(p3Package)) {
                updateText.setText("Failed to upload package " + p3Package.getVersion() + " (" + p3Package.getVersion() + ")");
                continue;
            }

            updateText.setText("Sending package " + p3Package.getId() + " (" + p3Package.getVersion() + ") to network...");
            count++;
        }

        if (count == 0) {
            sendPackageUpdate(Alert.AlertType.ERROR, "Failed to upload all files! Maybe NetworkCoordinator is down?");
            updateText.setText("Failed to upload all files!");
            PVIClient.get().setAwaitingAckResponse(false);
            uploading = false;
            return;
        }

        packageMap.clear();
        updateText.setText("Multiple file upload finished, waiting for ack...");
        latch = new CountDownLatch(count - acks);
        try {
            latch.await();
        } catch (InterruptedException ignored) {
        }
    }

    private boolean verifyCanUpload() {
        if (uploading) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Already uploading files");
            alert.setHeaderText(null);
            alert.setContentText("Upload already in progress, please wait for it to finish before uploading another package.");
            alert.showAndWait();
        }

        return !uploading;
    }

    private void sendPackageUpdate(Alert.AlertType type, String text) {
        Alert alert = new Alert(type);
        alert.setTitle("Package Update");
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.show();
    }

    private final class PackageTreeItem extends TreeItem<String> {
        PackageTreeItem(P3Package p3Package) {
            super(p3Package.getId() + " v" + p3Package.getVersion());
            this.getChildren().add(new DependencyTreeItem(p3Package.getDependencies()));
            packageMap.put(p3Package.getId() + " v" + p3Package.getVersion(), p3Package);
        }
    }

    private final class DependencyTreeItem extends TreeItem<String> {
        DependencyTreeItem(List<P3Package> p3Packages) {
            super("Dependencies");
            p3Packages.forEach(pack -> this.getChildren().add(new TreeItem<>(pack.getId() + " v" + pack.getVersion())));
        }
    }
}
