package com.smartcharge.evbooking.repository;

import com.smartcharge.evbooking.domain.User;
import com.smartcharge.evbooking.domain.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :role")
    long countByRole(RoleName role);
}
