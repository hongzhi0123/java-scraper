package com.example.entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "tpp")
@Data
@RequiredArgsConstructor
public class Tpp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String authId;
    private String natId;
    
    // Store roles as a set of enums → mapped to join table
    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @CollectionTable(
        name = "tpp_roles",
        joinColumns = @JoinColumn(name = "tpp_id")
    )
    @Enumerated(EnumType.STRING)  // Stores "READ", not ordinal
    @Column(name = "role_name")
    private Set<Role> roles = new HashSet<>();

        // Ensure defensive copy
    public Set<Role> getRoles() {
        return roles == null ? Collections.emptySet() : Set.copyOf(roles);
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
    }
}
