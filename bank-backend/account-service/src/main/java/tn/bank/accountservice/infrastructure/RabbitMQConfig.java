package tn.bank.accountservice.infrastructure;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.user}")
    private String userExchange;

    @Value("${rabbitmq.queue.user-created}")
    private String userCreatedQueue;

    @Value("${rabbitmq.routingkey.user-created}")
    private String userCreatedRoutingKey;

    @Bean
    public DirectExchange userExchange() {
        return new DirectExchange(userExchange);
    }

    @Bean
    public Queue userCreatedQueue() {
        return new Queue(userCreatedQueue, true);
    }

    @Bean
    public Binding userCreatedBinding(Queue userCreatedQueue, DirectExchange userExchange) {
        return BindingBuilder.bind(userCreatedQueue).to(userExchange).with(userCreatedRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}