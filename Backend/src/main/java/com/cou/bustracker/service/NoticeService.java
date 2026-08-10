package com.cou.bustracker.service;

import com.cou.bustracker.dto.request.CreateNoticeRequest;
import com.cou.bustracker.dto.response.NoticeResponse;
import com.cou.bustracker.entity.Notice;
import com.cou.bustracker.exception.ResourceNotFoundException;
import com.cou.bustracker.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public List<NoticeResponse> getActiveNotices() {
        List<Notice> notices = noticeRepository.findActiveNotices(LocalDateTime.now());
        return notices.stream()
                .map(this::mapToNoticeResponse)
                .collect(Collectors.toList());
    }

    public List<NoticeResponse> getAllNoticesForAdmin() {
        List<Notice> notices = noticeRepository.findAllByOrderByCreatedAtDesc();
        return notices.stream()
                .map(this::mapToNoticeResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public NoticeResponse createNotice(CreateNoticeRequest request) {
        Integer expiryHours = request.getExpiryHours() != null ? request.getExpiryHours() : 24;

        Notice notice = Notice.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .expiryHours(expiryHours)
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusHours(expiryHours))
                .build();

        Notice savedNotice = noticeRepository.save(notice);
        return mapToNoticeResponse(savedNotice);
    }

    @Transactional
    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found with id: " + id));
        noticeRepository.delete(notice);
    }

    @Transactional
    public void deactivateExpiredNotices() {
        List<Notice> expiredNotices = noticeRepository.findActiveNotices(LocalDateTime.now());
        // Notices not returned by findActiveNotices are already expired
    }

    private NoticeResponse mapToNoticeResponse(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .body(notice.getBody())
                .isActive(notice.getIsActive())
                .createdAt(notice.getCreatedAt())
                .expiresAt(notice.getExpiresAt())
                .build();
    }
}
