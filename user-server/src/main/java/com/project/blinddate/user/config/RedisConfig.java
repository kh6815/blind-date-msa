package com.project.blinddate.user.config;

import com.project.blinddate.user.service.UserLikeRedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisConfig {

    @Bean
    public ChannelTopic likeNotificationTopic() {
        return new ChannelTopic("user-like-notification");
    }

    @Bean
    public MessageListenerAdapter likeNotificationListenerAdapter(UserLikeRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onLikeNotification");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter likeNotificationListenerAdapter,
            ChannelTopic likeNotificationTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(likeNotificationListenerAdapter, likeNotificationTopic);
        return container;
    }
}
