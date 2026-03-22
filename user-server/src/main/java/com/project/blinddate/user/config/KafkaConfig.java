package com.project.blinddate.user.config;


import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic userInfoUpdatedTopic() {
        return TopicBuilder.name("user-info-updated")
                .partitions(3)
                .replicas(1)  // Single Kafka broker
//                .replicas(3)  // For Kafka cluster
                .build();
    }

//    @Bean
//    public ConsumerFactory<String, ChatMessageEvent> chatMessageConsumerFactory(
//            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
//    ) {
//        JsonDeserializer<ChatMessageEvent> jsonDeserializer =
//                new JsonDeserializer<>(ChatMessageEvent.class, false);
//        jsonDeserializer.addTrustedPackages("*");
//
//        Map<String, Object> props = new HashMap<>();
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, "user-server-chat-message");
//
//        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, ChatMessageEvent> chatMessageKafkaListenerContainerFactory(
//            ConsumerFactory<String, ChatMessageEvent> chatMessageConsumerFactory
//    ) {
//        ConcurrentKafkaListenerContainerFactory<String, ChatMessageEvent> factory =
//                new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(chatMessageConsumerFactory);
//        return factory;
//    }
}


