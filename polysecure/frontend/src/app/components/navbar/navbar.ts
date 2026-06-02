import { Component, inject, output } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar {
  readonly loggedOut = output<void>();
  readonly helpRequested = output<void>();
  protected readonly auth = inject(AuthService);

  protected logout() {
    this.auth.logout();
    this.loggedOut.emit();
  }
}
