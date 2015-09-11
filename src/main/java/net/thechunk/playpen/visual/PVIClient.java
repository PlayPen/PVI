package net.thechunk.playpen.visual;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import lombok.extern.log4j.Log4j2;
import net.thechunk.playpen.coordinator.PlayPen;
import net.thechunk.playpen.coordinator.api.APIClient;
import net.thechunk.playpen.networking.TransactionInfo;
import net.thechunk.playpen.protocol.Commands;

import java.net.InetAddress;

@Log4j2
public class PVIClient extends APIClient {
    public static PVIClient get() {
        return (PVIClient) PlayPen.get();
    }

    private String clientName;
    private String clientUUID;
    private String clientKey;
    private InetAddress networkIp;
    private int networkPort;

    public PVIClient(String name, String uuid, String key, InetAddress ip, int port) {
        super();

        clientName = name;
        clientUUID = uuid;
        clientKey = key;
        networkIp = ip;
        networkPort = port;
    }

    @Override
    public String getName() {
        return clientName;
    }

    @Override
    public String getUUID() {
        return clientUUID;
    }

    @Override
    public String getKey() {
        return clientKey;
    }

    @Override
    public InetAddress getNetworkIP() {
        return networkIp;
    }

    @Override
    public int getNetworkPort() {
        return networkPort;
    }

    @Override
    public boolean start() {
        if (super.start()) {
            getChannel().closeFuture().addListener(channelFuture -> {
                if (PVIApplication.get().isClosing())
                    return;

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Disconnected");
                    alert.setHeaderText(null);
                    alert.setContentText("Disconnected from network. Exiting!");
                    alert.showAndWait();

                    PVIApplication.get().quit();
                });
            });

            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public boolean processProvisionResponse(Commands.C_ProvisionResponse c_provisionResponse, TransactionInfo transactionInfo) {
        return false;
    }

    @Override
    public boolean processCoordinatorCreated(Commands.C_CoordinatorCreated c_coordinatorCreated, TransactionInfo transactionInfo) {
        return false;
    }

    @Override
    public boolean processConsoleMessage(Commands.C_ConsoleMessage c_consoleMessage, TransactionInfo transactionInfo) {
        return false;
    }

    @Override
    public boolean processDetachConsole(Commands.C_ConsoleDetached c_consoleDetached, TransactionInfo transactionInfo) {
        return false;
    }

    @Override
    public boolean processConsoleAttached(Commands.C_ConsoleAttached c_consoleAttached, TransactionInfo transactionInfo) {
        return false;
    }

    @Override
    public boolean processListResponse(Commands.C_CoordinatorListResponse c_coordinatorListResponse, TransactionInfo transactionInfo) {
        return false;
    }

    @Override
    public boolean processAck(Commands.C_Ack c_ack, TransactionInfo transactionInfo) {
        return false;
    }
}
