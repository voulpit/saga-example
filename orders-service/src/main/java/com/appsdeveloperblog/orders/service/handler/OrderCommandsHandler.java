package com.appsdeveloperblog.orders.service.handler;

import com.appsdeveloperblog.core.commands.ApproveOrderCommand;
import com.appsdeveloperblog.core.commands.RejectOrderCommand;
import com.appsdeveloperblog.orders.service.OrderService;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = "${orders.commands.topic.name}")
public class OrderCommandsHandler {
    private final OrderService orderService;

    public OrderCommandsHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaHandler
    public void handle(ApproveOrderCommand command) {
        orderService.approveOrder(command.getOrderId());
    }

    @KafkaHandler
    public void handle(RejectOrderCommand command) {
        orderService.cancelOrder(command.getOrderId());
    }
}
