package db.crud.service;

import db.crud.dto.UserRequest;
import db.crud.dto.UserResponse;
import db.crud.event.UserEvent;
import db.crud.event.UserOperation;
import db.crud.exception.EmailAlreadyExistsException;
import db.crud.exception.UserNotFoundException;
import db.crud.mapper.UserMapper;
import db.crud.model.User;
import db.crud.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.eventPublisher = eventPublisher;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public UserResponse getUser(Long id) {
        return userMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        User saved = userRepository.save(userMapper.toEntity(request));
        eventPublisher.publishEvent(new UserEvent(UserOperation.CREATED, saved.getEmail()));
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = findOrThrow(id);

        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new EmailAlreadyExistsException(request.email());
        }

        userMapper.updateEntity(user, request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findOrThrow(id);
        userRepository.delete(user);
        eventPublisher.publishEvent(new UserEvent(UserOperation.DELETED, user.getEmail()));
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
