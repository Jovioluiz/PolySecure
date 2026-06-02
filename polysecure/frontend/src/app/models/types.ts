export interface ColumnDef {
  name: string;
  type: string;
  store: string;
  primaryKey: boolean;
}

export interface SelectResult {
  rows: Record<string, unknown>[];
  rowCount: number;
}

export interface DmlResult {
  operation: string;
  tableName: string;
  affectedByStore: Record<string, number>;
  message: string;
}

export type QueryMode = 'standard' | 'poly';

export type QueryResult = SelectResult | DmlResult | null;
