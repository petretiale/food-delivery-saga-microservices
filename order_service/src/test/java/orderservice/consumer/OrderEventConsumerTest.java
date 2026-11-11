package orderservice.consumer;

import orderservice.event.DeliveryProcessedEvent;
import orderservice.event.PaymentProcessedEvent;
import orderservice.model.Order;
import orderservice.model.OrderStatus;
import orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderEventConsumer orderEventConsumer;

    @BeforeEach
    void setUp() {
        // Inizializziamo il consumer iniettando il repository simulato (mock)
        orderEventConsumer = new OrderEventConsumer(orderRepository);
    }

    @Test
    @DisplayName("Saga Abort: Fondi insufficienti imposta lo stato a CANCELLED")
    void shouldCancelOrderWhenPaymentFailsDueToInsufficientFunds() {
        // GIVEN (Dati di partenza)
        String orderId = "order-123";
        Order existingOrder = new Order(orderId, "Alessandro", "Pizza", 60.0, OrderStatus.PENDING);

        // Diciamo al mock del DB: quando cerchi questo ID, restituisci l'ordine sopra
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

        PaymentProcessedEvent failedPaymentEvent = new PaymentProcessedEvent(
                orderId,
                false,
                "Alessandro",
                "INSUFFICIENT_FUNDS",
                60.0
        );

        // WHEN (Azione che vogliamo testare)
        orderEventConsumer.handlePaymentEvent(failedPaymentEvent);

        // THEN (Verifiche)
        // 1. Verifichiamo che lo stato sull'oggetto sia diventato CANCELLED
        assertEquals(OrderStatus.CANCELLED, existingOrder.getOrderStatus());
        // 2. Verifichiamo che il consumer abbia chiamato orderRepository.save(...)
        verify(orderRepository).save(existingOrder);
    }

    @Test
    @DisplayName("Happy Path: Assegnazione rider imposta lo stato a CONFIRMED")
    void shouldConfirmOrderWhenRiderIsAssigned() {
        // GIVEN
        String orderId = "order-123";
        Order existingOrder = new Order(orderId, "Alessandro", "Pizza", 20.0, OrderStatus.PENDING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

        DeliveryProcessedEvent deliveryAssignedEvent = new DeliveryProcessedEvent(
                orderId,
                true,
                "RIDER_ASSIGNED",
                20.0
        );

        // WHEN
        orderEventConsumer.handleDeliveryResult(deliveryAssignedEvent);

        // THEN
        assertEquals(OrderStatus.CONFIRMED, existingOrder.getOrderStatus());
        verify(orderRepository).save(existingOrder);
    }

    @Test
    @DisplayName("Saga Rollback: Mancata consegna imposta lo stato a CANCELLED")
    void shouldCancelOrderWhenDeliveryFails() {
        // GIVEN
        String orderId = "order-123";
        Order existingOrder = new Order(orderId, "NoRider", "Pizza", 20.0, OrderStatus.PENDING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

        DeliveryProcessedEvent deliveryFailedEvent = new DeliveryProcessedEvent(
                orderId,
                false,
                "RIDER_NOT_FOUND",
                20.0
        );

        // WHEN
        orderEventConsumer.handleDeliveryResult(deliveryFailedEvent);

        // THEN
        assertEquals(OrderStatus.CANCELLED, existingOrder.getOrderStatus());
        verify(orderRepository).save(existingOrder);
    }
}
