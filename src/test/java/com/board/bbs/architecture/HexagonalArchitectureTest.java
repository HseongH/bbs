package com.board.bbs.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.jspecify.annotations.NullMarked;

@AnalyzeClasses(packages = "com.board.bbs", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

  @ArchTest
  static final ArchRule 도메인은_프레임워크를_모른다 =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework..", "jakarta..", "com.querydsl..")
          .because("도메인은 프레임워크와 영속성 기술에 의존하지 않는다");

  @ArchTest
  static final ArchRule 도메인은_어댑터를_모른다 =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..adapter..")
          .because("의존 방향은 어댑터에서 도메인으로만 향한다");

  @ArchTest
  static final ArchRule 애플리케이션은_어댑터를_모른다 =
      noClasses()
          .that()
          .resideInAPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..adapter..")
          .because("애플리케이션은 포트 인터페이스로만 바깥과 통신한다");

  @ArchTest
  static final ArchRule 인바운드_어댑터는_아웃바운드_어댑터를_모른다 =
      noClasses()
          .that()
          .resideInAPackage("..adapter.in..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..adapter.out..")
          .because("웹 계층이 영속성 계층을 직접 호출하면 유스케이스를 우회하게 된다");

  @ArchTest
  static final ArchRule 트랜잭션은_애플리케이션_서비스에만_존재한다 =
      noMethods()
          .that()
          .areDeclaredInClassesThat()
          .resideOutsideOfPackage("..application.service..")
          .should()
          .beAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
          .because("트랜잭션 경계는 유스케이스 단위로만 정의한다");

  @ArchTest
  static final ArchRule JPA_엔티티는_영속성_어댑터에만_존재한다 =
      classes()
          .that()
          .areAnnotatedWith(jakarta.persistence.Entity.class)
          .should()
          .resideInAPackage("..adapter.out.persistence..")
          .because("엔티티는 영속성 어댑터의 내부 구현이다");

  @ArchTest
  static final ArchRule 컨트롤러는_웹_어댑터에만_존재한다 =
      classes()
          .that()
          .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
          .should()
          .resideInAPackage("..adapter.in.web..")
          .because("HTTP 진입점은 인바운드 웹 어댑터에 모은다");

  @ArchTest
  static final ArchRule 아웃바운드_포트는_인터페이스다 =
      classes()
          .that()
          .resideInAPackage("..application.port..")
          .should()
          .beInterfaces()
          .because("포트는 구현이 아니라 계약이다");

  @ArchTest
  static final ArchRule 모든_패키지는_NullMarked다 =
      classes()
          .that()
          .resideInAPackage("com.board.bbs..")
          .should(declaredInNullMarkedPackage())
          .because("null 계약이 선언되지 않은 패키지는 NullAway 검사 대상에서 조용히 빠진다");

  private static ArchCondition<JavaClass> declaredInNullMarkedPackage() {
    return new ArchCondition<>("선언된 패키지가 @NullMarked여야 한다") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        if (!item.getPackage().isAnnotatedWith(NullMarked.class)) {
          events.add(
              SimpleConditionEvent.violated(
                  item, item.getPackage().getName() + " 패키지에 @NullMarked 선언이 없습니다."));
        }
      }
    };
  }
}
