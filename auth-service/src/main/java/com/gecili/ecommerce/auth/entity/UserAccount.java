package com.gecili.ecommerce.auth.entity;

import com.gecili.ecommerce.common.enums.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A user account. Owned solely by auth-service (this entity lives here, NOT in common-lib —
 * sharing it would couple every service to this schema).
 *
 * <p>The password is stored only as a BCrypt hash. Roles are stored in a collection table.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserAccount {

    @Id private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name")
    private String displayName;

    @Column(nullable = false)
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Optimistic lock guard against concurrent updates. */
    @Version private long version;

    public static UserAccount create(String email, String passwordHash, String displayName) {
        UserAccount u = new UserAccount();
        u.id = UUID.randomUUID();
        u.email = email;
        u.passwordHash = passwordHash;
        u.displayName = displayName;
        u.enabled = true;
        u.roles.add(Role.CUSTOMER);
        u.createdAt = Instant.now();
        return u;
    }
}
