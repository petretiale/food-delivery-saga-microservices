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
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String PAYMENT_PROCESSED_QUEUE = "payment.processed.queue";
    public static final String DELIVERY_PROCESSED_QUEUE = "delivery.processed.queue";
    public static final String PAYMENT_FAIL_ROUTING_KEY = "payment.failed";

    @Bean
    public Queue paymentProcessedQueue() {
        return new Queue(PAYMENT_PROCESSED_QUEUE, false);
    }

    @Bean
    public Queue deliveryProcessedQueue() {
        return new Queue(DELIVERY_PROCESSED_QUEUE, false);
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
    public Binding bindingPaymentProcessed(Queue paymentProcessedQueue, TopicExchange exchange) {
        return BindingBuilder
                .bind(paymentProcessedQueue)
                .to(exchange)
                .with(PAYMENT_FAIL_ROUTING_KEY);
    }

    @Bean
    public Binding bindingDeliveryProcessed(Queue deliveryProcessedQueue, TopicExchange exchange) {
        return BindingBuilder
                .bind(deliveryProcessedQueue)
                .to(exchange())
                .with("delivery.*");
    }


}
