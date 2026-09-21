import { HttpContextToken, type HttpInterceptorFn } from "@angular/common/http";
import { catchError, throwError } from "rxjs";

export const LOGIN_URL = "/oauth2/authorization/keycloak";
export const REDIRECT_KEY = "bbs:redirectAfterLogin";

/** 로그인 여부를 확인하는 요청처럼 401이 정상 응답인 경우에 단다. */
export const SKIP_LOGIN_REDIRECT = new HttpContextToken(() => false);

export function startLogin(): void {
  try {
    sessionStorage.setItem(REDIRECT_KEY, window.location.pathname + window.location.search);
  } catch {
    // 시크릿 모드 등 저장이 막힌 환경에서는 복귀 없이 로그인만 진행한다.
  }
  window.location.href = LOGIN_URL;
}

/** 로그인 후 돌아갈 경로를 한 번만 꺼낸다. */
export function takeRedirectPath(): string | null {
  try {
    const path = sessionStorage.getItem(REDIRECT_KEY);
    sessionStorage.removeItem(REDIRECT_KEY);
    return path;
  } catch {
    return null;
  }
}

function is401(error: unknown): boolean {
  return typeof error === "object" && error !== null && "status" in error && error.status === 401;
}

export const authInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(
    catchError((error: unknown) => {
      if (!request.context.get(SKIP_LOGIN_REDIRECT) && is401(error)) {
        startLogin();
      }
      return throwError(() => error);
    }),
  );
