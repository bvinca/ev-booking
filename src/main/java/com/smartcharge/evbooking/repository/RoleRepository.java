package com.smartcharge.evbooking.repository;

import com.smartcharge.evbooking.domain.Role;
import com.smartcharge.evbooking.domain.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Short> {
    Optional<Role> findByName(RoleName name);
}
