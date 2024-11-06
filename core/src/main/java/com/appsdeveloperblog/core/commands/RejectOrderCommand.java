package com.appsdeveloperblog.core.commands;

import java.util.UUID;

public class RejectOrderCommand {
    private UUID orderId;

    public RejectOrderCommand() {
    }

    public RejectOrderCommand(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }
}
