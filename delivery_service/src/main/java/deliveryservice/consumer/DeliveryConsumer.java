package deliveryservice.consumer;

import deliveryservice.config.RabbitMQConfig;
import deliveryservice.event.DeliveryProcessedEvent;
import deliveryservice.event.PaymentProcessedEvent;
import deliveryservice.model.Delivery;
import deliveryservice.model.ProcessedMessage;
import deliveryservice.repository.DeliveryRepository;
import deliveryservice.repository.ProcessedMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class DeliveryConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryConsumer.class);

    private final RabbitTemplate rabbitTemplate;
    private final DeliveryRepository deliveryRepository;
    private final ProcessedMessageRepository processedMessageRepository;

    public DeliveryConsumer(RabbitTemplate rabbitTemplate, DeliveryRepository deliveryRepository, ProcessedMessageRepository processedMessageRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.deliveryRepository = deliveryRepository;
        this.processedMessageRepository = processedMessageRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    @Transactional
    public void handleDelivery(PaymentProcessedEvent event) {
        String deduplicationKey = "DELIVERY_" + event.orderID();

        if (processedMessageRepository.existsById(deduplicationKey)) {
            log.warn("[IDEMPOTENZA] Ordine {} già processato da Delivery. Messaggio duplicato scartato!", event.orderID());
            return;
        }

        log.info(" [DELIVERY] Richiesta rider ricevuta - Ordine: {}, Cliente: {}",
                event.orderID(), event.customerName());

        boolean noRiderFound = "NoRider".equalsIgnoreCase(event.customerName());
        boolean riderAssigned = !noRiderFound;
        String riderName = riderAssigned ? "Marco (Rider)" : null;
        String status = riderAssigned ? "ASSIGNED" : "FAILED";
        String reason = riderAssigned ? "RIDER_ASSIGNED" : "RIDER_NOT_FOUND";
        String routingKey = riderAssigned ? RabbitMQConfig.DELIVERY_SUCCESS_ROUTING_KEY : RabbitMQConfig.DELIVERY_FAIL_ROUTING_KEY;

        // 1. Salva la consegna su DB
        deliveryRepository.save(new Delivery(
                event.orderID(),
                event.customerName(),
                riderAssigned,
                riderName,
                status
        ));

        // 2. Registra la chiave di de-duplicazione
        processedMessageRepository.save(new ProcessedMessage(
                deduplicationKey,
                "DELIVERY_PROCESSED",
                LocalDateTime.now()
        ));

        // 3. Emetti l'evento per la Saga
        DeliveryProcessedEvent deliveryEvent = new DeliveryProcessedEvent(
                event.orderID(),
                riderAssigned,
                reason,
                event.amount()
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, routingKey, deliveryEvent);

        if (riderAssigned) {
            log.info("[DELIVERY] Rider {} assegnato per ordine: {}", riderName, event.orderID());
        } else {
            log.warn("[DELIVERY] Nessun rider disponibile per ordine: {}. Inviato evento di fallimento.", event.orderID());
        }
    }
}
