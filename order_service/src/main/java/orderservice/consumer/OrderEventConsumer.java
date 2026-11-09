package orderservice.consumer;

import jakarta.transaction.Transactional;
import orderservice.config.RabbitMQConfig;
import orderservice.event.DeliveryProcessedEvent;
import orderservice.event.PaymentProcessedEvent;
import orderservice.model.OrderStatus;
import orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private final static Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final OrderRepository orderRepository;

    public OrderEventConsumer(OrderRepository repository) {
        this.orderRepository = repository;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_PAYMENT_EVENTS_QUEUE)
    @Transactional
    public void handlePaymentEvent(PaymentProcessedEvent event) {
        log.info("[ORDER SERVICE] Ricevuto evento pagamento - Ordine: {}, Esito: {}, Motivo: {}",
                event.orderID(),
                event.success() ? "SUCCESSO" : "FALLITO",
                event.reason());

        if (!event.success() && "INSUFFICIENT_FUNDS".equals(event.reason())) {
            updateStatus(event.orderID(), OrderStatus.CANCELLED, "Fondi insufficienti");
        }
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_DELIVERY_EVENTS_QUEUE)
    @Transactional
    public void handleDeliveryResult(DeliveryProcessedEvent deliveryEvent) {
        if (deliveryEvent.riderAssigned()) {
            updateStatus(deliveryEvent.orderID(), OrderStatus.CONFIRMED, "Rider assegnato");
        } else {
            updateStatus(deliveryEvent.orderID(), OrderStatus.CANCELLED, "Consegna fallita: " + deliveryEvent.reason());
        }
    }

    private void updateStatus(String orderId, OrderStatus status, String note) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setOrderStatus(status);
            orderRepository.save(order);
            log.info("[ORDER SERVICE] Ordine {} aggiornato a {} ({})", orderId, status, note);
        });
    }
}
