import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly token = signal<string | null>(localStorage.getItem('ps_token'));
  readonly username = signal<string | null>(localStorage.getItem('ps_user'));
  readonly isLoggedIn = computed(() => !!this.token());

  login(username: string, password: string) {
    return this.http
      .post<{ token: string }>(`${environment.apiBase}/auth/login`, { username, password })
      .pipe(
        tap(res => {
          this.token.set(res.token);
          this.username.set(username);
          localStorage.setItem('ps_token', res.token);
          localStorage.setItem('ps_user', username);
        }),
      );
  }

  logout() {
    this.token.set(null);
    this.username.set(null);
    localStorage.removeItem('ps_token');
    localStorage.removeItem('ps_user');
  }
}
