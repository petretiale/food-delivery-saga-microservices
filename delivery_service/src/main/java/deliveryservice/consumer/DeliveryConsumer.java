package deliveryservice.consumer;

import deliveryservice.config.RabbitMQConfig;
import deliveryservice.event.DeliveryProcessedEvent;
import deliveryservice.event.PaymentProcessedEvent;
import deliveryservice.model.Delivery;
import deliveryservice.repository.DeliveryRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeliveryConsumer {

    private final RabbitTemplate rabbitTemplate;
    private final DeliveryRepository deliveryRepository;

    public DeliveryConsumer(RabbitTemplate rabbitTemplate, DeliveryRepository deliveryRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.deliveryRepository = deliveryRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleDelivery(PaymentProcessedEvent event) {
        System.out.println("==================================================");
        System.out.println(" [DELIVERY SERVICE] Richiesta rider ricevuta:");
        System.out.println(" - ID Ordine: " + event.getOrderID());
        System.out.println(" - Cliente:   " + event.getCustomerName());

        if ("NoRider".equalsIgnoreCase(event.getCustomerName())) {
            Delivery delivery = new Delivery(
                    event.getOrderID(),
                    event.getCustomerName(),
                    false,
                    null,
                    "FAILED"
            );
            deliveryRepository.save(delivery);

            DeliveryProcessedEvent failureEvent = new DeliveryProcessedEvent(
                    event.getOrderID(),
                    false,
                    "RIDER_NOT_FOUND",
                    event.getAmount()
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.DELIVERY_FAIL_ROUTING_KEY,
                    failureEvent
            );
        } else {
            Delivery delivery = new Delivery(
                    event.getOrderID(),
                    event.getCustomerName(),
                    true,
                    "Marco (Rider)",
                    "ASSIGNED"
            );
            deliveryRepository.save(delivery);

            DeliveryProcessedEvent successEvent = new DeliveryProcessedEvent(event.getOrderID(),
                    true,
                    "RIDER_ASSIGNED",
                    event.getAmount()
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.DELIVERY_SUCCESS_ROUTING_KEY,
                    successEvent);
        }
    }
}
