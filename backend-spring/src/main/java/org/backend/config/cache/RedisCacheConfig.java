package org.backend.config.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            @Value("${dashboard.cache.analytic-ttl:30m}") Duration analyticTtl,
            @Value("${dashboard.cache.operational-ttl:2m}") Duration operationalTtl,
            @Value("${dashboard.cache.stale-ttl:24h}") Duration staleTtl,
            @Value("${dashboard.cache.churn-summary-ttl:10m}") Duration churnSummaryTtl) {
        ObjectMapper cacheObjectMapper = new ObjectMapper();
        cacheObjectMapper.registerModule(new JavaTimeModule());
        cacheObjectMapper.activateDefaultTyping(
                cacheObjectMapper.getPolymorphicTypeValidator(),
                // Cache values are read back as Object. Dashboard snapshots are
                // records (final), so the root type must be present as well.
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        valueSerializer));

        Map<String, RedisCacheConfiguration> caches = new LinkedHashMap<>();
        caches.put("dashboardAnalyticCache", defaults.entryTtl(analyticTtl));
        caches.put("dashboardOperationalCache", defaults.entryTtl(operationalTtl));
        caches.put("dashboardStaleCache", defaults.entryTtl(staleTtl));
        caches.put("churnSummaryCache", defaults.entryTtl(churnSummaryTtl));
        // Kept temporarily for rollout compatibility with existing diagnostics.
        caches.put("dashboardCache", defaults.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(caches)
                .enableStatistics()
                .build();
    }
}
