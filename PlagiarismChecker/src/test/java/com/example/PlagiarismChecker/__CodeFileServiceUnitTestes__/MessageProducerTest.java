package com.example.PlagiarismChecker.__CodeFileServiceUnitTestes__;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.example.PlagiarismChecker.Config.RabbitMQConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(RabbitMQConfig.class) // Import config directly if not picked up via component scan
class RabbitMQConfigTest {

    @Autowired
    private Queue uploadQueue;

    @Autowired
    private TopicExchange uploadExchange;

    @Autowired
    private Binding binding;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void uploadQueue_shouldBeDefined() {
        assertThat(uploadQueue).isNotNull();
        assertThat(uploadQueue.getName()).isEqualTo("uploadQueue");
    }

    @Test
    void uploadExchange_shouldBeDefined() {
        assertThat(uploadExchange).isNotNull();
        assertThat(uploadExchange.getName()).isEqualTo("uploadExchange");
    }

    @Test
    void binding_shouldBindQueueToExchange() {
        assertThat(binding).isNotNull();
        assertThat(binding.getDestination()).isEqualTo("uploadQueue");
        assertThat(binding.getExchange()).isEqualTo("uploadExchange");
    }

    @Test
    void rabbitTemplate_shouldBeConfigured() {
        assertThat(rabbitTemplate).isNotNull();
    }
}
