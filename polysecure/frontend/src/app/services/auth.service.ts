/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly token = signal<string | null>(this.loadValidToken());
  readonly username = signal<string | null>(localStorage.getItem('ps_user'));
  readonly isLoggedIn = computed(() => !!this.token());

  private loadValidToken(): string | null {
    const token = localStorage.getItem('ps_token');
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      if (payload.exp && payload.exp * 1000 < Date.now()) {
        localStorage.removeItem('ps_token');
        localStorage.removeItem('ps_user');
        return null;
      }
      return token;
    } catch {
      localStorage.removeItem('ps_token');
      localStorage.removeItem('ps_user');
      return null;
    }
  }

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
