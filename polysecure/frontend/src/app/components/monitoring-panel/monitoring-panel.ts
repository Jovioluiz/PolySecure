/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { AdminService } from '../../services/admin.service';

const REFRESH_INTERVAL_MS = 10_000;

@Component({
  selector: 'app-monitoring-panel',
  templateUrl: './monitoring-panel.html',
  styleUrl: './monitoring-panel.scss',
})
export class MonitoringPanel implements OnInit, OnDestroy {
  protected readonly admin = inject(AdminService);
  private timer?: ReturnType<typeof setInterval>;

  ngOnInit() {
    this.admin.refresh();
    this.timer = setInterval(() => this.admin.refresh(), REFRESH_INTERVAL_MS);
  }

  ngOnDestroy() {
    if (this.timer) clearInterval(this.timer);
  }

  protected truncate(s: string, n = 70): string {
    return s.length > n ? s.slice(0, n) + '…' : s;
  }

  protected formatTime(iso: string): string {
    try { return new Date(iso).toLocaleTimeString('pt-BR'); } catch { return iso; }
  }

  protected weightRows(): { name: string; value: number }[] {
    const model = this.admin.stats()?.costModel;
    if (!model) return [];
    return model.featureNames.map((name, i) => ({ name, value: model.weights[i] ?? 0 }));
  }
}
