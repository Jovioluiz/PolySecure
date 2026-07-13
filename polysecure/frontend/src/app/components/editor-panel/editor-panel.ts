/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

import {
  Component,
  ElementRef,
  ViewChild,
  afterNextRender,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { basicSetup, EditorView } from 'codemirror';
import { EditorState } from '@codemirror/state';
import { keymap } from '@codemirror/view';
import { sql } from '@codemirror/lang-sql';
import { QueryMode, QueryResult } from '../../models/types';
import { QueryService } from '../../services/query.service';

const darkTheme = EditorView.theme(
  {
    '&': { background: '#0d1117', color: '#c9d1d9', height: '180px', fontSize: '13px', fontFamily: 'var(--font-mono)' },
    '.cm-content': { caretColor: '#58a6ff' },
    '.cm-cursor': { borderLeftColor: '#58a6ff' },
    '.cm-gutters': { background: '#161b22', color: '#8b949e', border: 'none' },
    '.cm-activeLineGutter': { background: '#1c2128' },
    '.cm-activeLine': { background: '#1c2128' },
    '.cm-selectionBackground, ::selection': { background: '#264f78 !important' },
    '.cm-matchingBracket': { color: '#79c0ff', fontWeight: 'bold' },
  },
  { dark: true },
);

@Component({
  selector: 'app-editor-panel',
  templateUrl: './editor-panel.html',
  styleUrl: './editor-panel.scss',
})
export class EditorPanel {
  readonly defaultQuery = input('-- Exemplo: buscar dados de tabela cross-store\nSELECT *\nFROM orders\nWHERE id = 1');

  readonly result = output<QueryResult | { error: string }>();
  readonly translationReady = output<string>();
  readonly modeChange = output<QueryMode>();

  protected readonly queryService = inject(QueryService);

  protected mode = signal<QueryMode>('standard');
  protected running = signal(false);
  protected translating = signal(false);

  @ViewChild('editorWrap') private editorWrapRef!: ElementRef<HTMLDivElement>;

  private editor?: EditorView;

  constructor() {
    afterNextRender(() => {
      const runCmd = keymap.of([
        { key: 'F5', run: () => { this.run(); return true; } },
        { key: 'Ctrl-Enter', run: () => { this.run(); return true; } },
        { key: 'Mod-Enter', run: () => { this.run(); return true; } },
      ]);
      const state = EditorState.create({
        doc: this.defaultQuery(),
        extensions: [basicSetup, sql(), darkTheme, runCmd, EditorView.lineWrapping],
      });
      this.editor = new EditorView({ state, parent: this.editorWrapRef.nativeElement });
    });
  }

  getQuery(): string {
    return this.editor?.state.doc.toString() ?? '';
  }

  setQuery(sqlText: string) {
    if (!this.editor) return;
    this.editor.dispatch({
      changes: { from: 0, to: this.editor.state.doc.length, insert: sqlText },
    });
    this.editor.focus();
  }

  protected setMode(m: QueryMode) {
    this.mode.set(m);
    this.modeChange.emit(m);
  }

  protected run() {
    const sqlText = this.editor?.state.doc.toString().trim();
    if (!sqlText) return;
    this.running.set(true);
    this.queryService.execute(sqlText, this.mode()).subscribe({
      next: res => { this.running.set(false); this.result.emit(res); },
      error: err => {
        this.running.set(false);
        const msg = err?.error?.message ?? err?.message ?? 'Erro desconhecido';
        this.result.emit({ error: msg });
      },
    });
  }

  protected translate() {
    const sqlText = this.editor?.state.doc.toString().trim();
    if (!sqlText) return;
    this.translating.set(true);
    this.queryService.translate(sqlText).subscribe({
      next: res => { this.translating.set(false); this.translationReady.emit(res.translated); },
      error: () => { this.translating.set(false); },
    });
  }
}
