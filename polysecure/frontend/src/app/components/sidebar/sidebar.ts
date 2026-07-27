/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

import { Component, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { KeyValuePipe } from '@angular/common';
import { CatalogService, StorePayload, parseErrorMessage } from '../../services/catalog.service';

const DEFAULT_PORTS: Record<string, number> = {
  POSTGRES: 5432, MONGODB: 27017, NEO4J: 7687,
  MYSQL: 3306, MARIADB: 3306, SQLSERVER: 1433, ORACLE: 1521,
  SNOWFLAKE: 443, SQLITE: 0, FIREBIRD: 3050,
  DATABRICKS: 443, DOLPHINDB: 8848,
  ELASTICSEARCH: 9200, REDIS: 6379, CASSANDRA: 9042, SOLR: 8983, KAFKA: 9092,
};

const STORE_TYPE_LABELS: Record<string, string> = {
  POSTGRES: 'PostgreSQL', MYSQL: 'MySQL', MARIADB: 'MariaDB', SQLSERVER: 'SQL Server',
  ORACLE: 'Oracle', SNOWFLAKE: 'Snowflake', SQLITE: 'SQLite', FIREBIRD: 'Firebird',
  DATABRICKS: 'Databricks', DOLPHINDB: 'DolphinDB',
  MONGODB: 'MongoDB', NEO4J: 'Neo4j', REDIS: 'Redis', CASSANDRA: 'Cassandra',
  ELASTICSEARCH: 'Elasticsearch', SOLR: 'Apache Solr', KAFKA: 'Apache Kafka',
};

/** Grupos espelham os optgroups do formulário de cadastro. */
const STORE_TYPE_GROUP: Record<string, 'relational' | 'analytics' | 'nosql' | 'search'> = {
  POSTGRES: 'relational', MYSQL: 'relational', MARIADB: 'relational', SQLSERVER: 'relational',
  ORACLE: 'relational', SNOWFLAKE: 'relational', SQLITE: 'relational', FIREBIRD: 'relational',
  DATABRICKS: 'analytics', DOLPHINDB: 'analytics',
  MONGODB: 'nosql', NEO4J: 'nosql', REDIS: 'nosql', CASSANDRA: 'nosql',
  ELASTICSEARCH: 'search', SOLR: 'search', KAFKA: 'search',
};

const GROUP_COLOR: Record<string, string> = {
  relational: '#3fb950',
  analytics: '#d29922',
  nosql: '#bc8cff',
  search: '#58a6ff',
};

/** Stores cloud-only: "host" é um endpoint remoto, nunca localhost. */
const CLOUD_TYPES = new Set(['DATABRICKS', 'SNOWFLAKE']);

/** Stores embarcados/arquivo: host, porta e usuário não se aplicam. */
const FILE_BASED_TYPES = new Set(['SQLITE']);

/** Stores cuja autenticação ignora o campo usuário (fixo internamente). */
const NO_USERNAME_TYPES = new Set(['SQLITE', 'DATABRICKS']);

/** Stores em que a senha/token não é opcional (sem ela o driver falha com erro genérico). */
const PASSWORD_REQUIRED_TYPES = new Set(['DATABRICKS']);

const HOST_PLACEHOLDERS: Record<string, string> = {
  DATABRICKS: 'adb-xxxx.azuredatabricks.net',
  SNOWFLAKE: 'sua-conta.snowflakecomputing.com',
};

const DATABASE_PLACEHOLDERS: Record<string, string> = {
  DATABRICKS: 'ID do SQL warehouse',
  SQLITE: 'caminho do arquivo ou :memory:',
};

const PASSWORD_PLACEHOLDERS: Record<string, string> = {
  DATABRICKS: 'personal access token (PAT)',
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
    if (CLOUD_TYPES.has(this.form.type)) {
      if (this.form.host === 'localhost') this.form.host = '';
    } else if (!FILE_BASED_TYPES.has(this.form.type) && this.form.host === '') {
      this.form.host = 'localhost';
    }
    if (NO_USERNAME_TYPES.has(this.form.type)) this.form.username = '';
  }

  protected isFileBased(): boolean {
    return FILE_BASED_TYPES.has(this.form.type);
  }

  protected showUsername(): boolean {
    return !NO_USERNAME_TYPES.has(this.form.type);
  }

  protected hostPlaceholder(): string {
    return HOST_PLACEHOLDERS[this.form.type] ?? 'localhost';
  }

  protected databasePlaceholder(): string {
    return DATABASE_PLACEHOLDERS[this.form.type] ?? 'meu_banco';
  }

  protected passwordPlaceholder(): string {
    return PASSWORD_PLACEHOLDERS[this.form.type] ?? '(opcional)';
  }

  protected submitStore() {
    const hostRequired = !this.isFileBased();
    if (!this.form.name || (hostRequired && !this.form.host) || !this.form.database) {
      this.formError.set(hostRequired ? 'Preencha nome, host e database.' : 'Preencha nome e database.');
      return;
    }
    if (PASSWORD_REQUIRED_TYPES.has(this.form.type) && !this.form.password) {
      this.formError.set('Preencha o Personal Access Token (PAT) no campo Senha.');
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
        this.formError.set(parseErrorMessage(err, 'Erro ao registrar store.'));
      },
    });
  }

  protected deleteStore(name: string) {
    if (!confirm(`Remover a store "${name}"?`)) return;
    this.catalog.removeStore(name).subscribe({
      error: err => {
        alert(parseErrorMessage(err, 'Erro ao remover store.'));
      },
    });
  }

  protected selectTable(name: string) {
    this.tableSelected.emit(`SELECT * FROM ${name} LIMIT 10`);
  }

  protected storeColor(type: string): string {
    return GROUP_COLOR[STORE_TYPE_GROUP[type]] ?? GROUP_COLOR['search'];
  }

  /** col.store no catálogo de tabelas é o *nome* do store, não o tipo — resolve via catalog.stores(). */
  protected storeColorByName(name: string): string {
    const type = this.catalog.stores().find(s => s.name === name)?.type;
    return type ? this.storeColor(type) : GROUP_COLOR['search'];
  }

  protected storeBadgeClass(type: string): string {
    return `badge-${STORE_TYPE_GROUP[type] ?? 'search'}`;
  }

  protected storeTypeLabel(type: string): string {
    return STORE_TYPE_LABELS[type] ?? type;
  }

  private emptyForm(): StorePayload {
    return { name: '', type: 'POSTGRES', host: 'localhost', port: 5432, database: '', username: '', password: '' };
  }

  private resetForm() {
    this.form = this.emptyForm();
    this.formError.set(null);
  }
}
