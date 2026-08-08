package com.jbrmmg.home.email;

import com.jbrmmg.home.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

@Component
public class EmailRateLimiter {
    private static final Logger LOG = LoggerFactory.getLogger(EmailRateLimiter.class);

    private final MailForwarder forwarder;
    private final int ratePerMinute;

    private final Map<String, Deque<Long>> sentTimestamps = new HashMap<>();
    private final Map<String, Queue<byte[]>> pendingQueue = new HashMap<>();

    public EmailRateLimiter(MailForwarder forwarder, ApplicationProperties applicationProperties) {
        this.forwarder = forwarder;
        this.ratePerMinute = applicationProperties.getEmail().getRatePerMinute();
        long intervalMs = 60_000L / ratePerMinute;
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::flushQueue, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public synchronized void submit(byte[] rawMessage, String recipient) {
        pruneTimestamps(recipient);
        Deque<Long> timestamps = sentTimestamps.computeIfAbsent(recipient, k -> new ArrayDeque<>());

        if (timestamps.size() < ratePerMinute) {
            timestamps.addLast(System.currentTimeMillis());
            forwarder.forward(rawMessage, recipient);
            LOG.info("Email forwarded immediately to {}.", recipient);
        } else {
            pendingQueue.computeIfAbsent(recipient, k -> new LinkedList<>()).add(rawMessage);
            LOG.info("Rate limit reached for {}, email queued ({} pending).", recipient, pendingQueue.get(recipient).size());
        }
    }

    private synchronized void flushQueue() {
        for (Map.Entry<String, Queue<byte[]>> entry : pendingQueue.entrySet()) {
            String recipient = entry.getKey();
            Queue<byte[]> queue = entry.getValue();
            if (queue.isEmpty()) continue;

            pruneTimestamps(recipient);
            Deque<Long> timestamps = sentTimestamps.computeIfAbsent(recipient, k -> new ArrayDeque<>());
            int slots = ratePerMinute - timestamps.size();

            for (int i = 0; i < slots && !queue.isEmpty(); i++) {
                byte[] msg = queue.poll();
                timestamps.addLast(System.currentTimeMillis());
                forwarder.forward(msg, recipient);
                LOG.info("Queued email forwarded to {} ({} remaining).", recipient, queue.size());
            }
        }
    }

    private void pruneTimestamps(String recipient) {
        Deque<Long> timestamps = sentTimestamps.get(recipient);
        if (timestamps == null) return;
        long cutoff = System.currentTimeMillis() - 60_000L;
        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.pollFirst();
        }
    }
}
