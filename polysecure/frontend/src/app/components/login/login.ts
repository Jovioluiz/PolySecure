/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

import { Component, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  readonly loggedIn = output<void>();

  protected readonly auth = inject(AuthService);

  protected username = '';
  protected password = '';
  protected loading = signal(false);
  protected error = signal('');

  protected submit() {
    if (!this.username || !this.password) {
      this.error.set('Informe usuário e senha.');
      return;
    }
    this.loading.set(true);
    this.error.set('');

    this.auth.login(this.username, this.password).subscribe({
      next: () => {
        this.loading.set(false);
        this.loggedIn.emit();
      },
      error: () => {
        this.error.set('Credenciais inválidas.');
        this.loading.set(false);
      },
    });
  }
}
