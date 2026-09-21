import createClient, { type Middleware } from "openapi-fetch";
import type { paths } from "./schema";

export const LOGIN_URL = "/oauth2/authorization/keycloak";
export const REDIRECT_KEY = "bbs:redirectAfterLogin";

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

const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";
const 안전한_메서드 = new Set(["GET", "HEAD", "OPTIONS", "TRACE"]);

function csrfToken(): string | null {
  const found = document.cookie.split("; ").find((entry) => entry.startsWith(`${CSRF_COOKIE}=`));
  return found ? decodeURIComponent(found.slice(CSRF_COOKIE.length + 1)) : null;
}

/** 세션 쿠키로 인증하므로 변경 요청에는 CSRF 토큰을 함께 보낸다. */
const csrfMiddleware: Middleware = {
  onRequest({ request }) {
    if (안전한_메서드.has(request.method)) {
      return request;
    }
    const token = csrfToken();
    if (token) {
      request.headers.set(CSRF_HEADER, token);
    }
    return request;
  },
};

/** 로그아웃은 변경 요청이므로 POST로 보내고 토큰을 싣는다. */
export async function logout(): Promise<void> {
  const token = csrfToken();
  await fetch("/logout", {
    method: "POST",
    headers: token ? { [CSRF_HEADER]: token } : {},
  });
  window.location.href = "/";
}

/** 401은 개별 호출부가 아니라 여기서만 다룬다. */
const authMiddleware: Middleware = {
  onResponse({ response }) {
    if (response.status === 401) {
      startLogin();
    }
    return response;
  },
};

/** 프론트엔드와 API는 같은 오리진에서 제공된다. 개발 중에는 Vite 프록시가 그 상태를 만든다. */
export const client = createClient<paths>({
  baseUrl: window.location.origin,
  // 전역 fetch를 호출 시점에 찾는다. 참조를 붙들면 테스트에서 가로채기가 걸리지 않는다.
  fetch: (request) => globalThis.fetch(request),
});
client.use(csrfMiddleware);
client.use(authMiddleware);
