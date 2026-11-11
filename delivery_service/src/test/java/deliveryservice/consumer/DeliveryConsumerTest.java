package deliveryservice.consumer;

import deliveryservice.config.RabbitMQConfig;
import deliveryservice.event.DeliveryProcessedEvent;
import deliveryservice.event.PaymentProcessedEvent;
import deliveryservice.model.Delivery;
import deliveryservice.model.ProcessedMessage;
import deliveryservice.repository.DeliveryRepository;
import deliveryservice.repository.ProcessedMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryConsumerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private ProcessedMessageRepository processedMessageRepository;

    private DeliveryConsumer deliveryConsumer;

    @BeforeEach
    void setUp() {
        deliveryConsumer = new DeliveryConsumer(rabbitTemplate, deliveryRepository, processedMessageRepository);
    }

    @Test
    @DisplayName("Happy Path: Assegna un rider con successo e pubblica delivery.assigned")
    void shouldAssignRiderSuccessfully() {
        // GIVEN: pagamento autorizzato per cliente normale mai processato prima
        PaymentProcessedEvent paymentEvent = new PaymentProcessedEvent(
                "order-deliv-1",
                true,
                "Alessandro",
                "SUFFICIENT_FUNDS",
                25.0
        );
        when(processedMessageRepository.existsById("DELIVERY_order-deliv-1")).thenReturn(false);

        // WHEN
        deliveryConsumer.handleDelivery(paymentEvent);

        // THEN:
        // 1. Verifica che nel DB locale la consegna sia salvata con stato ASSIGNED e rider associato
        ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepository).save(deliveryCaptor.capture());
        Delivery savedDelivery = deliveryCaptor.getValue();
        assertTrue(savedDelivery.isRiderAssigned());
        assertEquals("ASSIGNED", savedDelivery.getStatus());
        assertNotNull(savedDelivery.getRiderName());

        // 2. Verifica che venga salvata la chiave di idempotenza
        verify(processedMessageRepository).save(any(ProcessedMessage.class));

        // 3. Verifica l'evento emesso su RabbitMQ
        ArgumentCaptor<DeliveryProcessedEvent> eventCaptor = ArgumentCaptor.forClass(DeliveryProcessedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.DELIVERY_SUCCESS_ROUTING_KEY),
                eventCaptor.capture()
        );
        DeliveryProcessedEvent published = eventCaptor.getValue();
        assertTrue(published.riderAssigned());
        assertEquals("RIDER_ASSIGNED", published.reason());
        assertEquals(25.0, published.amount());
    }

    @Test
    @DisplayName("Saga Failure Branch: Fallisce la consegna per cliente NoRider e pubblica delivery.failed")
    void shouldFailDeliveryWhenCustomerIsNoRider() {
        // GIVEN: ordine per il cliente speciale "NoRider"
        PaymentProcessedEvent paymentEvent = new PaymentProcessedEvent(
                "order-deliv-2",
                true,
                "NoRider",
                "SUFFICIENT_FUNDS",
                18.0
        );
        when(processedMessageRepository.existsById("DELIVERY_order-deliv-2")).thenReturn(false);

        // WHEN
        deliveryConsumer.handleDelivery(paymentEvent);

        // THEN:
        // 1. Verifica stato FAILED nel DB locale e nessun rider assegnato
        ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepository).save(deliveryCaptor.capture());
        Delivery savedDelivery = deliveryCaptor.getValue();
        assertFalse(savedDelivery.isRiderAssigned());
        assertEquals("FAILED", savedDelivery.getStatus());
        assertNull(savedDelivery.getRiderName());

        // 2. Verifica che venga emesso delivery.failed con l'importo originario da rimborsare
        ArgumentCaptor<DeliveryProcessedEvent> eventCaptor = ArgumentCaptor.forClass(DeliveryProcessedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.DELIVERY_FAIL_ROUTING_KEY),
                eventCaptor.capture()
        );
        DeliveryProcessedEvent published = eventCaptor.getValue();
        assertFalse(published.riderAssigned());
        assertEquals("RIDER_NOT_FOUND", published.reason());
        assertEquals(18.0, published.amount());
    }

    @Test
    @DisplayName("Idempotenza: Scarta eventi duplicati di pagamento già presi in carico")
    void shouldIgnoreDuplicateDeliveryRequests() {
        // GIVEN: chiave già presente nella tabella di de-duplicazione
        PaymentProcessedEvent duplicateEvent = new PaymentProcessedEvent(
                "order-deliv-dup",
                true,
                "Alessandro",
                "SUFFICIENT_FUNDS",
                15.0
        );
        when(processedMessageRepository.existsById("DELIVERY_order-deliv-dup")).thenReturn(true);

        // WHEN
        deliveryConsumer.handleDelivery(duplicateEvent);

        // THEN: nessuna scrittura su DB e nessun messaggio inviato al broker
        verify(deliveryRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
