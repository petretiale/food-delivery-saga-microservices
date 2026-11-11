package paymentservice.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import paymentservice.config.RabbitMQConfig;
import paymentservice.event.DeliveryProcessedEvent;
import paymentservice.event.OrderReceivedEvent;
import paymentservice.event.PaymentProcessedEvent;
import paymentservice.model.Payment;
import paymentservice.model.PaymentStatus;
import paymentservice.model.ProcessedMessage;
import paymentservice.repository.PaymentRepository;
import paymentservice.repository.ProcessedMessageRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentConsumerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProcessedMessageRepository processedMessageRepository;

    private PaymentConsumer paymentConsumer;

    @BeforeEach
    void setUp() {
        paymentConsumer = new PaymentConsumer(rabbitTemplate, paymentRepository, processedMessageRepository);
    }

    @Test
    @DisplayName("Regola Business: Autorizza pagamento quando l'importo è <= 50€")
    void shouldAuthorizePaymentWhenAmountIsUnderOrEqualTo50() {
        // GIVEN: arriva un ordine da 30€ che non è mai stato processato prima
        OrderReceivedEvent event = new OrderReceivedEvent("ord-1", "Alessandro", 30.0);
        when(processedMessageRepository.existsById("PAYMENT_ord-1")).thenReturn(false);

        // WHEN: il consumer riceve l'evento da RabbitMQ
        paymentConsumer.receiveOrder(event);

        // THEN:
        // 1. Verifica che nel DB sia stato salvato il pagamento con stato SUCCESS
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(PaymentStatus.SUCCESS, savedPayment.getStatus());
        assertEquals(30.0, savedPayment.getAmount());

        // 2. Verifica che sia stata salvata la chiave di idempotenza nel DB
        verify(processedMessageRepository).save(any(ProcessedMessage.class));

        // 3. Verifica l'evento emesso su RabbitMQ
        ArgumentCaptor<PaymentProcessedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentProcessedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY),
                eventCaptor.capture()
        );
        PaymentProcessedEvent published = eventCaptor.getValue();
        assertTrue(published.success());
        assertEquals("Alessandro", published.customerName());
        assertEquals("SUFFICIENT_FUNDS", published.reason());
    }

    @Test
    @DisplayName("Regola Business: Rifiuta pagamento quando l'importo supera 50€")
    void shouldRejectPaymentWhenAmountExceeds50() {
        // GIVEN: ordine da 75€ mai processato prima
        OrderReceivedEvent event = new OrderReceivedEvent("ord-2", "Alessandro", 75.0);
        when(processedMessageRepository.existsById("PAYMENT_ord-2")).thenReturn(false);

        // WHEN
        paymentConsumer.receiveOrder(event);

        // THEN:
        // 1. Verifica stato FAILED nel DB
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(PaymentStatus.FAILED, paymentCaptor.getValue().getStatus());

        // 2. Verifica che l'evento emesso sia con routing key payment.failed
        ArgumentCaptor<PaymentProcessedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentProcessedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.PAYMENT_FAIL_ROUTING_KEY),
                eventCaptor.capture()
        );
        PaymentProcessedEvent published = eventCaptor.getValue();
        assertFalse(published.success());
        assertEquals("INSUFFICIENT_FUNDS", published.reason());
    }

    @Test
    @DisplayName("Idempotenza: Scarta messaggi duplicati dello stesso ordine")
    void shouldIgnoreDuplicatePaymentMessages() {
        // GIVEN: la chiave esiste già nel DB (messaggio duplicato da rete o broker)
        OrderReceivedEvent duplicateEvent = new OrderReceivedEvent("ord-duplicate", "Alessandro", 20.0);
        when(processedMessageRepository.existsById("PAYMENT_ord-duplicate")).thenReturn(true);

        // WHEN
        paymentConsumer.receiveOrder(duplicateEvent);

        // THEN: non deve salvare né inviare messaggi su RabbitMQ
        verify(paymentRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Saga Compensazione: Rimborsa il pagamento quando riceve delivery.failed")
    void shouldCompensateAndRefundWhenDeliveryFails() {
        // GIVEN: la consegna è fallita per assenza rider su un ordine pagato in precedenza
        String orderId = "ord-refund";
        DeliveryProcessedEvent deliveryFailed = new DeliveryProcessedEvent(orderId, false, "RIDER_NOT_FOUND", 25.0);
        when(processedMessageRepository.existsById("REFUND_" + orderId)).thenReturn(false);

        Payment existingPayment = new Payment(orderId, "Alessandro", 25.0, PaymentStatus.SUCCESS, "SUFFICIENT_FUNDS");
        when(paymentRepository.findById(orderId)).thenReturn(Optional.of(existingPayment));

        // WHEN: il consumer riceve la notifica di fallimento consegna
        paymentConsumer.handleDeliveryFailure(deliveryFailed);

        // THEN:
        // 1. Il pagamento originario passa a REFUNDED
        assertEquals(PaymentStatus.REFUNDED, existingPayment.getStatus());
        verify(paymentRepository).save(existingPayment);

        // 2. Viene salvata la chiave di idempotenza per il rimborso
        verify(processedMessageRepository).save(any(ProcessedMessage.class));

        // 3. Viene emesso l'evento payment.refunded su RabbitMQ
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.PAYMENT_REFUNDED_ROUTING_KEY),
                any(PaymentProcessedEvent.class)
        );
    }
}
