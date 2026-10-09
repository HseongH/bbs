package com.board.bbs.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 기능(post, comment, member) 사이의 경계를 검증한다.
 *
 * <p>계층 규칙은 {@link HexagonalArchitectureTest}가 검증하지만, 계층 규칙만으로는 기능끼리 서로의 내부를 쓰거나 순환하는 것을 막지 못한다.
 */
@AnalyzeClasses(packages = "com.board.bbs", importOptions = ImportOption.DoNotIncludeTests.class)
class FeatureBoundaryTest {

  private static final Set<String> FEATURES = Set.of("post", "comment", "member");

  private static final Pattern FEATURE_PACKAGE =
      Pattern.compile("^com\\.board\\.bbs\\.([a-z]+)(\\..*)?$");

  @ArchTest
  static final ArchRule 기능_사이에_순환이_없다 =
      slices()
          .matching("com.board.bbs.(*)..")
          .should()
          .beFreeOfCycles()
          .because("서로를 아는 두 기능은 따로 떼어 내거나 따로 이해할 수 없다");

  @ArchTest
  static final ArchRule 다른_기능의_내부를_쓰지_않는다 =
      classes()
          .that()
          .resideInAnyPackage("..post..", "..comment..", "..member..")
          .should(onlyUsePublicApiOfOtherFeatures())
          .because("다른 기능에는 도메인과 애플리케이션 서비스로만 접근한다. 아웃바운드 포트와 어댑터는 그 기능의 구현이다");

  private static ArchCondition<JavaClass> onlyUsePublicApiOfOtherFeatures() {
    return new ArchCondition<>("다른 기능의 아웃바운드 포트와 어댑터에 의존하지 않아야 한다") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        Optional<String> origin = featureOf(item);
        if (origin.isEmpty()) {
          return;
        }
        for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
          JavaClass target = dependency.getTargetClass();
          Optional<String> targetFeature = featureOf(target);
          if (targetFeature.isEmpty()
              || targetFeature.equals(origin)
              || !isInternal(target)
              || isAllowedReadModelJoin(item, target)) {
            continue;
          }
          events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
        }
      }
    };
  }

  private static Optional<String> featureOf(JavaClass javaClass) {
    Matcher matcher = FEATURE_PACKAGE.matcher(javaClass.getPackageName());
    if (!matcher.matches() || !FEATURES.contains(matcher.group(1))) {
      return Optional.empty();
    }
    return Optional.of(matcher.group(1));
  }

  private static boolean isInternal(JavaClass javaClass) {
    String packageName = javaClass.getPackageName();
    return packageName.contains(".adapter") || packageName.contains(".application.port.out");
  }

  /**
   * 목록 조회처럼 여러 기능의 데이터를 한 번에 읽는 쿼리는 다른 기능의 테이블을 조인할 수 있다. 읽기 전용 조회 저장소가 Querydsl 메타모델을 참조하는 경우로만
   * 한정한다.
   */
  private static boolean isAllowedReadModelJoin(JavaClass origin, JavaClass target) {
    return origin.getSimpleName().endsWith("QueryRepository")
        && target.getSimpleName().startsWith("Q")
        && target.getSimpleName().endsWith("JpaEntity");
  }
}
