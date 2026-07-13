/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.model;

public sealed interface Statement
    permits SelectStatement, InsertStatement, InsertSelectStatement,
            UpdateStatement, DeleteStatement, CreateTableStatement, DropTableStatement,
            UnionStatement, ExceptStatement, RegisterStoreStatement,
            CreateIndexStatement, AlterTableStatement {}
