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

const TABS_STORAGE_KEY = 'polysecure.editor.tabs';
const DEFAULT_QUERY = '-- Exemplo: buscar dados de tabela cross-store\nSELECT *\nFROM orders\nWHERE id = 1';

interface StoredTabs {
  tabs: { id: number; label: string; sql: string }[];
  activeTabId: number;
  nextTabId: number;
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
  protected initialSql = DEFAULT_QUERY;

  @ViewChild(EditorPanel) private editorPanel?: EditorPanel;

  private persistTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    if (this.auth.isLoggedIn()) {
      this.catalog.load();
    }
    this.restoreTabs();
  }

  private restoreTabs() {
    const raw = localStorage.getItem(TABS_STORAGE_KEY);
    if (!raw) return;
    try {
      const stored: StoredTabs = JSON.parse(raw);
      if (!stored.tabs?.length) return;
      this.tabs.set(stored.tabs.map(t => ({ ...t, result: null, translationSql: null })));
      this.activeTabId.set(stored.activeTabId);
      this.nextTabId = stored.nextTabId;
      const active = stored.tabs.find(t => t.id === stored.activeTabId) ?? stored.tabs[0];
      this.initialSql = active.sql;
    } catch {
      // stored value is corrupted/from an incompatible version — ignore and keep defaults
    }
  }

  private persistTabs() {
    const stored: StoredTabs = {
      tabs: this.tabs().map(t => ({ id: t.id, label: t.label, sql: t.sql })),
      activeTabId: this.activeTabId(),
      nextTabId: this.nextTabId,
    };
    localStorage.setItem(TABS_STORAGE_KEY, JSON.stringify(stored));
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
    this.persistTabs();
  }

  protected switchTab(id: number) {
    if (id === this.activeTabId()) return;
    this.saveCurrent();
    this.activeTabId.set(id);
    const tab = this.tabs().find(t => t.id === id)!;
    this.queryResult.set(tab.result);
    this.translationSql.set(tab.translationSql);
    this.editorPanel?.setQuery(tab.sql);
    this.persistTabs();
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
    this.persistTabs();
  }

  private saveCurrent() {
    const id = this.activeTabId();
    const sql = this.editorPanel?.getQuery() ?? '';
    this.tabs.update(ts => ts.map(t =>
      t.id === id ? { ...t, sql, result: this.queryResult(), translationSql: this.translationSql() } : t
    ));
  }

  protected onQueryChange(sqlText: string) {
    const id = this.activeTabId();
    this.tabs.update(ts => ts.map(t => t.id === id ? { ...t, sql: sqlText } : t));
    clearTimeout(this.persistTimer);
    this.persistTimer = setTimeout(() => this.persistTabs(), 500);
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
