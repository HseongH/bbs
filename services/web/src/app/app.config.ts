import { provideHttpClient, withInterceptors, withXsrfConfiguration } from "@angular/common/http";
import { type ApplicationConfig, provideBrowserGlobalErrorListeners } from "@angular/core";
import { provideRouter, TitleStrategy, withComponentInputBinding } from "@angular/router";
import { authInterceptor } from "@/core/auth/auth.interceptor";
import { routes } from "./app.routes";
import { PageTitleStrategy } from "./page-title.strategy";

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    { provide: TitleStrategy, useExisting: PageTitleStrategy },
    provideHttpClient(
      withInterceptors([authInterceptor]),
      withXsrfConfiguration({ cookieName: "XSRF-TOKEN", headerName: "X-XSRF-TOKEN" }),
    ),
  ],
};
