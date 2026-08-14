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
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnnouncementService {

    private static final String MSG_ANNOUNCEMENT_NOT_FOUND = "Anúncio não encontrado.";
    private static final String MSG_ANNOUNCEMENT_TOKEN_NOT_FOUND =
            "Anúncio não encontrado ou link inválido.";
    private static final String MSG_ANNOUNCEMENT_OWNER_MISMATCH =
            "Este anúncio não pertence a este usuário.";
    private static final String MSG_ANNOUNCEMENT_NOT_EDITABLE =
            "Este anúncio não pode ser alterado pois já foi finalizado, vendido ou cancelado.";

    private final AnnouncementRepository announcementRepository;
    private final EncryptionService encryptionService;
    private final UserService userService;
    private final AnnouncementMapper announcementMapper;


    @Transactional
    public AnnouncementResponseDTO createAnnouncement(UUID sellerId, AnnouncementRequestDTO request) {
        User seller = userService.findEntityById(sellerId);

        Announcement announcement = announcementMapper.toEntity(request);
        announcement.setSeller(seller);
        announcement.setToken(UUID.randomUUID().toString());
        announcement.setStatus(AnnouncementStatus.ACTIVE);

        String encryptedCredentials = encryptionService.encrypt(request.credentials());
        announcement.setCredentialsEncrypted(encryptedCredentials);

        Announcement savedAnnouncement = announcementRepository.save(announcement);
        return announcementMapper.toResponse(savedAnnouncement);
    }


    //controllers
    public AnnouncementResponseDTO getAnnouncementByToken(String token) {
        Announcement announcement = announcementRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_ANNOUNCEMENT_TOKEN_NOT_FOUND));

        return announcementMapper.toResponse(announcement);
    }

    //services
    public Announcement findEntityByToken(String token) {
        return announcementRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_ANNOUNCEMENT_TOKEN_NOT_FOUND));
    }


    @Transactional
    public AnnouncementResponseDTO updateAnnouncement(UUID userId, UUID announcementId, AnnouncementRequestDTO updated) {

        Announcement existingAnnouncement = findAnnouncementAndValidateOwner(announcementId, userId);

        if (!existingAnnouncement.isEditable()) {
            throw new BusinessException(MSG_ANNOUNCEMENT_NOT_EDITABLE);
        }

        announcementMapper.updateEntity(existingAnnouncement, updated);

        existingAnnouncement.setCredentialsEncrypted(encryptionService.encrypt(updated.credentials()));

        Announcement savedAnnouncement = announcementRepository.save(existingAnnouncement);

        return announcementMapper.toResponse(savedAnnouncement);
    }


    @Transactional
    public AnnouncementResponseDTO cancelAnnouncement(UUID userId, UUID announcementId) {

        Announcement existingAnnouncement = findAnnouncementAndValidateOwner(announcementId, userId);

        existingAnnouncement.cancel();

        Announcement savedAnnouncement = announcementRepository.save(existingAnnouncement);

        return announcementMapper.toResponse(savedAnnouncement);
    }


    private Announcement findAnnouncementAndValidateOwner(UUID announcementId, UUID userId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_ANNOUNCEMENT_NOT_FOUND));

        if (!announcement.getSeller().getId().equals(userId)) {
            throw new UnauthorizedException(MSG_ANNOUNCEMENT_OWNER_MISMATCH);
        }

        return announcement;
    }
}