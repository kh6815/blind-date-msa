package com.project.blinddate.chat.config;

import com.project.blinddate.chat.dto.ChatMessageEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
/**
 * Kafka 설정 클래스
 *
 * Kafka Producer 및 Consumer 설정을 담당합니다.
 * 일반 문자열(String) 메시지와 채팅 메시지 이벤트(ChatMessageEvent)를 처리하기 위한
 * 팩토리와 템플릿을 빈으로 등록합니다.
 */
public class KafkaConfig {

    /**
     * String Producer Factory 빈 등록
     *
     * 일반 문자열 메시지를 Kafka로 발행하기 위한 Producer 설정을 정의합니다.
     * Key와 Value 모두 String 타입을 사용합니다.
     *
     * @param bootstrapServers Kafka 브로커 주소 목록
     * @return ProducerFactory<String, String>
     */
    @Bean
    public NewTopic chatMessageSaveTopic() {
        return TopicBuilder.name("chat-message-save")
                .partitions(3)
                .replicas(1)  // Single Kafka broker
//                .replicas(3)  // For Kafka cluster
                .build();
    }

    @Bean
    public NewTopic chatMessageSaveDltTopic() {
        return TopicBuilder.name("chat-message-save.DLT")
                .partitions(3)
                .replicas(1)  // Single Kafka broker
//                .replicas(3)  // For Kafka cluster
                .build();
    }

    @Bean
    public ProducerFactory<String, String> stringProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * String KafkaTemplate 빈 등록
     *
     * 문자열 메시지 전송을 위한 템플릿입니다.
     * stringProducerFactory를 사용하여 생성됩니다.
     *
     * @param stringProducerFactory 위에서 정의한 String ProducerFactory
     * @return KafkaTemplate<String, String>
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(
            ProducerFactory<String, String> stringProducerFactory
    ) {
        return new KafkaTemplate<>(stringProducerFactory);
    }

    /**
     * ChatMessageEvent Producer Factory 빈 등록
     *
     * 채팅 메시지 이벤트 객체를 Kafka로 발행하기 위한 Producer 설정을 정의합니다.
     * Key는 String, Value는 ChatMessageEvent 객체(JSON 직렬화)를 사용합니다.
     *
     * @param bootstrapServers Kafka 브로커 주소 목록
     * @return ProducerFactory<String, ChatMessageEvent>
     */
    @Bean
    public ProducerFactory<String, ChatMessageEvent> chatMessageProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * ChatMessageEvent KafkaTemplate 빈 등록
     *
     * 실제 코드에서 주입받아 메시지를 전송하는 데 사용되는 템플릿입니다.
     * chatMessageProducerFactory를 사용하여 생성됩니다.
     *
     * @param chatMessageProducerFactory 위에서 정의한 ProducerFactory
     * @return KafkaTemplate<String, ChatMessageEvent>
     */
    @Bean
    public KafkaTemplate<String, ChatMessageEvent> chatMessageKafkaTemplate(
            ProducerFactory<String, ChatMessageEvent> chatMessageProducerFactory
    ) {
        return new KafkaTemplate<>(chatMessageProducerFactory);
    }

    /**
     * ChatMessageEvent Consumer Factory 빈 등록
     *
     * Kafka 토픽에서 메시지를 소비(Consume)하기 위한 설정을 정의합니다.
     * JSON 형태의 메시지를 ChatMessageEvent 객체로 역직렬화합니다.
     *
     * @param bootstrapServers Kafka 브로커 주소 목록
     * @param groupId Consumer 그룹 ID
     * @return ConsumerFactory<String, ChatMessageEvent>
     */
    @Bean
    public ConsumerFactory<String, ChatMessageEvent> chatMessageConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id:chat-group}") String groupId
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        // 신뢰할 수 있는 패키지 설정 (보안 이슈 방지)
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(ChatMessageEvent.class));
    }

    /**
     * Kafka Listener Container Factory 빈 등록
     *
     * @KafkaListener 어노테이션이 붙은 메서드에서 메시지를 처리할 수 있도록 리스너 컨테이너를 생성하는 팩토리입니다.
     *
     * @param chatMessageConsumerFactory 위에서 정의한 ConsumerFactory
     * @return ConcurrentKafkaListenerContainerFactory<String, ChatMessageEvent>
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatMessageEvent> chatMessageKafkaListenerContainerFactory(
            ConsumerFactory<String, ChatMessageEvent> chatMessageConsumerFactory,
            KafkaTemplate<String, ChatMessageEvent> chatMessageKafkaTemplate
    ) {
        ConcurrentKafkaListenerContainerFactory<String, ChatMessageEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(chatMessageConsumerFactory);

        // 실패 메시지는 chat-message-save.DLT 토픽으로 전송, 최대 3회 1초 간격 재시도
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(chatMessageKafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}


