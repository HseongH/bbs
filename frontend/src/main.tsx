import { createRouter, RouterProvider } from "@tanstack/react-router";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { takeRedirectPath } from "./api/client";
import { routeTree } from "./routeTree.gen";
import "./styles.css";

const router = createRouter({ routeTree });

declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}

const container = document.getElementById("root");
if (!container) {
  throw new Error("root 엘리먼트를 찾을 수 없습니다.");
}

const redirectPath = takeRedirectPath();
if (redirectPath && redirectPath !== window.location.pathname + window.location.search) {
  window.history.replaceState(null, "", redirectPath);
}

createRoot(container).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
);
