package net.thechunk.playpen.visual;

import net.thechunk.playpen.networking.TransactionInfo;
import net.thechunk.playpen.protocol.Commands;

public interface PPEventListener {
    void receivedListResponse(Commands.C_CoordinatorListResponse response, TransactionInfo info);
}