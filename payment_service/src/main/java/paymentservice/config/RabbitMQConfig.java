package paymentservice.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";

    // Routing keys in ingresso
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String DELIVERY_FAIL_ROUTING_KEY = "delivery.failed";

    // Routing keys in uscita
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.successful";
    public static final String PAYMENT_FAIL_ROUTING_KEY = "payment.failed";
    public static final String PAYMENT_REFUNDED_ROUTING_KEY = "payment.refunded";

    // Code
    public static final String PENDING_ORDERS_QUEUE = "payment.pending.orders.queue";
    public static final String DELIVERY_FAILED_QUEUE = "payment.delivery.failed.queue";

    @Bean
    public Queue pendingOrdersQueue() {
        return new Queue(PENDING_ORDERS_QUEUE, false);
    }

    @Bean
    public Queue deliveryFailedQueue() {
        return new Queue(DELIVERY_FAILED_QUEUE, false);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Binding bindingPendingOrders(Queue pendingOrdersQueue, TopicExchange exchange) {
        return BindingBuilder
                .bind(pendingOrdersQueue)
                .to(exchange)
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding bindingDeliveryFailed(Queue deliveryFailedQueue, TopicExchange exchange) {
        return BindingBuilder
                .bind(deliveryFailedQueue)
                .to(exchange)
                .with(DELIVERY_FAIL_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

}
