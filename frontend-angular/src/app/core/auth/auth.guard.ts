import { inject } from "@angular/core";
import type { CanActivateFn } from "@angular/router";
import { startLogin } from "./auth.interceptor";
import { CurrentMemberStore } from "./current-member.store";

/** 주소로 직접 들어오면 회원 조회가 끝나기 전에 가드가 돈다. 기다리지 않으면 로그인 상태에서도 로그인으로 튕긴다. */
export const authGuard: CanActivateFn = async () => {
  const store = inject(CurrentMemberStore);
  if (await store.whenSettled()) {
    return true;
  }
  startLogin();
  return false;
};
