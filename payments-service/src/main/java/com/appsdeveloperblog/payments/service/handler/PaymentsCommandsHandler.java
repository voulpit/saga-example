package com.appsdeveloperblog.payments.service.handler;

import com.appsdeveloperblog.core.commands.ProcessPaymentCommand;
import com.appsdeveloperblog.core.dto.Payment;
import com.appsdeveloperblog.core.events.PaymentFailedEvent;
import com.appsdeveloperblog.core.events.PaymentProcessedEvent;
import com.appsdeveloperblog.core.exceptions.CreditCardProcessorUnavailableException;
import com.appsdeveloperblog.payments.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = "${payments.commands.topic.name}")
public class PaymentsCommandsHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentsCommandsHandler.class);
    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String paymentEventsTopicName;

    public PaymentsCommandsHandler(PaymentService paymentService,
                                   KafkaTemplate<String, Object> kafkaTemplate,
                                   @Value("${payments.events.topic.name}") String paymentEventsTopicName) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
        this.paymentEventsTopicName = paymentEventsTopicName;
    }

    @KafkaHandler
    public void handle(@Payload ProcessPaymentCommand command) {
        try {
            Payment payment = new Payment(command.getOrderId(), command.getProductId(), command.getProductPrice(),
                    command.getProductQuantity());
            Payment processedPayment = paymentService.process(payment);
            PaymentProcessedEvent processedEvent = new PaymentProcessedEvent(command.getOrderId(), processedPayment.getId());
            kafkaTemplate.send(paymentEventsTopicName, processedEvent);

        } catch (CreditCardProcessorUnavailableException e) {
            PaymentFailedEvent failedEvent = new PaymentFailedEvent(command.getOrderId(), command.getProductId(),
                    command.getProductQuantity());
            kafkaTemplate.send(paymentEventsTopicName, failedEvent);
            LOGGER.error(e.getLocalizedMessage(), e);
        }
    }
}
