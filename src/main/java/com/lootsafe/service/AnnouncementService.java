package com.lootsafe.service;

import com.lootsafe.entity.Announcement;
import com.lootsafe.entity.User;
import com.lootsafe.enums.AnnouncementStatus;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.exception.UnauthorizedException;
import com.lootsafe.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final EncryptionService encryptionService;
    private final UserService userService;


    public Announcement createAnnouncement(UUID sellerId, Announcement announcement) {
        User seller = userService.findById(sellerId);
        announcement.setSeller(seller);

        String token = UUID.randomUUID().toString();
        announcement.setToken(token);

        announcement.setStatus(AnnouncementStatus.ACTIVE);

        String encryptedCredentials = encryptionService.encrypt(announcement.getCredentialsEncrypted());
        announcement.setCredentialsEncrypted(encryptedCredentials);

        return announcementRepository.save(announcement);
    }

    public Announcement getAnnouncementByToken(String token) {
        return announcementRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Anúncio não encontrado ou link inválido."));
    }

    public Announcement updateAnnouncement(UUID userId, UUID announcementId, Announcement updated) {
        Announcement existingAnnouncement = findAnnouncementAndValidateOwner(announcementId, userId);

        validateAnnouncementIsEditable(existingAnnouncement,
                "Este anúncio não pode ser alterado pois já foi finalizado, vendido ou cancelado.");

        existingAnnouncement.setTitle(updated.getTitle());
        existingAnnouncement.setDescription(updated.getDescription());
        existingAnnouncement.setNotes(updated.getNotes());
        existingAnnouncement.setPrice(updated.getPrice());

        return announcementRepository.save(existingAnnouncement);
    }

    public Announcement cancelAnnouncement(UUID userId, UUID announcementId) {
        Announcement existingAnnouncement = findAnnouncementAndValidateOwner(announcementId, userId);

        validateAnnouncementIsEditable(existingAnnouncement,
                "Apenas anúncios ativos ou em rascunho podem ser cancelados.");

        existingAnnouncement.setStatus(AnnouncementStatus.CANCELLED);

        return announcementRepository.save(existingAnnouncement);
    }


    private Announcement findAnnouncementAndValidateOwner(UUID announcementId, UUID userId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Anúncio não encontrado."));

        if (!announcement.getSeller().getId().equals(userId)) {
            throw new UnauthorizedException("Este anúncio não pertence a este usuário.");
        }

        return announcement;
    }

    private void validateAnnouncementIsEditable(Announcement announcement, String errorMessage) {
        if (!(announcement.getStatus().equals(AnnouncementStatus.ACTIVE)
                || announcement.getStatus().equals(AnnouncementStatus.DRAFT))) {
            throw new BusinessException(errorMessage);
        }
    }
}