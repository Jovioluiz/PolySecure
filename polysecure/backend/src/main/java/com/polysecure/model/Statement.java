package com.polysecure.model;

public sealed interface Statement
    permits SelectStatement, InsertStatement, InsertSelectStatement,
            UpdateStatement, DeleteStatement, CreateTableStatement, DropTableStatement {}
