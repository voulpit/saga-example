package com.appsdeveloperblog.orders.service;

import com.appsdeveloperblog.core.dto.Order;
import com.appsdeveloperblog.core.events.OrderApprovedEvent;
import com.appsdeveloperblog.core.events.OrderCreatedEvent;
import com.appsdeveloperblog.core.types.OrderStatus;
import com.appsdeveloperblog.orders.dao.jpa.entity.OrderEntity;
import com.appsdeveloperblog.orders.dao.jpa.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String ordersEventsTopicName;

    public OrderServiceImpl(OrderRepository orderRepository, KafkaTemplate<String, Object> kafkaTemplate,
                            @Value("${orders.events.topic.name}") String topicName) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.ordersEventsTopicName = topicName;
    }

    @Override
    public Order placeOrder(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setCustomerId(order.getCustomerId());
        entity.setProductId(order.getProductId());
        entity.setProductQuantity(order.getProductQuantity());
        entity.setStatus(OrderStatus.CREATED);
        orderRepository.save(entity);

        OrderCreatedEvent event = new OrderCreatedEvent(entity.getId(), entity.getCustomerId(), order.getProductId(),
                order.getProductQuantity());
        kafkaTemplate.send(ordersEventsTopicName, event);

        return new Order(
                entity.getId(),
                entity.getCustomerId(),
                entity.getProductId(),
                entity.getProductQuantity(),
                entity.getStatus());
    }

    @Override
    public void approveOrder(UUID orderId) {
        OrderEntity order = orderRepository.findById(orderId).orElse(null);
        Assert.notNull(order, "No order found with id=" + orderId);
        order.setStatus(OrderStatus.APPROVED);
        orderRepository.save(order);

        OrderApprovedEvent event = new OrderApprovedEvent(orderId);
        kafkaTemplate.send(ordersEventsTopicName, event);
    }

    @Override
    public void cancelOrder(UUID orderId) {
        OrderEntity order = orderRepository.findById(orderId).orElse(null);
        Assert.notNull(order, "No order found with id=" + orderId);
        order.setStatus(OrderStatus.REJECTED);
        orderRepository.save(order);
    }
}
