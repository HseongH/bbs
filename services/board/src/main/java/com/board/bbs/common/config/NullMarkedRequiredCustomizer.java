package com.board.bbs.common.config;

import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * JSpecify의 nullability를 OpenAPI의 required로 옮긴다.
 *
 * <p>springdoc은 {@code @NullMarked}를 모르므로 모든 응답 필드를 선택 값으로 문서화한다. 그러면 스키마에서 생성한 클라이언트 타입이 실제보다 약해져
 * 계약을 강제하지 못한다. 레코드 컴포넌트에 {@code @Nullable}이 없으면 필수로 표시한다.
 */
@Configuration
public class NullMarkedRequiredCustomizer {

  private static final String DTO_BASE_PACKAGE = "com.board.bbs";

  /**
   * 응답·요청 레코드의 필수 필드를 채우는 커스터마이저를 만든다.
   *
   * @return OpenAPI 커스터마이저
   */
  @Bean
  OpenApiCustomizer requiredFromNullability() {
    Map<String, Class<?>> recordsBySimpleName = scanRecords();

    return openApi -> {
      Map<String, Schema> schemas = openApi.getComponents().getSchemas();
      if (schemas == null) {
        return;
      }
      schemas.forEach(
          (schemaName, schema) -> {
            Class<?> type = findRecord(recordsBySimpleName, schemaName);
            if (type == null || schema.getProperties() == null) {
              return;
            }
            applyRequired(schema, nullableComponentNames(type));
          });
    };
  }

  private static void applyRequired(Schema<?> schema, Set<String> nullableNames) {
    List<String> required = new ArrayList<>();
    for (Object propertyName : schema.getProperties().keySet()) {
      String name = String.valueOf(propertyName);
      if (!nullableNames.contains(name)) {
        required.add(name);
      }
    }
    if (!required.isEmpty()) {
      schema.setRequired(required);
    }
  }

  /**
   * 제네릭 응답은 스키마 이름이 타입 인자까지 이어붙은 형태가 된다. PageResponse&lt;PostSummaryResponse&gt;는
   * PageResponsePostSummaryResponse가 되므로 가장 긴 접두사로 원본 레코드를 찾는다.
   */
  private static @Nullable Class<?> findRecord(Map<String, Class<?>> records, String schemaName) {
    if (records.containsKey(schemaName)) {
      return records.get(schemaName);
    }
    return records.entrySet().stream()
        .filter(entry -> schemaName.startsWith(entry.getKey()))
        .max(Comparator.comparingInt(entry -> entry.getKey().length()))
        .map(Map.Entry::getValue)
        .orElse(null);
  }

  /** JSpecify의 @Nullable은 타입 애너테이션이므로 컴포넌트의 애너테이션과 타입 애너테이션을 모두 본다. */
  private static Set<String> nullableComponentNames(Class<?> type) {
    Set<String> names = new HashSet<>();
    for (RecordComponent component : type.getRecordComponents()) {
      boolean nullable =
          component.isAnnotationPresent(Nullable.class)
              || component.getAnnotatedType().isAnnotationPresent(Nullable.class);
      if (nullable) {
        names.add(component.getName());
      }
    }
    return names;
  }

  private static Map<String, Class<?>> scanRecords() {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AssignableTypeFilter(Record.class));

    Map<String, Class<?>> records = new HashMap<>();
    for (BeanDefinition definition : scanner.findCandidateComponents(DTO_BASE_PACKAGE)) {
      String className = definition.getBeanClassName();
      if (className == null) {
        continue;
      }
      try {
        Class<?> type = Class.forName(className);
        if (type.isRecord()) {
          records.put(type.getSimpleName(), type);
        }
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException("스캔한 클래스를 불러오지 못했습니다: " + className, e);
      }
    }
    return records;
  }
}
