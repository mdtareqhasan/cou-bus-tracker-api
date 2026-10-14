package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.UpdateTrackerLinkRequest;
import com.cou.bustracker.entity.Bus;
import com.cou.bustracker.entity.TrackerLink;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.BusRepository;
import com.cou.bustracker.repository.TrackerLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TrackerLinkService {

    private final TrackerLinkRepository trackerLinkRepository;
    private final BusRepository busRepository;

    @Transactional
    public void updateTrackerLink(Long busId, UpdateTrackerLinkRequest request, String updatedBy) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));

        TrackerLink trackerLink = trackerLinkRepository.findByBusId(busId)
                .orElse(TrackerLink.builder()
                        .bus(bus)
                        .build());

        trackerLink.setTrackerUrl(request.getTrackerUrl());
        trackerLink.setExpiresAt(request.getExpiresAt());
        trackerLink.setUpdatedBy(updatedBy);

        trackerLinkRepository.save(trackerLink);
    }

    public String getTrackerUrlByBusId(Long busId) {
        return trackerLinkRepository.findByBusId(busId)
                .map(TrackerLink::getTrackerUrl)
                .orElse(null);
    }
}
