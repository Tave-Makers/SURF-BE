package com.tavemakers.surf.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.SocialAccount;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import com.tavemakers.surf.global.jwt.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicLong;

/**
 * E2E 공통 지원 클래스.
 *
 * <p>전체 Spring 컨텍스트를 실제로 부팅한다(RANDOM_PORT). layer-first 재편 + 전 도메인 B안 전환이
 * 빈 배선/트랜잭션/직렬화를 깨지 않았는지 검증하는 것이 목적이므로, 프로덕션 빈은 최대한 그대로 두고
 * 부팅에 필요한 최소 설정만 주입한다.
 *
 * <p>부팅 게이트 분석 결과 주입한 설정(모두 test 전용):
 * <ul>
 *   <li>{@code jwt.access.expiration / jwt.refresh.expiration} — 프로덕션(JwtService)은 이 키를 쓰는데
 *       test application.yml 은 {@code jwt.access-token-expiration} 형태의 <b>낡은 키</b>를 갖고 있어
 *       전체 컨텍스트 로딩 시 placeholder 미해결로 부팅 실패한다(단위·슬라이스 테스트에서는 드러나지 않던 결함).</li>
 *   <li>{@code feedback.hash.secret}, {@code kakao.*}, {@code apple.*} — @Value / @Validated @ConfigurationProperties
 *       가 요구하는 값. test yml 에 없어 미주입 시 부팅 실패.</li>
 *   <li>{@code cloud.aws.bucket-name} — 프로덕션 @Value 키. test yml 은 {@code cloud.aws.s3.bucket} 로 다르게 정의됨.</li>
 *   <li>{@code spring.mail.username} — EmailSender @Value.</li>
 * </ul>
 *
 * <p>{@link JavaMailSender} 는 test 프로파일에 {@code spring.mail.host} 가 없어 자동 구성되지 않으므로
 * @MockBean 으로 대체한다(EmailSender 의존성 충족). Firebase/S3/Redis 는 지연 초기화라 부팅을 막지 않으며,
 * {@code @Transactional} 롤백으로 AFTER_COMMIT 비동기 리스너(FCM/외부 unlink)가 발화하지 않아 외부 호출도 없다.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jwt.access.expiration=3600000",
                "jwt.refresh.expiration=604800000",
                "feedback.hash.secret=e2e-feedback-hash-secret",
                "kakao.admin-key=e2e-kakao-admin-key",
                "kakao.unlink-uri=https://kapi.kakao.com/v1/user/unlink",
                "kakao.client-id=e2e-kakao-client-id",
                "kakao.client-secret=e2e-kakao-client-secret",
                "kakao.redirect-uri=https://e2e.local/login/oauth2/code/kakao",
                "kakao.token-uri=https://kauth.kakao.com/oauth/token",
                "kakao.userinfo-uri=https://kapi.kakao.com/v2/user/me",
                "apple.service-client-id=e2e.service.client",
                "apple.app-bundle-id=e2e.app.bundle",
                "apple.team-id=E2ETEAMID",
                "apple.key-id=E2EKEYID",
                "apple.private-key=e2e-dummy-private-key",
                "apple.redirect-uri=https://e2e.local/login/oauth2/code/apple",
                "cloud.aws.bucket-name=e2e-test-bucket",
                "spring.mail.username=e2e@surf.local"
        }
)
@AutoConfigureMockMvc
@Transactional
public abstract class E2ESupport {

    /** 같은 providerId 중복으로 unique 제약을 건드리지 않도록 테스트 실행 전역에서 유일값을 부여한다. */
    private static final AtomicLong PROVIDER_SEQ = new AtomicLong(1);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected MemberRepository memberRepository;

    @MockBean
    protected JavaMailSender javaMailSender;

    /** 승인 상태의 회원을 카카오 SocialAccount 연결과 함께 실제 DB에 저장한다 (실 데이터와 동일한 형태). */
    protected Member persistMember(MemberRole role) {
        long seq = PROVIDER_SEQ.getAndIncrement();
        Member member = Member.builder()
                .name("E2E회원" + seq)
                .email("e2e" + seq + "@surf.local")
                .phoneNumber("0101000" + String.format("%04d", seq))
                .phoneNumberPublic(false)
                .status(MemberStatus.APPROVED)
                .role(role)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        member.addSocialAccount(SocialAccount.builder()
                .provider(Provider.KAKAO)
                .providerId("e2e-provider-" + seq)
                .kakaoId(900000L + seq)
                .build());
        Member saved = memberRepository.save(member);
        memberRepository.flush();
        return saved;
    }

    /** 회원 기준 Authorization 헤더 값(Bearer AccessToken)을 만든다. */
    protected String bearer(Member member) {
        return "Bearer " + jwtService.createAccessToken(member.getId(), member.getRole().name());
    }

    /** MockMvc 요청이 별도 트랜잭션을 열 때 이전 쓰기가 보이도록 커밋 후 새 트랜잭션을 연다(선택적 사용). */
    protected void flushAndRestartTx() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
    }
}
