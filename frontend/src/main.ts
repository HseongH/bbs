import { bootstrapApplication } from "@angular/platform-browser";
import { takeRedirectPath } from "@/core/auth/auth.interceptor";
import { appConfig } from "./app/app.config";
import { App } from "./app/app";

const redirectPath = takeRedirectPath();
if (redirectPath && redirectPath !== window.location.pathname + window.location.search) {
  window.history.replaceState(null, "", redirectPath);
}

bootstrapApplication(App, appConfig).catch((err: unknown) => console.error(err));
