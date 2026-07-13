/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

import { Component, ViewChild, inject, signal } from '@angular/core';
import { AuthService } from './services/auth.service';
import { CatalogService } from './services/catalog.service';
import { QueryResult } from './models/types';
import { Login } from './components/login/login';
import { Navbar } from './components/navbar/navbar';
import { Sidebar } from './components/sidebar/sidebar';
import { EditorPanel } from './components/editor-panel/editor-panel';
import { TranslationPanel } from './components/translation-panel/translation-panel';
import { ResultsPanel } from './components/results-panel/results-panel';
import { HelpModal } from './components/help-modal/help-modal';
import { MonitoringPanel } from './components/monitoring-panel/monitoring-panel';

interface QueryTab {
  id: number;
  label: string;
  sql: string;
  result: QueryResult | { error: string } | null;
  translationSql: string | null;
}

@Component({
  selector: 'app-root',
  imports: [Login, Navbar, Sidebar, EditorPanel, TranslationPanel, ResultsPanel, HelpModal, MonitoringPanel],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly catalog = inject(CatalogService);

  protected queryResult = signal<QueryResult | { error: string } | null>(null);
  protected translationSql = signal<string | null>(null);
  protected showHelp = signal(false);
  protected showMonitoring = signal(false);

  protected tabs = signal<QueryTab[]>([{ id: 1, label: 'Query 1', sql: '', result: null, translationSql: null }]);
  protected activeTabId = signal(1);
  private nextTabId = 1;

  @ViewChild(EditorPanel) private editorPanel?: EditorPanel;

  constructor() {
    if (this.auth.isLoggedIn()) {
      this.catalog.load();
    }
  }

  protected onLoggedIn() {
    this.catalog.load();
  }

  protected addTab() {
    this.saveCurrent();
    const id = ++this.nextTabId;
    this.tabs.update(ts => [...ts, { id, label: `Query ${id}`, sql: '', result: null, translationSql: null }]);
    this.activeTabId.set(id);
    this.queryResult.set(null);
    this.translationSql.set(null);
    this.editorPanel?.setQuery('');
  }

  protected switchTab(id: number) {
    if (id === this.activeTabId()) return;
    this.saveCurrent();
    this.activeTabId.set(id);
    const tab = this.tabs().find(t => t.id === id)!;
    this.queryResult.set(tab.result);
    this.translationSql.set(tab.translationSql);
    this.editorPanel?.setQuery(tab.sql);
  }

  protected closeTab(id: number, event: Event) {
    event.stopPropagation();
    const tabs = this.tabs();
    if (tabs.length === 1) return;
    const idx = tabs.findIndex(t => t.id === id);
    this.tabs.update(ts => ts.filter(t => t.id !== id));
    if (this.activeTabId() === id) {
      const next = this.tabs()[Math.max(0, idx - 1)];
      this.switchTab(next.id);
    }
  }

  private saveCurrent() {
    const id = this.activeTabId();
    const sql = this.editorPanel?.getQuery() ?? '';
    this.tabs.update(ts => ts.map(t =>
      t.id === id ? { ...t, sql, result: this.queryResult(), translationSql: this.translationSql() } : t
    ));
  }

  protected onTableSelected(sql: string) {
    this.editorPanel?.setQuery(sql);
  }

  protected onResult(r: QueryResult | { error: string }) {
    this.queryResult.set(r);
    const id = this.activeTabId();
    this.tabs.update(ts => ts.map(t => t.id === id ? { ...t, result: r } : t));
  }

  protected onTranslation(sql: string) {
    this.translationSql.set(sql);
    const id = this.activeTabId();
    this.tabs.update(ts => ts.map(t => t.id === id ? { ...t, translationSql: sql } : t));
  }

  protected toggleMonitoring() {
    this.showMonitoring.update(v => !v);
  }
}
