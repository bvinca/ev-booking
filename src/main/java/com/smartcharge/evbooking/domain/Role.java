package com.smartcharge.evbooking.domain;

import com.smartcharge.evbooking.domain.enums.RoleName;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 20)
    private RoleName name;

    protected Role() { /* JPA */ }

    public Role(RoleName name) {
        this.name = name;
    }

    public Short getId() { return id; }
    public RoleName getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return name == role.name;
    }

    @Override
    public int hashCode() { return Objects.hash(name); }
}
