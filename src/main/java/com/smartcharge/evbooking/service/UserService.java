package com.smartcharge.evbooking.service;

import com.smartcharge.evbooking.domain.Role;
import com.smartcharge.evbooking.domain.User;
import com.smartcharge.evbooking.domain.enums.RoleName;
import com.smartcharge.evbooking.repository.RoleRepository;
import com.smartcharge.evbooking.repository.UserRepository;
import com.smartcharge.evbooking.service.exception.EmailAlreadyUsedException;
import com.smartcharge.evbooking.service.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerDriver(String fullName, String email, String rawPassword) {
        String normalised = email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalised)) {
            throw new EmailAlreadyUsedException(normalised);
        }
        User user = new User(fullName.trim(), normalised, passwordEncoder.encode(rawPassword));
        Role driverRole = roleRepository.findByName(RoleName.ROLE_DRIVER)
            .orElseThrow(() -> new IllegalStateException("ROLE_DRIVER missing — check Flyway migrations."));
        user.addRole(driverRole);
        return userRepository.save(user);
    }

    public User registerAdmin(String fullName, String email, String rawPassword) {
        String normalised = email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalised)) {
            throw new EmailAlreadyUsedException(normalised);
        }
        User user = new User(fullName.trim(), normalised, passwordEncoder.encode(rawPassword));
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
            .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN missing — check Flyway migrations."));
        user.addRole(adminRole);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public long countAdmins() {
        return userRepository.countByRole(RoleName.ROLE_ADMIN);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("User with email " + email));
    }
}
