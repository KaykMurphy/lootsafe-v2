package com.lootsafe.mapper;


import com.lootsafe.dto.request.UserRequestDTO;
import com.lootsafe.dto.response.UserResponseDTO;
import com.lootsafe.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDTO toResponse(User user);
    User toEntity(UserRequestDTO request);

    /*
    * ex: target.setName(request.name());
    */

    void updateEntity(@MappingTarget User target,
                      UserRequestDTO request);

}
