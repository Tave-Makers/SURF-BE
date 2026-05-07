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

}
