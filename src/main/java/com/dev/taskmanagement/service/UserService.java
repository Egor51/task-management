package com.dev.taskmanagement.service;

import com.dev.taskmanagement.dto.auth.RegisterRequest;
import com.dev.taskmanagement.exception.ResourceNotFoundException;
import com.dev.taskmanagement.model.Role;
import com.dev.taskmanagement.model.User;
import com.dev.taskmanagement.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис для управления пользователями в системе.
 * Обеспечивает основные операции с пользователями, включая создание новых пользователей,
 * поиск пользователей по различным критериям и управление их данными.
 * Реализует кэширование для оптимизации производительности часто запрашиваемых данных.
 *
 * <p>Основные возможности:</p>
 * <ul>
 *   <li>Создание новых пользователей с шифрованием паролей</li>
 *   <li>Поиск пользователей по ID и email</li>
 *   <li>Кэширование данных пользователей</li>
 *   <li>Управление ролями пользователей</li>
 * </ul>
 *
 * @see User
 * @see Role
 * @see PasswordEncoder
 */
@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Создает нового пользователя в системе.
     * При создании пользователя проверяется уникальность email,
     * пароль шифруется, и пользователю назначается роль ROLE_USER.
     * После создания пользователя кэш очищается.
     *
     * @param request Данные для регистрации пользователя
     * @return Созданный пользователь
     * @throws IllegalArgumentException если email уже занят
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public User createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already taken");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(Role.ROLE_USER);

        return userRepository.save(user);
    }

    /**
     * Получает пользователя по его ID.
     * Результат кэшируется для оптимизации производительности.
     *
     * @param id ID пользователя
     * @return Найденный пользователь
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        log.debug("Fetching user from database by id: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    /**
     * Получает список всех пользователей системы.
     * Метод не кэширует результаты, так как список может часто меняться.
     *
     * @return Список всех пользователей
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Получает пользователя по его email.
     * Результат кэшируется для оптимизации производительности.
     * Этот метод часто используется при аутентификации.
     *
     * @param email Email пользователя
     * @return Найденный пользователь
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#email")
    public User getUserByEmail(String email) {
        log.debug("Fetching user from database by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
