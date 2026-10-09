package com.workhub.auth.repository;

import com.workhub.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // 1. Cari token berdasarkan string hash-nya
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // 2. Cabut semua token dalam satu family (Reuse Detection - FR-AUTH-5)
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.tokenFamily = :family")
    void revokeFamily(@Param("family") UUID family);

    // 3. Cabut semua token milik seorang user (misal saat logout semua sesi)
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId")
    void revokeAllForUser(@Param("userId") UUID userId);
}
