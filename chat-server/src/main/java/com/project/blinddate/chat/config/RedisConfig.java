package com.project.blinddate.chat.config;

import com.project.blinddate.chat.service.ChatRedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisConfig {

    // 일반 채팅 메시지용 Topic
    @Bean
    public ChannelTopic channelTopic() {
        return new ChannelTopic("chatroom");
    }

    // 읽음 이벤트용 Topic
    @Bean
    public ChannelTopic readEventTopic() {
        return new ChannelTopic("chatroom-read");
    }

    // 읽지 않은 메시지 뱃지 업데이트용 Topic
    @Bean
    public ChannelTopic unreadBadgeTopic() {
        return new ChannelTopic("unread-badge");
    }

    // 일반 채팅 메시지 리스너
    @Bean
    public MessageListenerAdapter listenerAdapter(ChatRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    // 읽음 이벤트 리스너
    @Bean
    public MessageListenerAdapter readEventListenerAdapter(ChatRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onReadEvent");
    }

    // 읽지 않은 메시지 뱃지 업데이트 리스너
    @Bean
    public MessageListenerAdapter unreadBadgeListenerAdapter(ChatRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onUnreadBadge");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter,
            MessageListenerAdapter readEventListenerAdapter,
            MessageListenerAdapter unreadBadgeListenerAdapter,
            ChannelTopic channelTopic,
            ChannelTopic readEventTopic,
            ChannelTopic unreadBadgeTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, channelTopic);
        container.addMessageListener(readEventListenerAdapter, readEventTopic);
        container.addMessageListener(unreadBadgeListenerAdapter, unreadBadgeTopic);
        return container;
    }
}
