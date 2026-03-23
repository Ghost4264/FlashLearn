package com.flashlearn.mapper;

import com.flashlearn.dto.response.UserResponse;
import com.flashlearn.entity.Role;
import com.flashlearn.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct маппер для пользователей
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Конвертирует User в UserResponse
     */
    @Mapping(source = "role", target = "role", qualifiedByName = "roleToString")
    UserResponse toResponse(User user);

    /**
     * Преобразует enum Role в строковое представление для DTO
     */
    @Named("roleToString")
    default String roleToString(Role role) {
        return role.name();
    }
}
