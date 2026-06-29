import { Component, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { KeyValuePipe } from '@angular/common';
import { CatalogService, StorePayload } from '../../services/catalog.service';

const DEFAULT_PORTS: Record<string, number> = {
  POSTGRES: 5432, MONGODB: 27017, NEO4J: 7687,
  MYSQL: 3306, MARIADB: 3306, SQLSERVER: 1433, ORACLE: 1521,
  SNOWFLAKE: 443, CLICKHOUSE: 8123,
  ELASTICSEARCH: 9200, REDIS: 6379, CASSANDRA: 9042, SOLR: 8983, KAFKA: 9092,
};

@Component({
  selector: 'app-sidebar',
  imports: [KeyValuePipe, FormsModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
})
export class Sidebar {
  readonly tableSelected = output<string>();
  protected readonly catalog = inject(CatalogService);

  protected showForm = signal(false);
  protected saving = signal(false);
  protected formError = signal<string | null>(null);

  protected form: StorePayload = this.emptyForm();

  protected toggleForm() {
    this.showForm.update(v => !v);
    if (!this.showForm()) this.resetForm();
  }

  protected onTypeChange() {
    this.form.port = DEFAULT_PORTS[this.form.type] ?? 5432;
  }

  protected submitStore() {
    if (!this.form.name || !this.form.host || !this.form.database) {
      this.formError.set('Preencha nome, host e database.');
      return;
    }
    this.saving.set(true);
    this.formError.set(null);
    this.catalog.registerStore(this.form).subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.resetForm();
      },
      error: err => {
        this.saving.set(false);
        const msg = err?.error?.message ?? err?.message ?? 'Erro ao registrar store.';
        this.formError.set(msg);
      },
    });
  }

  protected deleteStore(name: string) {
    if (!confirm(`Remover a store "${name}"?`)) return;
    this.catalog.removeStore(name).subscribe({
      error: err => {
        const msg = err?.error?.message ?? 'Erro ao remover store.';
        alert(msg);
      },
    });
  }

  protected selectTable(name: string) {
    this.tableSelected.emit(`SELECT * FROM ${name} LIMIT 10`);
  }

  protected storeColor(name: string): string {
    if (name.includes('pg') || name.includes('postgres')) return '#3fb950';
    if (name.includes('mg') || name.includes('mongo')) return '#bc8cff';
    if (name.includes('neo')) return '#d29922';
    return '#58a6ff';
  }

  protected storeBadgeClass(name: string): string {
    if (name.includes('pg') || name.includes('postgres')) return 'badge-pg';
    if (name.includes('mg') || name.includes('mongo')) return 'badge-mg';
    if (name.includes('neo')) return 'badge-neo';
    return 'badge-def';
  }

  protected storeBadgeLabel(name: string): string {
    if (name.includes('pg') || name.includes('postgres')) return 'PG';
    if (name.includes('mg') || name.includes('mongo')) return 'MG';
    if (name.includes('neo')) return 'NEO4J';
    return name.toUpperCase();
  }

  private emptyForm(): StorePayload {
    return { name: '', type: 'POSTGRES', host: 'localhost', port: 5432, database: '', username: '', password: '' };
  }

  private resetForm() {
    this.form = this.emptyForm();
    this.formError.set(null);
  }
}
