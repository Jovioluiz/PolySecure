/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

import {
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  afterNextRender,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { basicSetup, EditorView } from 'codemirror';
import { EditorState } from '@codemirror/state';
import { keymap } from '@codemirror/view';
import { sql } from '@codemirror/lang-sql';
import { HighlightStyle, syntaxHighlighting } from '@codemirror/language';
import { tags } from '@lezer/highlight';
import { QueryMode, QueryResult } from '../../models/types';
import { isClientTimeoutError, QueryService } from '../../services/query.service';

// Presets shown in the timeout selector. `null` disables the client-side timeout entirely —
// some analytical queries (large/unindexed tables) legitimately take several minutes; see the
// "0 Unknown Error" investigation this was added for.
const TIMEOUT_PRESETS: { label: string; valueMs: number | null }[] = [
  { label: '30s', valueMs: 30_000 },
  { label: '60s', valueMs: 60_000 },
  { label: '2 min', valueMs: 120_000 },
  { label: '5 min', valueMs: 300_000 },
  { label: 'Sem limite', valueMs: null },
];

const darkTheme = EditorView.theme(
  {
    '&': { background: '#0d1117', color: '#c9d1d9', height: '100%', fontSize: '13px', fontFamily: 'var(--font-mono)' },
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

// basicSetup already registers syntaxHighlighting(defaultHighlightStyle), whose colors are
// tuned for a light background. Without this override, keywords/strings/etc. render in dark
// tones nearly invisible against the #0d1117 background above.
const darkHighlightStyle = HighlightStyle.define([
  { tag: tags.keyword, color: '#ff7b72', fontWeight: 'bold' },
  { tag: [tags.name, tags.deleted, tags.character, tags.propertyName, tags.macroName], color: '#c9d1d9' },
  { tag: [tags.function(tags.variableName), tags.labelName], color: '#d2a8ff' },
  { tag: [tags.color, tags.constant(tags.name), tags.standard(tags.name)], color: '#79c0ff' },
  { tag: [tags.definition(tags.name), tags.separator], color: '#c9d1d9' },
  { tag: [tags.typeName, tags.className, tags.number, tags.changed, tags.annotation, tags.modifier, tags.self, tags.namespace], color: '#79c0ff' },
  { tag: [tags.operator, tags.operatorKeyword], color: '#ff7b72' },
  { tag: [tags.url, tags.escape, tags.regexp, tags.link], color: '#a5d6ff' },
  { tag: tags.string, color: '#a5d6ff' },
  { tag: tags.comment, color: '#8b949e', fontStyle: 'italic' },
  { tag: tags.meta, color: '#8b949e' },
  { tag: tags.invalid, color: '#f85149' },
]);

@Component({
  selector: 'app-editor-panel',
  imports: [FormsModule],
  templateUrl: './editor-panel.html',
  styleUrl: './editor-panel.scss',
})
export class EditorPanel implements OnDestroy {
  readonly defaultQuery = input('-- Exemplo: buscar dados de tabela cross-store\nSELECT *\nFROM orders\nWHERE id = 1');

  readonly result = output<QueryResult | { error: string }>();
  readonly translationReady = output<string>();
  readonly modeChange = output<QueryMode>();
  readonly queryChange = output<string>();

  protected readonly queryService = inject(QueryService);

  protected mode = signal<QueryMode>('standard');
  protected running = signal(false);
  protected translating = signal(false);

  protected readonly timeoutPresets = TIMEOUT_PRESETS;
  protected timeoutMs = signal<number | null>(60_000);
  protected elapsedSeconds = signal(0);
  private elapsedTimer?: ReturnType<typeof setInterval>;

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
        extensions: [
          basicSetup,
          sql(),
          darkTheme,
          syntaxHighlighting(darkHighlightStyle),
          runCmd,
          EditorView.lineWrapping,
          EditorView.updateListener.of(update => {
            if (update.docChanged) this.queryChange.emit(update.state.doc.toString());
          }),
        ],
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
    this.startElapsedTimer();
    const timeoutMs = this.timeoutMs();
    this.queryService.execute(sqlText, this.mode(), timeoutMs).subscribe({
      next: res => { this.stopRun(); this.result.emit(res); },
      error: err => {
        this.stopRun();
        this.result.emit({ error: this.describeError(err, timeoutMs) });
      },
    });
  }

  private stopRun() {
    this.running.set(false);
    this.stopElapsedTimer();
  }

  // Distinguishes a client-side give-up (query may well still be running on the server — there
  // is no cancellation propagated to the DB driver) from a real error the backend reported, so
  // the message doesn't read as "your query failed" when it may just still be working.
  private describeError(err: unknown, timeoutMs: number | null): string {
    if (isClientTimeoutError(err)) {
      const label = this.timeoutPresets.find(p => p.valueMs === timeoutMs)?.label ?? `${timeoutMs}ms`;
      return `Sem resposta em ${label}. A consulta pode continuar rodando no servidor — `
        + `tabelas grandes ou sem índice adequado podem levar minutos. Tente de novo mais tarde `
        + `ou aumente o timeout no seletor ao lado do botão Executar.`;
    }
    const e = err as { error?: { message?: string }; message?: string };
    return e?.error?.message ?? e?.message ?? 'Erro desconhecido';
  }

  private startElapsedTimer() {
    this.elapsedSeconds.set(0);
    this.elapsedTimer = setInterval(() => this.elapsedSeconds.update(s => s + 1), 1000);
  }

  private stopElapsedTimer() {
    if (this.elapsedTimer !== undefined) {
      clearInterval(this.elapsedTimer);
      this.elapsedTimer = undefined;
    }
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

  ngOnDestroy() {
    this.stopElapsedTimer();
  }
}
