package com.example.PlagiarismChecker.Config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
	
    @Bean
    public Queue uploadQueue() {
        return new Queue("uploadQueue", true, false, false); // Durable queue
    }
    
    @Bean
    public CachingConnectionFactory rabbitConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost("172.22.224.194");
        factory.setPort(5672);
        factory.setUsername("admin");
        factory.setPassword("root");
        
        // Performance optimizations
        factory.setChannelCacheSize(25); // Increase channel cache
//        factory.setConnectionCacheSize(5); // Multiple connections
        factory.setChannelCheckoutTimeout(5000); // 5 second timeout
        
        return factory;
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        
        // Performance optimizations
        template.setUseDirectReplyToContainer(false); // Faster for fire-and-forget
        
        System.out.println("RabbitTemplate configured with Jackson2JsonMessageConverter");
        return template;
    }
    
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * Configure listener container for parallel processing
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            CachingConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        
        // Parallel consumer settings
        factory.setConcurrentConsumers(5); // Start with 5 consumers
        factory.setMaxConcurrentConsumers(10); // Scale up to 10 consumers
        factory.setPrefetchCount(1); // Process 1 message at a time per consumer
        
        // Performance settings
        factory.setDefaultRequeueRejected(false); // Don't requeue failed messages
        factory.setMissingQueuesFatal(false);
        
        return factory;
    }
}