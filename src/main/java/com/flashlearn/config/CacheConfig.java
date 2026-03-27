package com.flashlearn.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PUBLIC_DECKS = "publicDecks";
    public static final String PUBLIC_DECK_CATEGORIES = "publicDeckCategories";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.registerCustomCache(PUBLIC_DECKS,
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .build());

        manager.registerCustomCache(PUBLIC_DECK_CATEGORIES,
                Caffeine.newBuilder()
                        .maximumSize(1)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .build());

        return manager;
    }
}
