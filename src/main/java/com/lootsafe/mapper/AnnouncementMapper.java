package com.lootsafe.mapper;

import com.lootsafe.dto.request.AnnouncementRequestDTO;
import com.lootsafe.dto.response.AnnouncementResponseDTO;
import com.lootsafe.entity.Announcement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AnnouncementMapper {

    AnnouncementResponseDTO toResponse(Announcement announcement);

    @Mapping(target = "credentialsEncrypted", source = "credentials")
    Announcement toEntity(AnnouncementRequestDTO request);

    @Mapping(target = "credentialsEncrypted", source = "credentials")
    void updateEntity(@MappingTarget Announcement target,
                      AnnouncementRequestDTO request);

}