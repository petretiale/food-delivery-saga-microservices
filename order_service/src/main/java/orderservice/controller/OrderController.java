package orderservice.controller;

import orderservice.config.RabbitMQConfig;
import orderservice.dto.OrderRequest;
import orderservice.event.OrderCreatedEvent;
import orderservice.model.Order;
import orderservice.model.OrderStatus;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orderservice.repository.OrderRepository;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private OrderRepository orderRepository;
    private RabbitTemplate rabbitTemplate;

    public OrderController(OrderRepository repository, RabbitTemplate rabbitTemplate) {
        this.orderRepository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping
    public ResponseEntity<Order> createOrders(@RequestBody OrderRequest request) {
        String orderID = UUID.randomUUID().toString();

        Order order = new Order(
                orderID,
                request.customerName(),
                request.item(),
                request.amount(),
                OrderStatus.PENDING
        );

        Order savedOrder = orderRepository.save(order);

        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
                savedOrder.getOrderID(),
                savedOrder.getCustomerName(),
                savedOrder.getAmount());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                orderCreatedEvent
        );

        return ResponseEntity.ok(savedOrder);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable String id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
