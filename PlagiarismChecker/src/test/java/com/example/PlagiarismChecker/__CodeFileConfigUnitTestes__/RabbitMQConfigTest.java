package com.example.PlagiarismChecker.__CodeFileConfigUnitTestes__;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.example.PlagiarismChecker.Config.RabbitMQConfig;

import static org.junit.jupiter.api.Assertions.*;

class RabbitMQConfigTest {

    @Test
    void testUploadQueueBean() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(RabbitMQConfig.class);
        Queue queue = context.getBean("uploadQueue", Queue.class);
        assertNotNull(queue, "UploadQueue bean should be created");
        assertEquals("uploadQueue", queue.getName(), "Queue name should be uploadQueue");
        assertTrue(queue.isDurable(), "Queue should be durable");
        context.close();
    }

    @Test
    void testRabbitConnectionFactoryBean() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(RabbitMQConfig.class);
        CachingConnectionFactory factory = context.getBean(CachingConnectionFactory.class);
        assertNotNull(factory, "CachingConnectionFactory bean should be created");
        assertEquals("192.168.101.60", factory.getHost(), "Host should be 192.168.101.60");
        assertEquals(5672, factory.getPort(), "Port should be 5672");
        context.close();
    }

    @Test
    void testRabbitTemplateBean() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(RabbitMQConfig.class);
        RabbitTemplate template = context.getBean(RabbitTemplate.class);
        assertNotNull(template, "RabbitTemplate bean should be created");
        assertNotNull(template.getMessageConverter(), "RabbitTemplate should have a message converter");
        assertTrue(template.getMessageConverter() instanceof Jackson2JsonMessageConverter, "Message converter should be Jackson2JsonMessageConverter");
        context.close();
    }

    @Test
    void testJsonMessageConverterBean() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(RabbitMQConfig.class);
        Jackson2JsonMessageConverter converter = context.getBean(Jackson2JsonMessageConverter.class);
        assertNotNull(converter, "Jackson2JsonMessageConverter bean should be created");
        context.close();
    }
}
