/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.parser.sql;

import java.util.Optional;

import com.evolveum.midpoint.tools.dbdocs.model.DocMetadata;
import com.evolveum.midpoint.tools.dbdocs.model.IndexDoc;
import com.evolveum.midpoint.tools.dbdocs.parser.SqlParserSupport;

import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.Index;

/**
 * Parses CREATE INDEX statements into index documentation models.
 */
public class SqlIndexParser {

    /**
     * Tries to parse a statement as CREATE INDEX using JSqlParser.
     */
    public Optional<IndexDoc> parseIfSupported(String statement, DocMetadata metadata) {
        return SqlParserSupport.parseAs(statement, CreateIndex.class)
                .map(createIndex -> toIndexDoc(createIndex, metadata));
    }

    private IndexDoc toIndexDoc(CreateIndex createIndex, DocMetadata metadata) {
        Index index = createIndex.getIndex();
        String type = index.getType() != null ? index.getType() : "";
        return new IndexDoc(
                SqlParserSupport.normalizeIdentifier(index.getName()),
                SqlParserSupport.normalizeIdentifier(createIndex.getTable().getFullyQualifiedName()),
                metadata,
                String.join(", ", SqlParserSupport.columnNames(index)),
                type.toUpperCase().contains("UNIQUE"),
                index.getUsing() != null ? index.getUsing() : "");
    }
}
