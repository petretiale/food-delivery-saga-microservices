package orderservice.consumer;

import orderservice.config.RabbitMQConfig;
import orderservice.event.DeliveryProcessedEvent;
import orderservice.event.PaymentProcessedEvent;
import orderservice.model.OrderStatus;
import orderservice.repository.OrderRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    OrderRepository repository;

    public OrderEventConsumer(OrderRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_PROCESSED_QUEUE)
    public void paymentFailure (PaymentProcessedEvent paymentProcessed) {
        System.out.println("[ORDER SERVICE] Ricevuto fallimento pagamento per ordine: " + paymentProcessed.getOrderID());

        repository.findById(paymentProcessed.getOrderID()).ifPresent(order -> {
            order.setOrderStatus(OrderStatus.CANCELLED);
            repository.save(order);
        });
    }

    @RabbitListener(queues = RabbitMQConfig.DELIVERY_PROCESSED_QUEUE)
    public void deliveryResult(DeliveryProcessedEvent deliveryEvent) {
        repository.findById(deliveryEvent.getOrderID()).ifPresent(order -> {
            if (deliveryEvent.isRiderAssigned()) {
                System.out.println("[ORDER SERVICE] Consegna confermata per ordine: " + deliveryEvent.getOrderID());
                order.setOrderStatus(OrderStatus.CONFIRMED);
            } else {
                System.out.println("[ORDER SERVICE] Mancata consegna per ordine: " + deliveryEvent.getOrderID());
                order.setOrderStatus(OrderStatus.CANCELLED);
            }
            repository.save(order);
        });
    }
}
