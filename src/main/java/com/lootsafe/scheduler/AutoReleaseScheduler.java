package com.lootsafe.scheduler;

import com.lootsafe.service.AutoReleaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoReleaseScheduler {

    private final AutoReleaseService autoReleaseService;

    @Scheduled(fixedDelayString = "${inspection.auto-release-interval-ms:60000}")
    @SchedulerLock(name = "autoReleaseExpiredTransactionsLock", lockAtLeastFor = "15s", lockAtMostFor = "5m")
    public void scheduleAutoRelease() {
        log.debug("Iniciando scheduler de auto-release de transações expiradas...");

        autoReleaseService.processAutoReleases();
    }

}
