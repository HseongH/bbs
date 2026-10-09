import { HttpClient, type HttpResponse } from "@angular/common/http";
import { inject, Service } from "@angular/core";
import type { Observable } from "rxjs";
import type { components } from "@/core/api/schema";

export type PostSummary = components["schemas"]["PostSummaryResponse"];
export type PostDetail = components["schemas"]["PostResponse"];
export type PostPage = components["schemas"]["PageResponsePostSummaryResponse"];

@Service()
export class PostApiService {
  private readonly http = inject(HttpClient);

  create(input: { title: string; content: string }): Observable<HttpResponse<void>> {
    return this.http.post<void>("/api/posts", input, { observe: "response" });
  }

  update(id: number, input: { title: string; content: string }): Observable<void> {
    return this.http.patch<void>(`/api/posts/${id}`, input);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`/api/posts/${id}`);
  }

  like(id: number): Observable<void> {
    return this.http.post<void>(`/api/posts/${id}/likes`, null);
  }

  unlike(id: number): Observable<void> {
    return this.http.delete<void>(`/api/posts/${id}/likes`);
  }
}
