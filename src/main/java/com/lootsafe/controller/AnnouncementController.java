package com.lootsafe.controller;

import com.lootsafe.dto.request.AnnouncementRequestDTO;
import com.lootsafe.dto.response.AnnouncementResponseDTO;
import com.lootsafe.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponseDTO createAnnouncement(
            @RequestHeader("X-User-Id") UUID sellerId,
            @RequestBody @Valid AnnouncementRequestDTO request) {
        return announcementService.createAnnouncement(sellerId, request);
    }

    @GetMapping("/{token}")
    public AnnouncementResponseDTO getAnnouncementByToken(@PathVariable String token) {
        return announcementService.getAnnouncementByToken(token);
    }

    @PutMapping("/{id}")
    public AnnouncementResponseDTO updateAnnouncement(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @RequestBody @Valid AnnouncementRequestDTO request) {
        return announcementService.updateAnnouncement(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelAnnouncement(@RequestHeader("X-User-Id") UUID userId,
                                   @PathVariable UUID id) {
        announcementService.cancelAnnouncement(userId, id);
    }
}