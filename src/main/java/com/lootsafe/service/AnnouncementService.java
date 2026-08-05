package com.lootsafe.service;

import com.lootsafe.dto.request.AnnouncementRequestDTO;
import com.lootsafe.dto.response.AnnouncementResponseDTO;
import com.lootsafe.entity.Announcement;
import com.lootsafe.entity.User;
import com.lootsafe.enums.AnnouncementStatus;
import com.lootsafe.exception.BusinessException;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.exception.UnauthorizedException;
import com.lootsafe.mapper.AnnouncementMapper;
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
    private final AnnouncementMapper announcementMapper;


    public AnnouncementResponseDTO createAnnouncement(UUID sellerId, AnnouncementRequestDTO request) {
        User seller = userService.findEntityById(sellerId);

        Announcement announcement = announcementMapper.toEntity(request);
        announcement.setSeller(seller);
        announcement.setToken(UUID.randomUUID().toString());
        announcement.setStatus(AnnouncementStatus.ACTIVE);

        String encryptedCredentials = encryptionService.encrypt(request.credentialsEncrypted());
        announcement.setCredentialsEncrypted(encryptedCredentials);

        Announcement savedAnnouncement = announcementRepository.save(announcement);
        return announcementMapper.toResponse(savedAnnouncement);
    }


    //controllers
    public AnnouncementResponseDTO getAnnouncementByToken(String token) {
        Announcement announcement = announcementRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Anúncio não encontrado ou link inválido."));

        return announcementMapper.toResponse(announcement);
    }

    //services
    public Announcement findEntityByToken(String token) {
        return announcementRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Anúncio não encontrado ou link inválido."));
    }


    public AnnouncementResponseDTO updateAnnouncement(UUID userId, UUID announcementId, AnnouncementRequestDTO updated) {

        Announcement existingAnnouncement = findAnnouncementAndValidateOwner(announcementId, userId);

        validateAnnouncementIsEditable(existingAnnouncement,
                "Este anúncio não pode ser alterado pois já foi finalizado, vendido ou cancelado.");

        announcementMapper.updateEntity(existingAnnouncement, updated);

        Announcement savedAnnouncement = announcementRepository.save(existingAnnouncement);

        return announcementMapper.toResponse(savedAnnouncement);
    }


    public AnnouncementResponseDTO cancelAnnouncement(UUID userId, UUID announcementId) {

        Announcement existingAnnouncement = findAnnouncementAndValidateOwner(announcementId, userId);

        validateAnnouncementIsEditable(existingAnnouncement,
                "Apenas anúncios ativos ou em rascunho podem ser cancelados.");

        existingAnnouncement.setStatus(AnnouncementStatus.CANCELLED);

        Announcement savedAnnouncement = announcementRepository.save(existingAnnouncement);

        return announcementMapper.toResponse(savedAnnouncement);
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