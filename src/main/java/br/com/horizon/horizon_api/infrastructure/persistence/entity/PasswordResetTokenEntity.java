package br.com.horizon.horizon_api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
public class PasswordResetTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "token_hash", length = 255, unique = true)
    private String tokenHash;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "used")
    private Boolean used;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
