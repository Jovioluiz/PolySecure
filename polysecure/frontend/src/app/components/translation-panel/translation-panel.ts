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
