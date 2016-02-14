package io.playpen.visual;

import io.playpen.core.networking.TransactionInfo;
import io.playpen.core.protocol.Commands;

public interface PPEventListener {
    void receivedListResponse(Commands.C_CoordinatorListResponse response, TransactionInfo info);
    void receivedConsoleAttach(String consoleId, TransactionInfo info);
    void receivedConsoleAttachFail(TransactionInfo info);
    void receivedDetachConsole(String consoleId, TransactionInfo info);
    void receivedConsoleMessage(String consoleId, String value, TransactionInfo info);
    void receivedPackageList(Commands.C_PackageList list, TransactionInfo info);
    void receivedProvisionResponse(Commands.C_ProvisionResponse response, TransactionInfo info);
}
