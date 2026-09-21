import { inject } from "@angular/core";
import type { CanActivateFn } from "@angular/router";
import { startLogin } from "./auth.interceptor";
import { CurrentMemberStore } from "./current-member.store";

export const authGuard: CanActivateFn = () => {
  const store = inject(CurrentMemberStore);
  if (store.member()) {
    return true;
  }
  startLogin();
  return false;
};
