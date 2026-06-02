import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { QueryMode, QueryResult } from '../models/types';

@Injectable({ providedIn: 'root' })
export class QueryService {
  private readonly http = inject(HttpClient);

  execute(sql: string, mode: QueryMode) {
    const endpoint = mode === 'standard' ? '/query/standard' : '/query';
    return this.http.post<QueryResult>(`${environment.apiBase}${endpoint}`, { sql });
  }

  translate(sql: string) {
    return this.http.post<{ translated: string }>(
      `${environment.apiBase}/query/translate`,
      { sql },
    );
  }
}
