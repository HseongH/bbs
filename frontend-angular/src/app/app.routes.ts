import type { Routes } from "@angular/router";

export const routes: Routes = [
  {
    path: "",
    pathMatch: "full",
    loadComponent: () => import("@/features/post/pages/post-list-page").then((m) => m.PostListPage),
  },
  {
    path: "posts/:postId",
    loadComponent: () =>
      import("@/features/post/pages/post-detail-page").then((m) => m.PostDetailPage),
  },
];
