package com.tavemakers.surf.domain.notification.repository;

import com.tavemakers.surf.domain.notification.entity.DeviceToken;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findAllByMemberIdAndEnabledTrue(Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM DeviceToken d WHERE d.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);

    /** 로그아웃 시 해당 기기의 토큰 삭제 — 본인 소유 토큰만 지운다 (타인 토큰 삭제 방지) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM DeviceToken d WHERE d.memberId = :memberId AND d.token = :token")
    void deleteByMemberIdAndToken(@Param("memberId") Long memberId, @Param("token") String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DeviceToken d SET d.enabled = false WHERE d.token IN :tokens")
    void disableAllByTokenIn(@Param("tokens") List<String> tokens);

}
