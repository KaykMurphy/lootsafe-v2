package com.lootsafe.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public javax.cache.CacheManager jCacheManager() {
        var provider = Caching.getCachingProvider();
        var cacheManager = provider.getCacheManager();
        if (cacheManager.getCache("buckets") == null) {
            cacheManager.createCache("buckets", new MutableConfiguration<>()
                    .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.HOURS, 1))));
        }
        return cacheManager;
    }
}