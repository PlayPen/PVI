package io.playpen.visual;

import io.playpen.core.networking.TransactionInfo;
import io.playpen.core.protocol.Commands;

public interface PPEventListener {
    default void receivedListResponse(Commands.C_CoordinatorListResponse response, TransactionInfo info) {

    }

    default void receivedConsoleAttach(String consoleId, TransactionInfo info) {

    }

    default void receivedConsoleAttachFail(TransactionInfo info) {

    }

    default void receivedDetachConsole(String consoleId, TransactionInfo info) {

    }

    default void receivedConsoleMessage(String consoleId, String value, TransactionInfo info) {

    }

    default void receivedPackageList(Commands.C_PackageList list, TransactionInfo info) {

    }

    default void receivedProvisionResponse(Commands.C_ProvisionResponse response, TransactionInfo info) {

    }

    default void receivedAccessDenied(Commands.C_AccessDenied message, TransactionInfo info) {

    }

    default void receivedAck(Commands.C_Ack c_ack) {

    }
}
