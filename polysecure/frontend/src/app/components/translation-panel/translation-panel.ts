/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-translation-panel',
  templateUrl: './translation-panel.html',
  styleUrl: './translation-panel.scss',
})
export class TranslationPanel {
  readonly sql = input<string | null>(null);
  readonly closed = output<void>();
}
