package orderservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";

    // Routing keys pubblicate da order_service
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    // Routing patterns ascoltati da order_service
    public static final String PAYMENT_EVENTS_ROUTING_KEY = "payment.*";
    public static final String DELIVERY_EVENTS_ROUTING_KEY = "delivery.*";

    // Code di order_service
    public static final String ORDER_PAYMENT_EVENTS_QUEUE = "order.payment.events.queue";
    public static final String ORDER_DELIVERY_EVENTS_QUEUE = "order.delivery.events.queue";


    @Bean
    public Queue paymentProcessedQueue() {
        return new Queue(ORDER_PAYMENT_EVENTS_QUEUE, false);
    }

    @Bean
    public Queue deliveryProcessedQueue() {
        return new Queue(ORDER_DELIVERY_EVENTS_QUEUE, false);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(ORDER_EXCHANGE);
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

    @Bean
    public Binding bindingOrderPaymentEvents(Queue paymentProcessedQueue, TopicExchange exchange) {
        return BindingBuilder
                .bind(paymentProcessedQueue)
                .to(exchange)
                .with(PAYMENT_EVENTS_ROUTING_KEY);
    }

    @Bean
    public Binding bindingOrderDeliveryEvents(Queue deliveryProcessedQueue, TopicExchange exchange) {
        return BindingBuilder
                .bind(deliveryProcessedQueue)
                .to(exchange)
                .with(DELIVERY_EVENTS_ROUTING_KEY);
    }


}
