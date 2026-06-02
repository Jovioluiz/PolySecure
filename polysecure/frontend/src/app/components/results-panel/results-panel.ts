import { Component, input } from '@angular/core';
import { KeyValuePipe } from '@angular/common';
import { DmlResult, QueryResult, SelectResult } from '../../models/types';

function isSelect(r: QueryResult): r is SelectResult {
  return !!r && 'rows' in r;
}

function isDml(r: QueryResult): r is DmlResult {
  return !!r && 'affectedByStore' in r;
}

@Component({
  selector: 'app-results-panel',
  imports: [KeyValuePipe],
  templateUrl: './results-panel.html',
  styleUrl: './results-panel.scss',
})
export class ResultsPanel {
  readonly result = input<QueryResult | { error: string } | null>(null);

  get asSelect(): SelectResult | null {
    const r = this.result();
    return r && isSelect(r as QueryResult) ? (r as SelectResult) : null;
  }

  get asDml(): DmlResult | null {
    const r = this.result();
    return r && isDml(r as QueryResult) ? (r as DmlResult) : null;
  }

  get errorMsg(): string | null {
    const r = this.result();
    return r && 'error' in r ? (r as { error: string }).error : null;
  }

  get columns(): string[] {
    const s = this.asSelect;
    if (!s || s.rows.length === 0) return [];
    return Object.keys(s.rows[0]);
  }

  cellValue(row: Record<string, unknown>, col: string): string {
    const v = row[col];
    if (v === null || v === undefined) return '—';
    if (typeof v === 'object') return JSON.stringify(v);
    return String(v);
  }
}
