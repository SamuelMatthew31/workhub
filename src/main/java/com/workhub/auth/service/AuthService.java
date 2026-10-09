package com.workhub.auth.service;

import com.workhub.auth.dto.*;
import com.workhub.auth.entity.RefreshToken;
import com.workhub.auth.entity.User;
import com.workhub.auth.repository.RefreshTokenRepository;
import com.workhub.auth.repository.UserRepository;
import com.workhub.common.exception.ConflictException;
import com.workhub.common.exception.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * FR-AUTH-1: Registrasi user baru
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email already in use");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User user = new User(normalizedEmail, hashedPassword, request.displayName().trim());
        user = userRepository.save(user);

        return issueNewTokenFamily(user);
    }

    /**
     * FR-AUTH-3 & FR-AUTH-6: Login dengan pencegahan account enumeration
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return issueNewTokenFamily(user);
    }

    /**
     * FR-AUTH-4 & FR-AUTH-5: Refresh Token Rotation & Reuse Detection
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.refreshToken());

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // FR-AUTH-5: Reuse Detection
        // Jika token yang sudah berstatus used/revoked dicoba pakai ulang:
        // Cabut SEMUA token dalam satu family karena dicurigai dicuri!
        if (storedToken.isUsed() || storedToken.isRevoked()) {
            refreshTokenRepository.revokeFamily(storedToken.getTokenFamily());
            throw new UnauthorizedException("Compromised token detected. All sessions in this family revoked.");
        }

        // Cek masa kadaluarsa
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Tandai token lama sebagai sudah digunakan (FR-AUTH-4: single-use)
        storedToken.setUsed(true);
        refreshTokenRepository.save(storedToken);

        // Buat refresh token baru dalam family yang sama (Rotasi)
        User user = storedToken.getUser();
        String newRawRefreshToken = UUID.randomUUID().toString();
        String newHashedToken = hashToken(newRawRefreshToken);

        RefreshToken nextToken = new RefreshToken(
                user,
                newHashedToken,
                storedToken.getTokenFamily(),
                Instant.now().plus(30, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(nextToken);

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());

        return new AuthResponse(
                newAccessToken,
                newRawRefreshToken,
                "Bearer",
                new AuthResponse.UserDto(user.getId(), user.getEmail(), user.getDisplayName())
        );
    }

    /**
     * FR-AUTH-4: Logout (cabut token yang aktif)
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    /**
     * Helper: Menerbitkan pasangan token baru (membuat family baru)
     */
    private AuthResponse issueNewTokenFamily(User user) {
        UUID familyId = UUID.randomUUID();
        String rawRefreshToken = UUID.randomUUID().toString();
        String hashedToken = hashToken(rawRefreshToken);

        RefreshToken token = new RefreshToken(
                user,
                hashedToken,
                familyId,
                Instant.now().plus(30, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(token);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                new AuthResponse.UserDto(user.getId(), user.getEmail(), user.getDisplayName())
        );
    }

    /**
     * Helper SHA-256 untuk hashing refresh token sebelum masuk database
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
