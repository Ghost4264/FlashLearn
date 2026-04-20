package com.flashlearn.service.impl;

import com.flashlearn.service.AiAbuseProtectionService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AiAbuseProtectionServiceImpl implements AiAbuseProtectionService {

    private final Cache<String, MinuteWindowCounter> perMinuteCounters = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(2))
            .maximumSize(200_000)
            .build();

    private final Cache<String, AtomicInteger> perDayCounters = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofDays(2))
            .maximumSize(500_000)
            .build();

    private final Cache<String, Long> lastRequestEpochMs = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(500_000)
            .build();

    @Value("${app.ai.limits.requests-per-minute:10}")
    private int requestsPerMinute;

    @Value("${app.ai.limits.requests-per-day:100}")
    private int requestsPerDay;

    @Value("${app.ai.limits.min-seconds-between-requests:3}")
    private int minSecondsBetweenRequests;

    @Override
    public void validateGenerationAllowed(Long userId) {
        String key = userId != null ? "u:" + userId : "anon";
        validateCooldown(key);
        validatePerMinute(key);
        validatePerDay(key);
    }

    private void validateCooldown(String key) {
        long now = System.currentTimeMillis();
        Long prev = lastRequestEpochMs.getIfPresent(key);
        if (prev != null && now - prev < minSecondsBetweenRequests * 1000L) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Слишком часто. Подождите пару секунд перед следующей AI-генерацией"
            );
        }
        lastRequestEpochMs.put(key, now);
    }

    private void validatePerMinute(String key) {
        long currentMinute = Instant.now().getEpochSecond() / 60;
        MinuteWindowCounter counter = perMinuteCounters.asMap().compute(key, (k, existing) -> {
            if (existing == null || existing.getMinuteEpoch() != currentMinute) {
                MinuteWindowCounter fresh = new MinuteWindowCounter();
                fresh.setMinuteEpoch(currentMinute);
                fresh.setCounter(new AtomicInteger(1));
                return fresh;
            }
            existing.getCounter().incrementAndGet();
            return existing;
        });
        if (counter != null && counter.getCounter().get() > requestsPerMinute) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Превышен лимит AI-запросов в минуту"
            );
        }
    }

    private void validatePerDay(String key) {
        String dayKey = key + ":" + LocalDate.now(ZoneOffset.UTC);
        AtomicInteger count = perDayCounters.asMap().computeIfAbsent(dayKey, k -> new AtomicInteger(0));
        if (count.incrementAndGet() > requestsPerDay) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Превышен суточный лимит AI-запросов"
            );
        }
    }

    @Data
    private static class MinuteWindowCounter {
        private long minuteEpoch;
        private AtomicInteger counter;
    }
}
