import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import type { Observable } from "rxjs";
import type { components } from "@/core/api/schema";

export type Comment = components["schemas"]["CommentResponse"];
export type CommentPage = components["schemas"]["PageResponseCommentResponse"];

@Injectable({ providedIn: "root" })
export class CommentApiService {
  private readonly http = inject(HttpClient);

  write(postId: number, input: { body: string; parentCommentId?: number }): Observable<void> {
    return this.http.post<void>(`/api/posts/${postId}/comments`, input);
  }

  update(id: number, body: string): Observable<void> {
    return this.http.patch<void>(`/api/comments/${id}`, { body });
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`/api/comments/${id}`);
  }
}
