package db.crud.mapper;

import db.crud.dto.UserRequest;
import db.crud.dto.UserResponse;
import db.crud.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserRequest request) {
        return new User(request.name(), request.email(), request.age());
    }

    public void updateEntity(User user, UserRequest request) {
        user.setName(request.name());
        user.setEmail(request.email());
        user.setAge(request.age());
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getCreatedAt()
        );
    }
}
