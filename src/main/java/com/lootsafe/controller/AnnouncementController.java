package com.lootsafe.controller;

import com.lootsafe.dto.request.AnnouncementRequestDTO;
import com.lootsafe.dto.response.AnnouncementResponseDTO;
import com.lootsafe.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SELLER')")
    public AnnouncementResponseDTO createAnnouncement(@RequestBody @Valid AnnouncementRequestDTO request,
                                                      @AuthenticationPrincipal UUID currentUserId) {
        return announcementService.createAnnouncement(currentUserId, request);
    }

    @GetMapping
    public List<AnnouncementResponseDTO> getActiveAnnouncements() {
        return announcementService.getActiveAnnouncements();
    }

    @GetMapping("/me")
    public List<AnnouncementResponseDTO> getMyAnnouncements(@AuthenticationPrincipal UUID currentUserId) {
        return announcementService.getMyAnnouncements(currentUserId);
    }

    @GetMapping("/{token}")
    public AnnouncementResponseDTO getAnnouncementByToken(@PathVariable String token) {
        return announcementService.getAnnouncementByToken(token);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public AnnouncementResponseDTO updateAnnouncement(@PathVariable UUID id,
                                                      @RequestBody @Valid AnnouncementRequestDTO request,
                                                      @AuthenticationPrincipal UUID currentUserId) {
        return announcementService.updateAnnouncement(currentUserId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SELLER')")
    public void cancelAnnouncement(@PathVariable UUID id,
                                   @AuthenticationPrincipal UUID currentUserId) {
        announcementService.cancelAnnouncement(currentUserId, id);
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SELLER')")
    public void cancelAnnouncementPost(@PathVariable UUID id,
                                       @AuthenticationPrincipal UUID currentUserId) {
        announcementService.cancelAnnouncement(currentUserId, id);
    }

    @PostMapping("/{id}/cancel-reservation")
    @PreAuthorize("hasRole('SELLER')")
    public AnnouncementResponseDTO cancelReservation(@PathVariable UUID id,
                                                     @AuthenticationPrincipal UUID currentUserId) {
        return announcementService.cancelReservation(id, currentUserId);
    }
}