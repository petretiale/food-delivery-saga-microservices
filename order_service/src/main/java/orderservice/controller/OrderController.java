package orderservice.controller;

import orderservice.config.RabbitMQConfig;
import orderservice.dto.OrderRequest;
import orderservice.dto.OrderResponse;
import orderservice.event.OrderCreatedEvent;
import orderservice.model.Order;
import orderservice.model.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orderservice.repository.OrderRepository;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    public OrderController(OrderRepository repository, RabbitTemplate rabbitTemplate) {
        this.orderRepository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        String orderID = UUID.randomUUID().toString();

        Order order = new Order(
                orderID,
                request.customerName(),
                request.item(),
                request.amount(),
                OrderStatus.PENDING
        );

        Order savedOrder = orderRepository.save(order);
        log.info("[ORDER SERVICE] Nuovo ordine creato: {} per cliente: {} (€{})",
                savedOrder.getOrderID(), savedOrder.getCustomerName(), savedOrder.getAmount());

        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
                savedOrder.getOrderID(),
                savedOrder.getCustomerName(),
                savedOrder.getAmount());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                orderCreatedEvent
        );

        return ResponseEntity.ok(toResponse(savedOrder));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String id) {
        log.info("[ORDER SERVICE] Richiesta stato ordine: {}", id);
        return orderRepository.findById(id)
                .map(order -> ResponseEntity.ok(toResponse(order)))
                .orElse(ResponseEntity.notFound().build());
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getOrderID(),
                order.getCustomerName(),
                order.getItem(),
                order.getAmount(),
                order.getOrderStatus()
        );
    }
}
