package paymentservice.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

    private static final Logger log = LoggerFactory.getLogger(PaymentConsumer.class);

    private final RabbitTemplate rabbitTemplate;
    private final PaymentRepository paymentRepository;
    private final ProcessedMessageRepository processedMessageRepository;

    public PaymentConsumer(RabbitTemplate rabbitTemplate,
                           PaymentRepository repository,
                           ProcessedMessageRepository processedMessageRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.paymentRepository = repository;
        this.processedMessageRepository = processedMessageRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.PENDING_ORDERS_QUEUE)
    @Transactional
    public void receiveOrder(OrderReceivedEvent order) {
        String deduplicationKey = "PAYMENT_" + order.orderID();

       if (isAlreadyProcessed(deduplicationKey)) {
           return;
       }

        log.info(" [PAYMENT] Elaborazione ordine: {} (Cliente: {}, Importo: €{})",
                order.orderID(), order.customerName(), order.amount());

        boolean isApproved = order.amount() <= 50.0;
        PaymentStatus status = isApproved ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        String reason = isApproved ? "SUFFICIENT_FUNDS" : "INSUFFICIENT_FUNDS";
        String routingKey = isApproved ? RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY : RabbitMQConfig.PAYMENT_FAIL_ROUTING_KEY;

        paymentRepository.save(new Payment(
                order.orderID(),
                order.customerName(),
                order.amount(),
                status,
                reason
        ));

        markAsProcessed(deduplicationKey, "ORDER_CREATED");

        PaymentProcessedEvent event = new PaymentProcessedEvent(
                order.orderID(),
                isApproved,
                order.customerName(),
                reason,
                order.amount()
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, routingKey, event);

        log.info("[PAYMENT] Ordine {} completato con esito: {}", order.orderID(), status);
    }

    @RabbitListener(queues = RabbitMQConfig.DELIVERY_FAILED_QUEUE)
    @Transactional // Aggiunto per garantire atomicità anche nel rimborso
    public void handleDeliveryFailure(DeliveryProcessedEvent event) {
        String deduplicationKey = "REFUND_" + event.orderID();

       if (isAlreadyProcessed(deduplicationKey)) {
           return;
       }

        log.info(" [REFUND] Rimborso per mancata consegna - Ordine: {}, Importo: €{}", event.orderID(), event.amount());

        paymentRepository.findById(event.orderID()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setReason("REFUND_DELIVERY_FAILED: " + event.reason());
            paymentRepository.save(payment);
        });

        markAsProcessed(deduplicationKey, "DELIVERY_FAILED_REFUND");

        PaymentProcessedEvent refundEvent = new PaymentProcessedEvent(
                event.orderID(),
                false,
                null, // todo: togliere questo null
                "PAYMENT_REFUNDED",
                event.amount()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.PAYMENT_REFUNDED_ROUTING_KEY,
                refundEvent
        );
        log.info("[REFUND] Storno registrato ed evento emesso per ordine: {}", event.orderID());
    }

    private boolean isAlreadyProcessed(String key) {
        if (processedMessageRepository.existsById(key)) {
            log.warn("[IDEMPOTENZA] Evento {} già elaborato. Messaggio duplicato scartato!", key);
            return true;
        }
        return false;
    }

    private void markAsProcessed(String key, String eventType) {
        processedMessageRepository.save(new ProcessedMessage(key, eventType, LocalDateTime.now()));
    }
}
