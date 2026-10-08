package hexlet.code.mapper;

import hexlet.code.dto.UserCreateDTO;
import hexlet.code.dto.UserDTO;
import hexlet.code.dto.UserUpdateDTO;
import hexlet.code.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = JsonNullableMapper.class)
public abstract class UserMapper {
    public abstract UserDTO toDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordDigest", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract User toEntity(UserCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordDigest", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract void update(UserUpdateDTO dto, @MappingTarget User user);
}