package com.salgosipo.amenity.service;

import com.salgosipo.amenity.dto.AmenityFilter;
import com.salgosipo.amenity.dto.AmenityRequestDTO;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Log4j2
@Component
public class AmenityCalculationQueue implements InitializingBean, DisposableBean {

    private static final long REQUEST_INTERVAL_MILLIS = 1_000L;

    private final AmenityService amenityService;
    private final BlockingQueue<AmenityRequestDTO> queue = new LinkedBlockingQueue<>();
    private final Set<String> pendingKeys = ConcurrentHashMap.newKeySet();
    private ExecutorService executor;

    public AmenityCalculationQueue(@Lazy AmenityService amenityService) {
        this.amenityService = amenityService;
    }

    @Override
    public void afterPropertiesSet() {
        executor = Executors.newSingleThreadExecutor();
        executor.submit(this::process);
    }

    public void enqueue(Integer propertyId, List<AmenityFilter> filters) {
        if (propertyId == null || filters == null) return;

        for (AmenityFilter filter : filters) {
            if (filter == null || filter.getAmenityType() == null || filter.getWalkTimeMinutes() == null) continue;

            String key = propertyId + ":" + filter.getAmenityType();
            if (!pendingKeys.add(key)) continue;

            AmenityFilter queuedFilter = new AmenityFilter();
            queuedFilter.setAmenityType(filter.getAmenityType());
            queuedFilter.setWalkTimeMinutes(filter.getWalkTimeMinutes());

            AmenityRequestDTO request = AmenityRequestDTO.builder()
                    .propertyId(propertyId)
                    .amenities(List.of(queuedFilter))
                    .build();

            queue.offer(request);
        }
    }

    private void process() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                AmenityRequestDTO request = queue.take();
                Integer propertyId = request.getPropertyId();
                Integer amenityType = request.getAmenities().get(0).getAmenityType();
                try {
                    amenityService.getAmenitiesByFilter(request);
                } catch (RuntimeException e) {
                    log.warn("Amenity background calculation failed. PropertyId: {}", propertyId, e);
                } finally {
                    pendingKeys.remove(propertyId + ":" + amenityType);
                }
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
}
