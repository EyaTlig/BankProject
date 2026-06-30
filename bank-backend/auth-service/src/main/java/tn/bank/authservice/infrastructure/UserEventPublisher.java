package tn.bank.authservice.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.bank.authservice.application.UserCreatedEvent;

@Service
@RequiredArgsConstructor
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.user}")
    private String userExchange;

    @Value("${rabbitmq.routingkey.user-created}")
    private String userCreatedRoutingKey;

    public void publishUserCreated(UserCreatedEvent event) {
        rabbitTemplate.convertAndSend(userExchange, userCreatedRoutingKey, event);
    }
}