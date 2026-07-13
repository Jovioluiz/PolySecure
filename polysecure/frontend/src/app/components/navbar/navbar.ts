/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

import { Component, inject, input, output } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar {
  readonly loggedOut = output<void>();
  readonly helpRequested = output<void>();
  readonly monitoringToggled = output<void>();
  readonly monitoringActive = input(false);
  protected readonly auth = inject(AuthService);

  protected logout() {
    this.auth.logout();
    this.loggedOut.emit();
  }
}
