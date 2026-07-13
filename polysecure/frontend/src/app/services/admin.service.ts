/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { environment } from '../../environments/environment';

export interface StoreHealth {
  name: string;
  type: string;
  status: 'UP' | 'DOWN';
  latencyMs: number;
}

export interface ViewStats {
  query: string;
  stores: string[];
  rowCount: number;
  hits: number;
}

export interface AuditRecord {
  timestamp: string;
  username: string;
  roleName: string;
  query: string;
  storesAccessed: string[];
  rowsReturned: number;
  anonymizationApplied: string[];
  durationMs: number;
}

export interface CostModel {
  featureNames: string[];
  weights: number[];
  sampleCount: number;
  description: string;
}

export interface StatsResponse {
  cardinalityCache: Record<string, number>;
  costModel: CostModel;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  readonly health = signal<StoreHealth[]>([]);
  readonly views = signal<ViewStats[]>([]);
  readonly audit = signal<AuditRecord[]>([]);
  readonly stats = signal<StatsResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  refresh() {
    this.loading.set(true);
    forkJoin({
      health: this.http.get<StoreHealth[]>(`${environment.apiBase}/admin/health`),
      views: this.http.get<ViewStats[]>(`${environment.apiBase}/admin/views`),
      audit: this.http.get<AuditRecord[]>(`${environment.apiBase}/admin/audit?limit=20`),
      stats: this.http.get<StatsResponse>(`${environment.apiBase}/admin/stats`),
    }).subscribe({
      next: res => {
        this.health.set(res.health);
        this.views.set(res.views);
        this.audit.set(res.audit.slice().reverse());
        this.stats.set(res.stats);
        this.loading.set(false);
        this.error.set(null);
      },
      error: () => {
        this.error.set('Erro ao carregar dados de monitoramento (requer papel DBA_ADMIN).');
        this.loading.set(false);
      },
    });
  }
}
