package com.ecommerce.id;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for registering the distributed Snowflake ID generator bean.
 */
@Configuration
@EnableConfigurationProperties(IdGeneratorProperties.class)
public class IdGeneratorConfig {

    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    @ConditionalOnProperty(prefix = "app.id-generator", name = "enabled", havingValue = "true", matchIfMissing = true)
    public IdGenerator idGenerator(IdGeneratorProperties properties) {
        return new SnowflakeIdGenerator(
                properties.getDatacenterId(),
                properties.getWorkerId(),
                properties.getCustomEpochMs(),
                properties.getMaxBackwardMs()
        );
    }
}
