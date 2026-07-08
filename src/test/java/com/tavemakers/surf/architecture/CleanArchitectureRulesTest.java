package com.tavemakers.surf.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * 클린 아키텍처 의존성 규칙 R1~R6 (docs/refactoring-plan.md 참고).
 *
 * 모든 규칙은 FreezingArchRule로 감싸져 있다:
 * 기존 위반은 src/test/resources/archunit-store 베이스라인에 동결되어 통과하고,
 * "새로운" 위반만 테스트를 실패시킨다. 기존 위반은 도메인 전환(Phase 3) 때 청산한다.
 */
@AnalyzeClasses(packages = "com.tavemakers.surf", importOptions = ImportOption.DoNotIncludeTests.class)
public class CleanArchitectureRulesTest {

    private static final String DOMAIN_ROOT = "com.tavemakers.surf.domain.";

    /** 패키지명에서 도메인 세그먼트 추출 (domain 하위가 아니면 null) */
    private static String domainOf(String packageName) {
        if (!packageName.startsWith(DOMAIN_ROOT)) {
            return null;
        }
        String rest = packageName.substring(DOMAIN_ROOT.length());
        int dot = rest.indexOf('.');
        return dot == -1 ? rest : rest.substring(0, dot);
    }

    /**
     * 패키지가 특정 도메인의 특정 계층(presentation/application/domain/infrastructure)에 속하는지.
     * "domain"이 도메인 루트(com.tavemakers.surf.domain)와 계층 이름으로 중복되므로
     * 도메인 세그먼트 뒤의 계층 세그먼트를 정확히 대조한다.
     */
    private static boolean isLayer(String packageName, String domain, String layer) {
        String base = DOMAIN_ROOT + domain + "." + layer;
        return packageName.equals(base) || packageName.startsWith(base + ".");
    }

    // ── R1: Controller는 application 계층(usecase/query)만 호출 ──────────────

    @ArchTest
    static final ArchRule R1a_controller_는_repository_직접_의존_금지 = FreezingArchRule.freeze(
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat(resideInAPackage("..repository.."))
                    .as("R1a: Controller는 Repository에 직접 의존할 수 없다"));

    @ArchTest
    static final ArchRule R1b_controller_는_usecase_query_만_호출 = FreezingArchRule.freeze(
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat(
                            resideInAnyPackage("..service..", "..facade..")
                                    .and(not(simpleNameEndingWith("GetService")))
                                    .and(not(simpleNameEndingWith("Usecase"))))
                    .as("R1b: Controller는 Usecase 또는 query(GetService)만 호출한다"));

    @ArchTest
    static final ArchRule R1c_controller_는_infrastructure_직접_의존_금지 = FreezingArchRule.freeze(
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat(resideInAPackage("..infrastructure.."))
                    .as("R1c: Controller는 infrastructure 계층(외부 어댑터)에 직접 의존할 수 없다 "
                            + "(Wave 3 심사에서 발견된 규칙 갭 — FcmService infra 이동으로 R1b 범위 이탈)"));

    // ── R2: 타 도메인 접근은 query(GetService) 또는 이벤트만 ────────────────

    @ArchTest
    static final ArchRule R2_타_도메인_비Get_service_호출_금지 = FreezingArchRule.freeze(
            noClasses().should(new ArchCondition<>("타 도메인의 비-Get Service/Usecase/Facade에 의존한다") {
                @Override
                public void check(JavaClass clazz, ConditionEvents events) {
                    String origin = domainOf(clazz.getPackageName());
                    if (origin == null) {
                        return;
                    }
                    for (Dependency dep : clazz.getDirectDependenciesFromSelf()) {
                        JavaClass target = dep.getTargetClass();
                        String targetDomain = domainOf(target.getPackageName());
                        if (targetDomain == null || targetDomain.equals(origin)) {
                            continue;
                        }
                        String pkg = target.getPackageName();
                        boolean isBehavior = pkg.contains(".service") || pkg.contains(".usecase")
                                || pkg.contains(".facade") || pkg.contains(".application");
                        boolean isAllowedQuery = target.getSimpleName().endsWith("GetService")
                                || pkg.contains(".query");
                        if (isBehavior && !isAllowedQuery) {
                            events.add(SimpleConditionEvent.satisfied(clazz, dep.getDescription()));
                        }
                    }
                }
            }).as("R2: 타 도메인 접근은 query(GetService) 조회 또는 도메인 이벤트만 허용된다"));

    // ── R3: 타 도메인 Repository 직접 참조 금지 ─────────────────────────────

    @ArchTest
    static final ArchRule R3_타_도메인_repository_직접_참조_금지 = FreezingArchRule.freeze(
            noClasses().should(new ArchCondition<>("타 도메인의 Repository에 의존한다") {
                @Override
                public void check(JavaClass clazz, ConditionEvents events) {
                    String origin = domainOf(clazz.getPackageName());
                    if (origin == null) {
                        return;
                    }
                    for (Dependency dep : clazz.getDirectDependenciesFromSelf()) {
                        JavaClass target = dep.getTargetClass();
                        String targetDomain = domainOf(target.getPackageName());
                        if (targetDomain != null && !targetDomain.equals(origin)
                                && target.getPackageName().contains(".repository")) {
                            events.add(SimpleConditionEvent.satisfied(clazz, dep.getDescription()));
                        }
                    }
                }
            }).as("R3: 타 도메인의 Repository를 직접 참조할 수 없다"));

    // ── R4: @Transactional은 application 계층(usecase/query)에만 ────────────

    @ArchTest
    static final ArchRule R4a_transactional_클래스는_application_계층에만 = FreezingArchRule.freeze(
            noClasses().that().resideOutsideOfPackages("..usecase..", "..application..", "..query..")
                    .should().beAnnotatedWith(Transactional.class)
                    .as("R4a: @Transactional 클래스는 application 계층(usecase/query)에만 둔다"));

    @ArchTest
    static final ArchRule R4b_transactional_메서드는_application_계층에만 = FreezingArchRule.freeze(
            noMethods().that().areDeclaredInClassesThat()
                    .resideOutsideOfPackages("..usecase..", "..application..", "..query..")
                    .should().beAnnotatedWith(Transactional.class)
                    .as("R4b: @Transactional 메서드는 application 계층(usecase/query)에만 둔다"));

    // ── R5: 트랜잭션 클래스는 외부 API 클라이언트에 의존 금지 ───────────────

    private static final Set<String> EXTERNAL_CLIENT_TYPES = Set.of(
            "org.springframework.web.client.RestTemplate",
            "org.springframework.web.reactive.function.client.WebClient",
            "org.springframework.mail.javamail.JavaMailSender",
            "com.google.firebase.messaging.FirebaseMessaging",
            "com.amazonaws.services.s3.AmazonS3",
            // 래퍼를 통한 외부 I/O도 감지 (Wave 3 letter R5 수정에서 드러난 사각지대)
            "com.tavemakers.surf.global.util.EmailSender");

    private static final DescribedPredicate<JavaClass> 트랜잭션_보유_클래스 =
            new DescribedPredicate<>("@Transactional을 클래스 또는 메서드에 선언한 클래스") {
                @Override
                public boolean test(JavaClass clazz) {
                    return clazz.isAnnotatedWith(Transactional.class)
                            || clazz.getMethods().stream().anyMatch(m -> m.isAnnotatedWith(Transactional.class));
                }
            };

    private static final DescribedPredicate<JavaClass> 외부_API_클라이언트 =
            new DescribedPredicate<>("외부 API 클라이언트 (RestTemplate/WebClient/Mail/FCM/S3/*ApiClient)") {
                @Override
                public boolean test(JavaClass clazz) {
                    return EXTERNAL_CLIENT_TYPES.contains(clazz.getFullName())
                            || clazz.getSimpleName().endsWith("ApiClient");
                }
            };

    @ArchTest
    static final ArchRule R5_트랜잭션_안_외부_API_호출_금지 = FreezingArchRule.freeze(
            noClasses().that(트랜잭션_보유_클래스)
                    .should().dependOnClassesThat(외부_API_클라이언트)
                    .as("R5: 트랜잭션을 여는 클래스는 외부 API 클라이언트에 의존할 수 없다 "
                            + "(커밋 후 @TransactionalEventListener(AFTER_COMMIT)로 분리)"));

    // ── R6: 비동기 부수효과 리스너는 @TransactionalEventListener로 통일 ──────
    // 동기 인-트랜잭션 정리 리스너(plain @EventListener, @Async 없음)는 발행자 트랜잭션에
    // 참여해 함께 롤백되므로 허용한다 (D1 결정 — docs/refactoring-plan.md).
    // 금지 대상은 커밋 전에 별도 스레드로 새는 부수효과(@Async + plain @EventListener)다.

    @ArchTest
    static final ArchRule R6_비동기_plain_EventListener_금지 = FreezingArchRule.freeze(
            noMethods().that().areDeclaredInClassesThat()
                    .resideInAPackage("com.tavemakers.surf.domain..")
                    .and().areAnnotatedWith(Async.class)
                    .should().beAnnotatedWith(EventListener.class)
                    .as("R6: 비동기(@Async) 부수효과 리스너는 @TransactionalEventListener(AFTER_COMMIT)를 "
                            + "사용한다 (@Async + plain @EventListener 금지)"));

    // ── R7: domain 계층은 같은 도메인의 application/presentation에 의존 금지 ──
    // 계층 방향성(presentation → application → domain)을 강제한다. domain 계층이
    // 같은 도메인의 application(usecase/query)이나 presentation(controller/dto)을
    // 참조하면 역방향 의존이다. 타 도메인 접근은 R2/R3가 담당한다.
    // 4계층 전환으로 드러난 기존 결합(예: PostDeleteService→PostGetService,
    // PostPatchService→presentation DTO)은 동결하고 신규 유입만 차단한다.

    @ArchTest
    static final ArchRule R7_domain_계층은_application_presentation_역의존_금지 = FreezingArchRule.freeze(
            noClasses().should(new ArchCondition<>("같은 도메인의 application/presentation 계층에 의존한다") {
                @Override
                public void check(JavaClass clazz, ConditionEvents events) {
                    String domain = domainOf(clazz.getPackageName());
                    if (domain == null || !isLayer(clazz.getPackageName(), domain, "domain")) {
                        return;
                    }
                    for (Dependency dep : clazz.getDirectDependenciesFromSelf()) {
                        String targetPkg = dep.getTargetClass().getPackageName();
                        if (!domain.equals(domainOf(targetPkg))) {
                            continue;
                        }
                        if (isLayer(targetPkg, domain, "application")
                                || isLayer(targetPkg, domain, "presentation")) {
                            events.add(SimpleConditionEvent.satisfied(clazz, dep.getDescription()));
                        }
                    }
                }
            }).as("R7: domain 계층은 같은 도메인의 application(usecase/query) 또는 "
                    + "presentation(controller/dto)에 의존할 수 없다"));
}
