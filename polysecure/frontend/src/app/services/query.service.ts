/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { throwError, timeout } from 'rxjs';
import { environment } from '../../environments/environment';
import { QueryMode, QueryResult } from '../models/types';

// Marks a query error as "the client gave up waiting", not a real failure reported by the
// backend — a query against a huge/unindexed table can legitimately take minutes, and the
// server keeps executing it after the client disconnects (there's no cancellation propagation
// to the DB driver), so this is purely "no answer yet", distinct from a 4xx/5xx the backend
// actually returned.
export interface ClientTimeoutError {
  readonly isClientTimeout: true;
  readonly timeoutMs: number;
}

export function isClientTimeoutError(err: unknown): err is ClientTimeoutError {
  return !!err && typeof err === 'object' && (err as { isClientTimeout?: boolean }).isClientTimeout === true;
}

@Injectable({ providedIn: 'root' })
export class QueryService {
  private readonly http = inject(HttpClient);

  // timeoutMs === null means "wait indefinitely" (the user explicitly opted out — some
  // analytical queries genuinely take several minutes on a large or unindexed table).
  execute(sql: string, mode: QueryMode, timeoutMs: number | null = 60_000) {
    const endpoint = mode === 'standard' ? '/query/standard' : '/query';
    const req$ = this.http.post<QueryResult>(`${environment.apiBase}${endpoint}`, { sql });
    if (timeoutMs == null) return req$;
    return req$.pipe(
      timeout({
        each: timeoutMs,
        with: () => throwError(() => ({ isClientTimeout: true, timeoutMs } satisfies ClientTimeoutError)),
      }),
    );
  }

  translate(sql: string) {
    return this.http.post<{ translated: string }>(
      `${environment.apiBase}/query/translate`,
      { sql },
    );
  }
}
