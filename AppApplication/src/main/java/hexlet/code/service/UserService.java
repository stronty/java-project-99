package hexlet.code.service;

import hexlet.code.dto.UserCreateDTO;
import hexlet.code.dto.UserDTO;
import hexlet.code.dto.UserUpdateDTO;
import hexlet.code.mapper.UserMapper;
import hexlet.code.model.User;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDTO> getAll() {
        return userRepository.findAll().stream().map(userMapper::map).toList();
    }

    @Transactional(readOnly = true)
    public UserDTO get(Long id) {
        return userMapper.map(find(id));
    }

    @Transactional
    public UserDTO create(UserCreateDTO dto) {
        checkEmailFree(dto.getEmail(), null);
        var user = userMapper.map(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        return userMapper.map(userRepository.save(user));
    }

    @Transactional
    public UserDTO update(Long id, UserUpdateDTO dto) {
        var user = find(id);
        if (dto.getEmail() != null) {
            checkEmailFree(dto.getEmail(), id);
        }
        userMapper.update(dto, user);
        if (dto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        return userMapper.map(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id);
        }
        if (taskRepository.existsByAssigneeId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User has tasks");
        }
        userRepository.deleteById(id);
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    private void checkEmailFree(String email, Long selfId) {
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(selfId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            }
        });
    }
}