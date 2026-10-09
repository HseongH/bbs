// @ts-check
const eslint = require("@eslint/js");
const { defineConfig } = require("eslint/config");
const tseslint = require("typescript-eslint");
const angular = require("angular-eslint");

module.exports = defineConfig([
  { ignores: ["dist", "node_modules", "src/app/core/api/schema.d.ts"] },
  {
    files: ["**/*.ts"],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    // no-uncalled-signals 등 신호를 다루는 규칙은 타입 정보가 있어야 동작한다.
    languageOptions: {
      parserOptions: { projectService: true, tsconfigRootDir: __dirname },
    },
    rules: {
      // Angular 22의 현재 방식(CS-F03~F05). AI가 예전 방식으로 생성한 코드를 린트에서 막는다 (ADR-0015).
      "@angular-eslint/prefer-signals": "error",
      "@angular-eslint/prefer-output-emitter-ref": "error",
      "@angular-eslint/prefer-host-metadata-property": "error",
      "@angular-eslint/prefer-service-decorator": "error",
      "@angular-eslint/inject-at-top": "error",
      // 신호를 잘못 쓰는 실수
      "@angular-eslint/no-uncalled-signals": "error",
      "@angular-eslint/computed-must-return": "error",
      "@angular-eslint/reactive-context-must-read-signal": "error",
      // 안정 API만 쓴다. 업그레이드 때 깨질 수 있는 코드를 들이지 않는다.
      "@angular-eslint/no-developer-preview": "error",
      "@angular-eslint/no-experimental": "error",
      "@angular-eslint/directive-selector": [
        "error",
        {
          type: "attribute",
          prefix: "app",
          style: "camelCase",
        },
      ],
      "@angular-eslint/component-selector": [
        "error",
        {
          type: "element",
          prefix: "app",
          style: "kebab-case",
        },
      ],
    },
  },
  {
    files: ["**/*.html"],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
    rules: {
      "@angular-eslint/template/prefer-class-binding": "error",
      "@angular-eslint/template/prefer-style-binding": "error",
      "@angular-eslint/template/prefer-self-closing-tags": "error",
      "@angular-eslint/template/no-any": "error",
    },
  },
]);
