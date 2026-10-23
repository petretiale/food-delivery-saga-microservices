package paymentservice.consumer;

import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import paymentservice.config.RabbitMQConfig;
import paymentservice.event.DeliveryProcessedEvent;
import paymentservice.event.OrderReceivedEvent;
import paymentservice.event.PaymentProcessedEvent;
import paymentservice.model.Payment;
import paymentservice.model.PaymentStatus;
import paymentservice.model.ProcessedMessage;
import paymentservice.repository.PaymentRepository;
import paymentservice.repository.ProcessedMessageRepository;

import java.time.LocalDateTime;

@Component
public class PaymentConsumer {

    private final RabbitTemplate rabbitTemplate;
    private final PaymentRepository paymentRepository;
    private final ProcessedMessageRepository processedMessageRepository;

    public PaymentConsumer(RabbitTemplate rabbitTemplate, PaymentRepository repository, ProcessedMessageRepository processedMessageRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.paymentRepository = repository;
        this.processedMessageRepository = processedMessageRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.PENDING_ORDERS_QUEUE)
    @Transactional
    public void receiveOrder(OrderReceivedEvent order) {

        String deduplicationKey = "PAYMENT_" + order.getOrderID();
        if (processedMessageRepository.existsById(deduplicationKey)) {
            System.out.println("⚠️ Messaggio già processato per ordine: " + order.getOrderID() + ". Lo scarto!");
            return;
        }

        System.out.println("==================================================");
        System.out.println("[PAYMENT SERVICE] Ricevuto ordine da elaborare:");
        System.out.println(" - ID Ordine: " + order.getOrderID());
        System.out.println(" - Cliente:   " + order.getCustomerName());
        System.out.println(" - Importo:   €" + order.getAmount());
        System.out.println("==================================================");

        if (order.getAmount() <= 50) {
            Payment payment = new Payment(
                    order.getOrderID(),
                    order.getCustomerName(),
                    order.getAmount(),
                    PaymentStatus.SUCCESS,
                    "SUFFICIENT_FUNDS"
            );
            paymentRepository.save(payment);

            PaymentProcessedEvent paymentProcessedEvent = new PaymentProcessedEvent(
                    order.getOrderID(),
                    true,
                    order.getCustomerName(),
                    "SUFFICIENT_FUNDS",
                    order.getAmount()
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY,
                    paymentProcessedEvent
            );
        } else {
            Payment payment = new Payment(
                    order.getOrderID(),
                    order.getCustomerName(),
                    order.getAmount(),
                    PaymentStatus.FAILED,
                    "INSUFFICIENT_FUNDS"
            );
            paymentRepository.save(payment);

            PaymentProcessedEvent paymentProcessedEvent = new PaymentProcessedEvent(
                    order.getOrderID(),
                    false,
                    "INSUFFICENT_FUNDS",
                    order.getCustomerName(),
                    order.getAmount()
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.PAYMENT_FAIL_ROUTING_KEY,
                    paymentProcessedEvent
            );
        }
        // 2. SALVATAGGIO DELLA CHIAVE: registriamo che questo ordine è stato gestito
        ProcessedMessage message = new ProcessedMessage(
                deduplicationKey,
                "ORDER_CREATED",
                LocalDateTime.now()
        );
        processedMessageRepository.save(message);
        System.out.println("🔒 [IDEMPOTENZA] Chiave salvata su DB: " + deduplicationKey);
    }

    @RabbitListener(queues = RabbitMQConfig.DELIVERY_FAILED_QUEUE)
    public void handleDeliveryFailure(DeliveryProcessedEvent event) {

        String deduplicationKey = "REFUND_" + event.getOrderID();

        // Controllo se il rimborso è già stato registrato
        if (processedMessageRepository.existsById(deduplicationKey)) {
            System.out.println("⚠️  [IDEMPOTENZA] Rimborso già elaborato per ordine: " + event.getOrderID() + ". Messaggio scartato!");
            return;
        }

        System.out.println("==================================================");
        System.out.println(" Rimborso per mancata consegna!");
        System.out.println(" - ID Ordine: " + event.getOrderID());
        System.out.println(" - Importo:   €" + event.getAmount());
        System.out.println("==================================================");

        paymentRepository.findById(event.getOrderID()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setReason("REFUND_DELIVERY_FAILED: " + event.getReason());
            paymentRepository.save(payment);
        });

        PaymentProcessedEvent refundEvent = new PaymentProcessedEvent(
                event.getOrderID(),
                false,
                null,
                "PAYMENT_REFUNDED",
                event.getAmount()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.PAYMENT_REFUNDED_ROUTING_KEY,
                refundEvent
        );
        // Registriamo anche la chiave del rimborso
        ProcessedMessage message = new ProcessedMessage(
                deduplicationKey,
                "DELIVERY_FAILED_REFUND",
                LocalDateTime.now()
        );
        processedMessageRepository.save(message);
        System.out.println("🔒 [IDEMPOTENZA] Chiave salvata su DB: " + deduplicationKey);
    }
}
