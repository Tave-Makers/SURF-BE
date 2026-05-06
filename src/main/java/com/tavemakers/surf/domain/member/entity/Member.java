package com.tavemakers.surf.domain.member.entity;

import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.dto.request.ProfileUpdateReqDTO;
import com.tavemakers.surf.domain.member.exception.InvalidMemberInfoException;
import com.tavemakers.surf.domain.member.exception.MisMatchPasswordException;
import com.tavemakers.surf.domain.member.exception.PasswordNotSettingException;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import com.tavemakers.surf.domain.member.dto.request.MemberSignupReqDTO;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.global.util.SecurityUtils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import com.tavemakers.surf.domain.member.entity.enums.Part;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.BatchSize;


@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_member_provider_provider_id",
                        columnNames = {"provider", "provider_id"}
                )
        }
)
@Getter
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자 protected 설정
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    /** @deprecated provider/providerId 모델 도입 (D1) — 호출자 일소 후 후속 PR(Step 7)에서 제거. */
    @Deprecated
    @Column
    private Long kakaoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Provider provider;

    @Column(nullable = false, length = 64)
    private String providerId;

    /** Apple 첫 로그인 시 닉네임 미제공 (D5) → nullable 허용. 가입 폼(applySignup)에서 채워진다. */
    @Column
    private String name;

    /** Apple 계정 탈퇴(revoke) 시 사용. Web 로그인 최초 코드 교환 시 저장, 탈퇴 시 null 처리. */
    @Column(name = "apple_refresh_token", length = 1024)
    private String appleRefreshToken;

    private String profileImageUrl;

    private String university;

    private String graduateSchool;

    @Column(nullable = false, unique = true) // 이메일은 고유해야 함
    private String email;

    @Embedded
    private Password password;

    private String phoneNumber;

    @Column(length = 256)
    private String selfIntroduction;

    @Column(length = 1024)
    private String link;

    private Boolean phoneNumberPublic=false;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<Track> tracks = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status = MemberStatus.WAITING; // 회원 상태; // 회원 상태 (가입중, 대기중, 승인)

    @Enumerated(EnumType.STRING)
    private MemberRole role; // 역할 (루트, 회장, 매니저, 회원)

    @Enumerated(EnumType.STRING)
    private MemberType memberType; // OB, YB 구분

    private boolean activityStatus; // 활동/비활동 여부

    @Column(nullable = false)
    private boolean termsAgreed = false;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    private LocalDateTime deletedAt;

    public boolean isYB() {
        return memberType == MemberType.YB;
    }

    public boolean isActive() {
        return activityStatus;
    }

    public boolean isApproved() {
        return status == MemberStatus.APPROVED;
    }

    public boolean isRegistering() {
        return status == MemberStatus.REGISTERING;
    }

    @Builder
    public Member(Provider provider,
                  String providerId,
                  Long kakaoId,
                  String name,
                  String profileImageUrl,
                  String university,
                  String graduateSchool,
                  String email,
                  String phoneNumber,
                  Boolean phoneNumberPublic,
                  MemberStatus status,
                  MemberRole role,
                  MemberType memberType,
                  boolean activityStatus) {
        this.provider = provider;
        this.providerId = providerId;
        this.kakaoId = kakaoId;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.university = university;
        this.graduateSchool = graduateSchool;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.phoneNumberPublic = phoneNumberPublic;
        this.status = status != null ? status : MemberStatus.WAITING;
        this.role = role != null ? role : MemberRole.MEMBER;
        this.memberType = memberType != null ? memberType : MemberType.YB;
        this.activityStatus = activityStatus;
        this.tracks = new ArrayList<>();
    }

    /**
     * OAuth provider 정보로 REGISTERING 상태의 회원을 생성한다 (D1, D5).
     * Kakao 는 닉네임이 일반적으로 채워져 있고, Apple 은 첫 로그인 시 nickname/profileImageUrl 이 null 일 수 있다.
     */
    public static Member createRegisteringFromOAuth(Provider provider, OAuthUserInfoDTO info) {
        if (provider == null) {
            throw new IllegalStateException("provider 는 필수입니다.");
        }
        if (info.email() == null || info.email().isBlank()) {
            throw new IllegalStateException(provider + " 계정의 이메일 권한이 필요합니다.");
        }
        if (info.oauthId() == null || info.oauthId().isBlank()) {
            throw new IllegalStateException(provider + " 식별자(oauthId)가 비어 있습니다.");
        }

        Long legacyKakaoId = null;
        if (provider == Provider.KAKAO) {
            try {
                legacyKakaoId = Long.parseLong(info.oauthId());
            } catch (NumberFormatException e) {
                throw new InvalidMemberInfoException(
                        "Provider.KAKAO oauthId가 유효한 숫자 형식이 아닙니다: " + info.oauthId());
            }
        }

        return Member.builder()
                .provider(provider)
                .providerId(info.oauthId())
                .kakaoId(legacyKakaoId)
                .name(info.nickname())
                .email(info.email())
                .phoneNumberPublic(false)
                .profileImageUrl(info.profileImageUrl())
                .status(MemberStatus.REGISTERING)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
    }

    public void applySignup(MemberSignupReqDTO req, String normalizedEmail, String normalizedPhone) {
        this.name = req.getName();
        this.university = req.getUniversity();
        this.graduateSchool = req.getGraduateSchool();
        this.email = normalizedEmail;
        this.phoneNumber = normalizedPhone;

        // 기본 정책 보정 (비어있을 수 있는 값들)
        if (this.role == null) this.role = MemberRole.MEMBER;
        if (this.memberType == null) this.memberType = MemberType.YB;
        this.activityStatus = true;

        // 상태 전이: REGISTERING -> WAITING (또는 정책상 APPROVED)
        if (this.status == MemberStatus.REGISTERING) {
            this.status = MemberStatus.WAITING;
        }

        //트랙 저장
        if (req.getTracks() != null) {
            req.getTracks().forEach(t ->
                    this.addTrack(t.getGeneration(), t.getPart())
            );
        }
    }

    /**
     * ===== [도메인 행위 메서드] =====
     */
    public void approve() {
        this.status = MemberStatus.APPROVED;
    }

    /** 회원 기수 상태와 활동 여부를 현재 정책에 맞게 동기화합니다. */
    public void syncGenerationStatus(MemberType memberType, boolean activityStatus) {
        this.memberType = memberType;
        this.activityStatus = activityStatus;
    }

    /** 약관 동의 처리 */
    public void agreeTerms() {
        this.termsAgreed = true;
    }

    public void reject() {
        this.status = MemberStatus.REJECTED;
    }

    /**
     * ===== [연관관계 편의 메서드] =====
     */
    // 트랙 추가 (기수+파트로 생성)
    public void addTrack(Integer generation, Part part) {
        boolean exists = this.tracks.stream()
                .anyMatch(t -> t.getGeneration().equals(generation));

        if (exists) return; // 같은 기수 이미 있으면 추가 안 함

        Track track = new Track(generation, part);
        track.setMember(this); // 여기서만 add 수행
    }

    /** 댓글의 멘션 기능에서 회원들의 기수별로 정렬하기 위한 메서드 */
    public Integer getFirstGeneration() {
        if (tracks == null || tracks.isEmpty()) return null;

        // 가장 먼저 활동한 기수
        return tracks.stream()
                .map(Track::getGeneration)
                .min(Integer::compareTo)
                .orElse(null);
    }

    //프로필 수정하기
    public void updateProfile(ProfileUpdateReqDTO dto) {
        updateIfNotNull(dto.phoneNumber(), phoneNumber -> this.phoneNumber = phoneNumber);
        updateIfNotNull(dto.email(), email -> this.email = email);
        updateIfNotNull(dto.university(),university -> this.university = university);
        updateIfNotNull(dto.graduateSchool(), graduateSchool -> this.graduateSchool = graduateSchool);
        updateIfNotNull(dto.phoneNumberPublic(), phoneNumberPublic -> this.phoneNumberPublic = phoneNumberPublic);
        updateIfNotNull(dto.selfIntroduction(), selfIntroduction -> this.selfIntroduction = selfIntroduction);
        updateIfNotNull(dto.link(), link -> this.link = link);
        if(dto.isProfileImageChanged() != null && dto.isProfileImageChanged()) {
            this.profileImageUrl = dto.profileImageUrl();
        }
    }

    //유저 권한 변경
    public void exchangeRole(MemberRole newRole) {
        if (newRole == null) {
            return;
        }
        this.role = newRole;
    }

    public boolean isNotOwner() {
        return !Objects.equals(this.id, SecurityUtils.getCurrentMemberId());
    }

    public boolean hasDeleteRole() {
        return isManager() || isPresident() || isAdmin();
    }

    public boolean isMember() {
        return this.role == MemberRole.MEMBER;
    }

    public boolean isManager() {
        return this.role == MemberRole.MANAGER;
    }

    public boolean isPresident() {
        return this.role == MemberRole.PRESIDENT;
    }

    public boolean isAdmin() {
        return this.role == MemberRole.ADMIN;
    }

    private <T> void updateIfNotNull(T value, Consumer<T> updater) {
        if (value != null) {
            updater.accept(value);
        }
    }


    // 회원 탈퇴 처리
    public void withdraw() {
        if (this.isDeleted || this.status == MemberStatus.WITHDRAWN) return;

        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.activityStatus = false;
        this.status = MemberStatus.WITHDRAWN;

        anonymizeOnWithdraw();
    }

    private void anonymizeOnWithdraw() {
        if (this.id == null) {
            throw new IllegalStateException("withdraw는 영속화된 회원만 가능합니다.");
        }

        this.name = "탈퇴한 회원";
        this.profileImageUrl = null;
        this.password = null;
        this.phoneNumber = null;
        this.phoneNumberPublic = false;
        this.selfIntroduction = null;
        this.link = null;
        this.university = null;
        this.graduateSchool = null;

        long ts = System.currentTimeMillis();
        this.email = "withdrawn_" + this.id + "_" + ts + "@deleted.local";
        this.providerId = "withdrawn_" + this.id + "_" + ts;
        this.kakaoId = null;
        this.appleRefreshToken = null;
    }

    /** Apple refresh_token 갱신 — Web 로그인 코드 교환 시 호출 */
    public void updateAppleRefreshToken(String refreshToken) {
        this.appleRefreshToken = refreshToken;
    }

    public void updatePassword(String password) {
        this.password = Password.from(password);
    }

    public void checkPassword(String password) {
        try {
            this.password.validateMatches(password);
        } catch (NullPointerException e) {
            throw new PasswordNotSettingException();
        } catch (MisMatchPasswordException e) {
            throw new MisMatchPasswordException();
        }
    }

}
