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

    OrderRepository orderRepository;

    public OrderEventConsumer(OrderRepository repository) {
        this.orderRepository = repository;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_PAYMENT_EVENTS_QUEUE)
    public void paymentFailure (PaymentProcessedEvent event) {
        System.out.println("==================================================");
        System.out.println("💳 [ORDER SERVICE] Ricevuto evento di pagamento:");
        System.out.println(" - ID Ordine: " + event.getOrderID());
        System.out.println(" - Esito:     " + (event.isSuccess() ? "SUCCESSO" : "FALLITO"));
        System.out.println(" - Motivo:    " + event.getReason());
        System.out.println("==================================================");

        if (!event.isSuccess()) {
            // Se il motivo è il fallimento del pagamento iniziale, annulla l'ordine
            // (se fosse PAYMENT_REFUNDED, l'ordine viene già gestito/annullato dalla compensazione di delivery)
            if ("INSUFFICIENT_FUNDS".equals(event.getReason())) {
                orderRepository.findById(event.getOrderID()).ifPresent(order -> {
                    order.setOrderStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);
                    System.out.println("❌ [ORDER SERVICE] Ordine " + event.getOrderID() + " impostato a CANCELLED per fondi insufficienti.");
                });
            }
        } else {
            // Pagamento autorizzato con successo: l'ordine resta in PENDING in attesa dell'esito delivery
            System.out.println("ℹ️ [ORDER SERVICE] Pagamento confermato per ordine " + event.getOrderID() + ". In attesa di assegnazione rider.");
        }
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_DELIVERY_EVENTS_QUEUE)
    public void deliveryResult(DeliveryProcessedEvent deliveryEvent) {
        orderRepository.findById(deliveryEvent.getOrderID()).ifPresent(order -> {
            if (deliveryEvent.isRiderAssigned()) {
                System.out.println("[ORDER SERVICE] Consegna confermata per ordine: " + deliveryEvent.getOrderID());
                order.setOrderStatus(OrderStatus.CONFIRMED);
            } else {
                System.out.println("[ORDER SERVICE] Mancata consegna per ordine: " + deliveryEvent.getOrderID());
                order.setOrderStatus(OrderStatus.CANCELLED);
            }
            orderRepository.save(order);
        });
    }
}
