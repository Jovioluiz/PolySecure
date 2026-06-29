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

@Component({
  selector: 'app-root',
  imports: [Login, Navbar, Sidebar, EditorPanel, TranslationPanel, ResultsPanel, HelpModal],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly catalog = inject(CatalogService);

  protected queryResult = signal<QueryResult | { error: string } | null>(null);
  protected translationSql = signal<string | null>(null);
  protected showHelp = signal(false);

  @ViewChild(EditorPanel) private editorPanel?: EditorPanel;

  constructor() {
    if (this.auth.isLoggedIn()) {
      this.catalog.load();
    }
  }

  protected onLoggedIn() {
    this.catalog.load();
  }

  protected onTableSelected(sql: string) {
    this.editorPanel?.setQuery(sql);
  }

  protected onResult(r: QueryResult | { error: string }) {
    this.queryResult.set(r);
  }

  protected onTranslation(sql: string) {
    this.translationSql.set(sql);
  }
}
