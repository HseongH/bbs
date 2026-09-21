import type { Routes } from "@angular/router";
import { authGuard } from "@/core/auth/auth.guard";

export const routes: Routes = [
  {
    path: "",
    pathMatch: "full",
    loadComponent: () => import("@/features/post/pages/post-list-page").then((m) => m.PostListPage),
  },
  {
    path: "posts/new",
    canActivate: [authGuard],
    loadComponent: () => import("@/features/post/pages/post-new-page").then((m) => m.PostNewPage),
  },
  {
    path: "posts/:postId/edit",
    canActivate: [authGuard],
    loadComponent: () => import("@/features/post/pages/post-edit-page").then((m) => m.PostEditPage),
  },
  {
    path: "posts/:postId",
    loadComponent: () =>
      import("@/features/post/pages/post-detail-page").then((m) => m.PostDetailPage),
  },
];
