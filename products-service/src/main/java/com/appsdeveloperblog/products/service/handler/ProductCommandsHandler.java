package com.appsdeveloperblog.products.service.handler;

import com.appsdeveloperblog.core.commands.CancelProductReservationCommand;
import com.appsdeveloperblog.core.commands.ReserveProductCommand;
import com.appsdeveloperblog.core.dto.Product;
import com.appsdeveloperblog.core.events.ProductReservedCanceledEvent;
import com.appsdeveloperblog.core.events.ProductReservedEvent;
import com.appsdeveloperblog.core.events.ProductReservedFailedEvent;
import com.appsdeveloperblog.products.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@KafkaListener (topics = "${products.commands.topic.name}") // listens on the commands topic
public class ProductCommandsHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductCommandsHandler.class);
    private final ProductService productService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String productEventsTopicName;

    public ProductCommandsHandler(ProductService productService,
                                  KafkaTemplate<String, Object> kafkaTemplate,
                                  @Value("${products.events.topic.name}") String productEventsTopicName) {
        this.productService = productService;
        this.kafkaTemplate = kafkaTemplate;
        this.productEventsTopicName = productEventsTopicName;
    }

    @KafkaHandler
    public void handleCommand(@Payload ReserveProductCommand command) {
        try {
            Product product = new Product(command.getProductId(), command.getProductQuantity());
            Product reservedProduct = productService.reserve(product, command.getOrderId());
            ProductReservedEvent productReservedEvent = new ProductReservedEvent(command.getOrderId(),
                    reservedProduct.getId(), reservedProduct.getPrice(), reservedProduct.getQuantity());
            kafkaTemplate.send(productEventsTopicName, productReservedEvent); // ack on the events topic
        } catch (Exception e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            ProductReservedFailedEvent failedEvent = new ProductReservedFailedEvent(command.getOrderId(),
                    command.getProductId(), command.getProductQuantity());
            kafkaTemplate.send(productEventsTopicName, failedEvent);
        }
    }

    @KafkaHandler
    public void handleCommand(@Payload CancelProductReservationCommand command) {
        Product productToCancel = new Product(command.getProductId(), command.getProductQuantity());
        productService.cancelReservation(productToCancel, command.getOrderId());
        ProductReservedCanceledEvent event = new ProductReservedCanceledEvent(command.getOrderId(), command.getProductId());
        kafkaTemplate.send(productEventsTopicName, event);
    }
}
