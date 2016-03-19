package io.playpen.visual;

import io.playpen.core.coordinator.PlayPen;
import io.playpen.core.coordinator.api.APIClient;
import io.playpen.core.networking.TransactionInfo;
import io.playpen.core.networking.TransactionManager;
import io.playpen.core.protocol.Commands;
import io.playpen.core.protocol.Coordinator;
import io.playpen.core.protocol.P3;
import io.playpen.core.protocol.Protocol;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import lombok.extern.log4j.Log4j2;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    private List<PPEventListener> listeners = new LinkedList<>();

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
        log.info("Received provision response: success = " + c_provisionResponse.getOk());
        listeners.stream().forEach(listener -> listener.receivedProvisionResponse(c_provisionResponse, transactionInfo));
        return true;
    }

    @Override
    public boolean processCoordinatorCreated(Commands.C_CoordinatorCreated c_coordinatorCreated, TransactionInfo transactionInfo) {
        return false;
    }

    @Override
    public boolean processConsoleMessage(Commands.C_ConsoleMessage c_consoleMessage, TransactionInfo transactionInfo) {
        listeners.stream().forEach(listener -> listener.receivedConsoleMessage(c_consoleMessage.getConsoleId(), c_consoleMessage.getValue(), transactionInfo));
        return true;
    }

    @Override
    public boolean processDetachConsole(Commands.C_ConsoleDetached c_consoleDetached, TransactionInfo transactionInfo) {
        log.info("Received console detach: " + c_consoleDetached.getConsoleId());
        listeners.stream().forEach(listener -> listener.receivedDetachConsole(c_consoleDetached.getConsoleId(), transactionInfo));
        return true;
    }

    @Override
    public boolean processConsoleAttached(Commands.C_ConsoleAttached c_consoleAttached, TransactionInfo transactionInfo) {
        log.info("Received console attach: " + c_consoleAttached.getConsoleId());
        if (c_consoleAttached.getOk())
            listeners.stream().forEach(listener -> listener.receivedConsoleAttach(c_consoleAttached.getConsoleId(), transactionInfo));
        else
            listeners.stream().forEach(listener -> listener.receivedConsoleAttachFail(transactionInfo));
        return true;
    }

    @Override
    public boolean processListResponse(Commands.C_CoordinatorListResponse c_coordinatorListResponse, TransactionInfo transactionInfo) {
        log.info("Received coordinator list: " + c_coordinatorListResponse.getCoordinatorsCount() + " local coordinators.");
        listeners.stream().forEach(listener -> listener.receivedListResponse(c_coordinatorListResponse, transactionInfo));
        return true;
    }

    @Override
    public boolean processAck(Commands.C_Ack c_ack, TransactionInfo transactionInfo) {
        log.info("ACK - " + (c_ack.hasResult() ? c_ack.getResult() : "no result"));
        return true;
    }

    @Override
    public boolean processPackageList(Commands.C_PackageList message, TransactionInfo info) {
        log.info("Received package list: " + message.getPackagesCount() + " packages.");
        listeners.stream().forEach(listener -> listener.receivedPackageList(message, info));
        return true;
    }

    @Override
    public boolean processAccessDenied(Commands.C_AccessDenied c_accessDenied, TransactionInfo transactionInfo) {
        log.info("Received access denied for transaction " + c_accessDenied.getTid() + ": " + c_accessDenied.getResult());
        listeners.stream().forEach(listener -> listener.receivedAccessDenied(c_accessDenied, transactionInfo));
        return true;
    }

    public void addEventListener(PPEventListener listener) {
        listeners.add(listener);
    }

    public boolean sendSync() {
        Commands.Sync sync = Commands.Sync.newBuilder()
                .setEnabled(false)
                .setName(getName())
                .build();

        Commands.BaseCommand command = Commands.BaseCommand.newBuilder()
                .setType(Commands.BaseCommand.CommandType.SYNC)
                .setSync(sync)
                .build();

        TransactionInfo info = TransactionManager.get().begin();

        Protocol.Transaction message = TransactionManager.get()
                .build(info.getId(), Protocol.Transaction.Mode.SINGLE, command);
        if (message == null) {
            log.error("Unable to build message for sync");
            TransactionManager.get().cancel(info.getId());
            return false;
        }

        log.info("Sending SYNC to network coordinator");
        return TransactionManager.get().send(info.getId(), message, null);
    }

    public TransactionInfo sendListRequest() {
        Commands.BaseCommand command = Commands.BaseCommand.newBuilder()
                .setType(Commands.BaseCommand.CommandType.C_GET_COORDINATOR_LIST)
                .build();

        TransactionInfo info = TransactionManager.get().begin();

        Protocol.Transaction message = TransactionManager.get()
                .build(info.getId(), Protocol.Transaction.Mode.CREATE, command);
        if (message == null) {
            log.error("Unable to build message for C_GET_COORDINATOR_LIST");
            TransactionManager.get().cancel(info.getId());
            return null;
        }

        log.info("Sending C_GET_COORDINATOR_LIST to network");
        if (TransactionManager.get().send(info.getId(), message, null))
            return info;
        return null;
    }

    public TransactionInfo sendRequestPackageList() {
        Commands.BaseCommand command = Commands.BaseCommand.newBuilder()
                .setType(Commands.BaseCommand.CommandType.C_REQUEST_PACKAGE_LIST)
                .build();

        TransactionInfo info = TransactionManager.get().begin();

        Protocol.Transaction message = TransactionManager.get()
                .build(info.getId(), Protocol.Transaction.Mode.CREATE, command);
        if (message == null) {
            log.error("Unable to build message for C_REQUEST_PACKAGE_LIST");
            TransactionManager.get().cancel(info.getId());
            return null;
        }

        log.info("Sending C_REQUEST_PACKAGE_LIST to network");
        if (TransactionManager.get().send(info.getId(), message, null))
            return info;
        return null;
    }

    public boolean sendShutdown(String coordId) {
        Commands.C_Shutdown shutdown = Commands.C_Shutdown.newBuilder()
                .setUuid(coordId)
                .build();

        Commands.BaseCommand command = Commands.BaseCommand.newBuilder()
                .setType(Commands.BaseCommand.CommandType.C_SHUTDOWN)
                .setCShutdown(shutdown)
                .build();

        TransactionInfo info = TransactionManager.get().begin();

        Protocol.Transaction message = TransactionManager.get()
                .build(info.getId(), Protocol.Transaction.Mode.SINGLE, command);
        if (message == null) {
            log.error("Unable to build message for C_SHUTDOWN");
            TransactionManager.get().cancel(info.getId());
            return false;
        }

        log.info("Sending C_SHUTDOWN to network");
        return TransactionManager.get().send(info.getId(), message, null);
    }

    public TransactionInfo sendAttachConsole(String coordId, String serverId) {
        Commands.C_AttachConsole attach = Commands.C_AttachConsole.newBuilder()
                .setCoordinatorId(coordId)
                .setServerId(serverId)
                .build();

        Commands.BaseCommand command = Commands.BaseCommand.newBuilder()
                .setType(Commands.BaseCommand.CommandType.C_ATTACH_CONSOLE)
                .setCAttachConsole(attach)
                .build();

        TransactionInfo info = TransactionManager.get().begin();

        Protocol.Transaction message = TransactionManager.get()
                .build(info.getId(), Protocol.Transaction.Mode.CREATE, command);
        if(message == null) {
            log.error("Unable to build message for C_ATTACH_CONSOLE");
            TransactionManager.get().cancel(info.getId());
            return null;
        }

        log.info("Sending C_ATTACH_CONSOLE to network coordinator");
        if(TransactionManager.get().send(info.getId(), message, null))
            return info;
        return null;
    }

    public boolean sendDetachConsole(String consoleId) {
        Commands.C_DetachConsole.Builder detach = Commands.C_DetachConsole.newBuilder();
        if (consoleId != null)
            detach.setConsoleId(consoleId);

        Commands.BaseCommand command = Commands.BaseCommand.newBuilder()
                .setType(Commands.BaseCommand.CommandType.C_DETACH_CONSOLE)
                .setCDetachConsole(detach.build())
                .build();

        TransactionInfo info = TransactionManager.get().begin();

        Protocol.Transaction message = TransactionManager.get()
                .build(info.getId(), Protocol.Transaction.Mode.SINGLE, command);
        if(message == null) {
            log.error("Unable to build message for C_DETACH_CONSOLE");
            TransactionManager.get().cancel(info.getId());
            return false;
        }

        log.info("Sending C_DETACH_CONSOLE to network coordinator");
        return TransactionManager.get().send(info.getId(), message, null);
    }

    public boolean sendInput(String coordId, String serverId, String input) {
        Commands.C_SendInput protoInput = Commands.C_SendInput.newBuilder()
                .setCoordinatorId(coordId)
                .setServerId(serverId)
                .setInput(input)
                .build();

        Commands.BaseCommand command = Commands.BaseCommand.newBuilder()
                .setType(Commands.BaseCommand.CommandType.C_SEND_INPUT)
                .setCSendInput(protoInput)
                .build();

        TransactionInfo info = TransactionManager.get().begin();

        Protocol.Transaction message = TransactionManager.get()
                .build(info.getId(), Protocol.Transaction.Mode.SINGLE, command);
        if(message == null) {
            log.error("Unable to build message for send input");
            TransactionManager.get().cancel(info.getId());
            return false;
        }

        log.info("Sending C_SEND_INPUT to network coordinator");
        return TransactionManager.get().send(info.getId(), message, null);
    }

    public boolean sendDeprovision(String coordId, String serverId, boolean force) {
        Commands.C_Deprovision deprovision = Commands.C_Deprovision.newBuilder()
                .setCoordinatorId(coordId)
                .setServerId(serverId)
                .setForce(force)
                .build();

        Commands.BaseCommand command = Commands.BaseCommand.newBuilder()
                .setType(Commands.BaseCommand.CommandType.C_DEPROVISION)
                .setCDeprovision(deprovision)
                .build();

        TransactionInfo info = TransactionManager.get().begin();

        Protocol.Transaction message = TransactionManager.get()
                .build(info.getId(), Protocol.Transaction.Mode.SINGLE, command);
        if(message == null) {
            log.error("Unable to build message for deprovision");
            TransactionManager.get().cancel(info.getId());
            return false;
        }

        log.info("Sending C_DEPROVISION to network coordinator");
        return TransactionManager.get().send(info.getId(), message, null);
    }

    public boolean sendFreezeServer(String coordId, String serverId) {
        Commands.C_FreezeServer freeze = Commands.C_FreezeServer.newBuilder()
                .setCoordinatorId(coordId)
                .setServerId(serverId)
                .build();

        Commands.BaseCommand command = Commands.BaseCommand.newBuilder()
                .setType(Commands.BaseCommand.CommandType.C_FREEZE_SERVER)
                .setCFreezeServer(freeze)
                .build();

        TransactionInfo info = TransactionManager.get().begin();

        Protocol.Transaction message = TransactionManager.get()
                .build(info.getId(), Protocol.Transaction.Mode.SINGLE, command);
        if(message == null) {
            log.error("Unable to build message for C_FREEZE_SERVER");
            TransactionManager.get().cancel(info.getId());
            return false;
        }

        log.info("Sending C_FREEZE_SERVER to network coordinator");
        return TransactionManager.get().send(info.getId(), message, null);
    }

    public TransactionInfo sendProvision(String id, String version, String coordinator, String serverName, Map<String, String> properties) {
        P3.P3Meta meta = P3.P3Meta.newBuilder()
                .setId(id)
                .setVersion(version)
                .build();

        Commands.C_Provision.Builder provisionBuilder = Commands.C_Provision.newBuilder()
                .setP3(meta);

        if(coordinator != null) {
            provisionBuilder.setCoordinator(coordinator);
        }

        if(serverName != null) {
            provisionBuilder.setServerName(serverName);
        }

        for(Map.Entry<String, String> prop : properties.entrySet()) {
            provisionBuilder.addProperties(Coordinator.Property.newBuilder().setName(prop.getKey()).setValue(prop.getValue()).build());
        }

        Commands.BaseCommand command = Commands.BaseCommand.newBuilder()
                .setType(Commands.BaseCommand.CommandType.C_PROVISION)
                .setCProvision(provisionBuilder.build())
                .build();

        TransactionInfo info = TransactionManager.get().begin();

        Protocol.Transaction message = TransactionManager.get()
                .build(info.getId(), Protocol.Transaction.Mode.CREATE, command);
        if(message == null) {
            log.error("Unable to build message for provision");
            TransactionManager.get().cancel(info.getId());
            return null;
        }

        log.info("Sending C_PROVISION to network coordinator");
        if (TransactionManager.get().send(info.getId(), message, null))
            return info;
        return null;
    }

    public boolean sendPromote(String id, String version) {
        Commands.C_Promote promote = Commands.C_Promote.newBuilder()
                .setP3(P3.P3Meta.newBuilder().setId(id).setVersion(version).build())
                .build();

        Commands.BaseCommand command = Commands.BaseCommand.newBuilder()
                .setType(Commands.BaseCommand.CommandType.C_PROMOTE)
                .setCPromote(promote)
                .build();

        TransactionInfo info = TransactionManager.get().begin();

        Protocol.Transaction message = TransactionManager.get()
                .build(info.getId(), Protocol.Transaction.Mode.SINGLE, command);
        if(message == null) {
            log.error("Unable to build message for promote");
            TransactionManager.get().cancel(info.getId());
            return false;
        }

        log.info("Sending C_PROMOTE to network coordinator");
        return TransactionManager.get().send(info.getId(), message, null);
    }
}
