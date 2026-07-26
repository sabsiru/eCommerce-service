package kr.hhplus.be.server.infrastructure.redis;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kr.hhplus.be.server.application.product.PopularProductInfo;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(om);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer)
                );

        long baseSeconds   = Duration.ofMinutes(10).getSeconds();
        long jitterSeconds = ThreadLocalRandom.current().nextLong(-60, +60);
        Duration randomizedTtl = Duration.ofSeconds(baseSeconds + jitterSeconds);

        // GenericJackson2JsonRedisSerializer는 캐시 값을 타입 힌트 없이(Object.class로) 읽기 때문에
        // List<T> 같은 컬렉션을 최상위로 캐싱하면 원소 타입을 복원하지 못하고 LinkedHashMap이 되어버린다.
        // popularProducts 캐시는 항상 List<PopularProductInfo>이므로 구체적인 JavaType을 아는
        // Jackson2JsonRedisSerializer로 직렬화해서 이 문제를 피한다.
        JavaType popularProductsType = om.getTypeFactory()
                .constructCollectionType(List.class, PopularProductInfo.class);
        Jackson2JsonRedisSerializer<List<PopularProductInfo>> popularProductsSerializer =
                new Jackson2JsonRedisSerializer<>(om, popularProductsType);

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put(
                "popularProducts",
                defaultConfig.entryTtl(randomizedTtl)
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(popularProductsSerializer)
                        )
        );

        return RedisCacheManager.builder(cf)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configs)
                .build();
    }
}
