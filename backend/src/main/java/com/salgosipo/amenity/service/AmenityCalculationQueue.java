package com.salgosipo.amenity.service;

import com.salgosipo.amenity.dto.AmenityFilter;
import com.salgosipo.amenity.dto.AmenityRequestDTO;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AmenityCalculationQueue implements InitializingBean, DisposableBean {

    private static final long REQUEST_INTERVAL_MILLIS = 1_000L;

    private final AmenityService amenityService;
    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();
    private ExecutorService executor;

    public AmenityCalculationQueue(@Lazy AmenityService amenityService) {
        this.amenityService = amenityService;
    }

    @Override
    public void afterPropertiesSet() {
        executor = Executors.newSingleThreadExecutor();
        executor.submit(this::process);
    }

    public String createJob(List<Task> tasks) {
        String jobId = UUID.randomUUID().toString();
        JobState state = new JobState(tasks.size());
        jobs.put(jobId, state);
        tasks.forEach(task -> queue.offer(task.withJobId(jobId)));
        return jobId;
    }

    public String getJobStatus(String jobId) {
        JobState state = jobs.get(jobId);
        return state == null ? null : state.status();
    }

    private void process() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Task task = queue.take();
                try {
                    amenityService.getAmenitiesByFilter(task.request());
                } catch (RuntimeException ignored) {
                }

                JobState state = jobs.get(task.jobId());
                if (state != null) state.finish();
                Thread.sleep(REQUEST_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void destroy() {
        if (executor != null) executor.shutdownNow();
    }

    public record Task(Integer propertyId, AmenityFilter filter, String jobId) {
        Task withJobId(String value) {
            return new Task(propertyId, filter, value);
        }

        AmenityRequestDTO request() {
            return AmenityRequestDTO.builder().propertyId(propertyId).amenities(List.of(filter)).build();
        }

    }

    private static class JobState {
        private final int totalCount;
        private final AtomicInteger completedCount = new AtomicInteger();

        private JobState(int totalCount) {
            this.totalCount = totalCount;
        }

        private void finish() {
            completedCount.incrementAndGet();
        }

        private String status() {
            int completed = completedCount.get();
            return completed >= totalCount ? "COMPLETED" : "PROCESSING";
        }
    }
}
