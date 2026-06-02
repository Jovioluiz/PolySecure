import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { ColumnDef } from '../models/types';

export interface StorePayload {
  name: string;
  type: 'POSTGRES' | 'MONGODB' | 'NEO4J';
  host: string;
  port: number;
  database: string;
  username: string | null;
  password: string | null;
}

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  readonly stores = signal<string[]>([]);
  readonly tables = signal<Record<string, ColumnDef[]>>({});
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  load() {
    this.loading.set(true);
    this.error.set(null);
    forkJoin([
      this.http.get<string[]>(`${environment.apiBase}/catalog/stores`),
      this.http.get<Record<string, ColumnDef[]>>(`${environment.apiBase}/catalog/tables`),
    ]).subscribe({
      next: ([stores, tables]) => {
        this.stores.set(stores);
        this.tables.set(tables);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erro ao carregar catálogo');
        this.loading.set(false);
      },
    });
  }

  registerStore(payload: StorePayload) {
    return this.http
      .post(`${environment.apiBase}/stores`, payload, { responseType: 'text' })
      .pipe(tap(() => this.load()));
  }

  removeStore(name: string) {
    return this.http
      .delete(`${environment.apiBase}/stores/${name}`, { responseType: 'text' })
      .pipe(tap(() => this.load()));
  }
}
