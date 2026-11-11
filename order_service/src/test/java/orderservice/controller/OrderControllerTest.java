package orderservice.controller;

import orderservice.config.RabbitMQConfig;
import orderservice.dto.OrderRequest;
import orderservice.dto.OrderResponse;
import orderservice.event.OrderCreatedEvent;
import orderservice.model.Order;
import orderservice.model.OrderStatus;
import orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OrderController orderController;

    @BeforeEach
    void setUp() {
        orderController = new OrderController(orderRepository, rabbitTemplate);
    }

    @Test
    @DisplayName("POST /orders crea l'ordine in PENDING e pubblica l'evento su RabbitMQ")
    void shouldCreateOrderAndPublishEvent() {
        // GIVEN
        OrderRequest request = new OrderRequest("Alessandro", "Pizza Margherita", 12.0);

        // Quando il repository salva un ordine, facciamogli restituire l'ordine stesso
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        ResponseEntity<OrderResponse> response = orderController.createOrder(request);

        // THEN
        assertNotNull(response.getBody());
        assertEquals(OrderStatus.PENDING, response.getBody().status());
        assertEquals("Alessandro", response.getBody().customerName());

        // Verifichiamo che il messaggio sia stato inviato con la routing key corretta
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.ORDER_CREATED_ROUTING_KEY),
                eventCaptor.capture()
        );

        OrderCreatedEvent publishedEvent = eventCaptor.getValue();
        assertEquals("Alessandro", publishedEvent.customerName());
        assertEquals(12.0, publishedEvent.amount());
    }
}
