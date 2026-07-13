/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

import { Component, input, output, signal } from '@angular/core';

type Tab = 'standard' | 'poly' | 'stores';

@Component({
  selector: 'app-help-modal',
  templateUrl: './help-modal.html',
  styleUrl: './help-modal.scss',
})
export class HelpModal {
  readonly showHelp = input(false);
  readonly closed = output<void>();

  protected activeTab = signal<Tab>('standard');

  protected setTab(t: Tab) {
    this.activeTab.set(t);
  }

  protected close() {
    this.closed.emit();
  }
}
