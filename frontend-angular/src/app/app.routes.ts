import type { Routes } from "@angular/router";

export const routes: Routes = [
  {
    path: "",
    pathMatch: "full",
    loadComponent: () => import("@/features/post/pages/post-list-page").then((m) => m.PostListPage),
  },
];
