package com.tavemakers.surf.domain.member.entity;

import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.exception.MisMatchPasswordException;
import com.tavemakers.surf.domain.member.exception.PasswordNotSettingException;
import com.tavemakers.surf.domain.member.exception.TrackAlreadyExistsException;
import com.tavemakers.surf.global.common.entity.BaseEntity;
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
import java.util.Optional;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.BatchSize;


@Entity
@Getter
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자 protected 설정
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    /** Apple 첫 로그인 시 닉네임 미제공 (D5) → nullable 허용. 가입 폼(applySignup)에서 채워진다. */
    @Column
    private String name;

    private String profileImageUrl;

    private String university;

    private String graduateSchool;

    /** 온보딩에서 사용자가 입력한 통합 이메일. 소셜이 준 이메일(SocialAccount.providerEmail)과 구분된다. REGISTERING은 값 없음. */
    @Column(unique = true)
    private String email;

    @Embedded
    private Password password;

    /** 통합 판단 기준 전화번호. REGISTERING은 값 없음. */
    @Column(unique = true)
    private String phoneNumber;

    @Column(length = 256)
    private String selfIntroduction;

    @Column(length = 1024)
    private String link;

    private Boolean phoneNumberPublic=false;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<Track> tracks = new ArrayList<>();

    /** 회원에 연결된 소셜 로그인 수단 목록 (provider별 최대 1개). */
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<SocialAccount> socialAccounts = new ArrayList<>();

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
    public Member(String name,
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
     * OAuth provider 정보로 REGISTERING 상태의 회원을 생성한다 (D5).
     * Kakao 는 닉네임이 일반적으로 채워져 있고, Apple 은 첫 로그인 시 nickname/profileImageUrl 이 null 일 수 있다.
     * provider 식별 정보는 회원이 아니라 {@link SocialAccount}(정규 저장소)가 보유한다 —
     * 통합 이메일(Member.email)은 온보딩 입력값으로만 채우며 REGISTERING 단계에서는 값이 없다(null).
     */
    public static Member createRegisteringFromOAuth(OAuthUserInfoDTO info) {
        return Member.builder()
                .name(info.nickname())
                .phoneNumberPublic(false)
                .profileImageUrl(info.profileImageUrl())
                .status(MemberStatus.REGISTERING)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
    }

    /**
     * 회원가입 폼 값을 반영한다. 트랙 추가는 호출자(usecase)가 {@link #addTrack(Integer, Part)}로 해체해 수행한다.
     */
    public void applySignup(String name, String university, String graduateSchool,
                            String normalizedEmail, String normalizedPhone) {
        this.name = name;
        this.university = university;
        this.graduateSchool = graduateSchool;
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

        if (exists) {
            throw new TrackAlreadyExistsException();
        }

        Track track = new Track(generation, part);
        track.setMember(this); // 여기서만 add 수행
    }

    /** 소셜 계정을 연결한다. 동일 provider 계정이 이미 있으면 거부한다 (1 provider = 1 account). */
    public void addSocialAccount(SocialAccount socialAccount) {
        if (hasProvider(socialAccount.getProvider())) {
            throw new IllegalStateException(
                    "이미 연결된 provider 입니다: " + socialAccount.getProvider());
        }
        socialAccount.setMember(this); // setMember 에서만 양방향 add 수행
    }

    /**
     * 소셜 계정 연결 해제 — orphanRemoval 로 DB에서도 삭제된다.
     * cascade=ALL 관계에서는 리포지토리 직접 delete 시 부모 컬렉션의 잔존 참조가
     * 재영속화를 일으킬 수 있으므로 반드시 이 메서드로 컬렉션에서 제거해야 한다.
     */
    public void removeSocialAccount(SocialAccount socialAccount) {
        this.socialAccounts.remove(socialAccount);
    }

    /** 해당 provider 로그인 수단을 이미 보유하고 있는지 여부. */
    public boolean hasProvider(Provider provider) {
        return socialAccounts.stream()
                .anyMatch(sa -> sa.getProvider() == provider);
    }

    /** 해당 provider 의 소셜 계정을 조회한다. */
    public Optional<SocialAccount> findSocialAccount(Provider provider) {
        return socialAccounts.stream()
                .filter(sa -> sa.getProvider() == provider)
                .findFirst();
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
    public void updateProfile(String email,
                              String university,
                              String graduateSchool,
                              String selfIntroduction,
                              String link,
                              String phoneNumber,
                              Boolean phoneNumberPublic,
                              String profileImageUrl,
                              Boolean isProfileImageChanged) {
        updateIfNotNull(phoneNumber, value -> this.phoneNumber = value);
        updateIfNotNull(email, value -> this.email = value);
        updateIfNotNull(university, value -> this.university = value);
        updateIfNotNull(graduateSchool, value -> this.graduateSchool = value);
        updateIfNotNull(phoneNumberPublic, value -> this.phoneNumberPublic = value);
        updateIfNotNull(selfIntroduction, value -> this.selfIntroduction = value);
        updateIfNotNull(link, value -> this.link = value);
        if (isProfileImageChanged != null && isProfileImageChanged) {
            this.profileImageUrl = profileImageUrl;
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

        // 소셜 계정 연결 제거(orphanRemoval로 DB에서도 삭제) — 남겨두면 재로그인 시
        // 탈퇴 회원에 매칭되어 신규 가입이 영구히 막힌다 (재가입은 새 계정으로 시작)
        this.socialAccounts.clear();
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
        // provider 식별자/unlink·revoke 데이터 정리는 SocialAccount 삭제(withdraw의 clear())가 담당한다.
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
