package com.flashlearn.service;

import com.flashlearn.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    @Scheduled(cron = "${app.auth.refresh-token-cleanup-cron:0 0 * * * *}")
    public void cleanup() {
        int deleted = refreshTokenRepository.deleteExpiredOrRevoked(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Refresh token cleanup removed {} records", deleted);
        }
    }
}
