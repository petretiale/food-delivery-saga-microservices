package orderservice.consumer;

import jakarta.transaction.Transactional;
import orderservice.config.RabbitMQConfig;
import orderservice.event.DeliveryProcessedEvent;
import orderservice.event.PaymentProcessedEvent;
import orderservice.model.OrderStatus;
import orderservice.repository.OrderRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private final OrderRepository orderRepository;

    public OrderEventConsumer(OrderRepository repository) {
        this.orderRepository = repository;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_PAYMENT_EVENTS_QUEUE)
    @Transactional
    public void paymentFailure (PaymentProcessedEvent event) {
        System.out.println("==================================================");
        System.out.println("💳 [ORDER SERVICE] Ricevuto evento di pagamento:");
        System.out.println(" - ID Ordine: " + event.orderID());
        System.out.println(" - Esito:     " + (event.success() ? "SUCCESSO" : "FALLITO"));
        System.out.println(" - Motivo:    " + event.reason());
        System.out.println("==================================================");

        if (!event.success()) {
            // Se il motivo è il fallimento del pagamento iniziale, annulla l'ordine
            // (se fosse PAYMENT_REFUNDED, l'ordine viene già gestito/annullato dalla compensazione di delivery)
            if ("INSUFFICIENT_FUNDS".equals(event.reason())) {
                orderRepository.findById(event.orderID()).ifPresent(order -> {
                    order.setOrderStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);
                    System.out.println("❌ [ORDER SERVICE] Ordine " + event.orderID() + " impostato a CANCELLED per fondi insufficienti.");
                });
            }
        } else {
            // Pagamento autorizzato con successo: l'ordine resta in PENDING in attesa dell'esito delivery
            System.out.println("ℹ️ [ORDER SERVICE] Pagamento confermato per ordine " + event.orderID() + ". In attesa di assegnazione rider.");
        }
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_DELIVERY_EVENTS_QUEUE)
    @Transactional
    public void deliveryResult(DeliveryProcessedEvent deliveryEvent) {
        orderRepository.findById(deliveryEvent.orderID()).ifPresent(order -> {
            if (deliveryEvent.riderAssigned()) {
                System.out.println("[ORDER SERVICE] Consegna confermata per ordine: " + deliveryEvent.orderID());
                order.setOrderStatus(OrderStatus.CONFIRMED);
            } else {
                System.out.println("[ORDER SERVICE] Mancata consegna per ordine: " + deliveryEvent.orderID());
                order.setOrderStatus(OrderStatus.CANCELLED);
            }
            orderRepository.save(order);
        });
    }
}
