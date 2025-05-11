package faang.school.accountservice.publisher;

import faang.school.accountservice.event.CreateAccountEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateAccountPublisher {

    @Value("${spring.data.redis.channel.create-account}")
    private String topic;

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(CreateAccountEvent message) {
        redisTemplate.convertAndSend(topic, message);
    }

}
