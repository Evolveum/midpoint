/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.querydsl.sql.ColumnMetadata;

public class ExtensionColumns {

    private final Map<String, ColumnMetadata> columnMetadata = new LinkedHashMap<>();

    public void registerExtensionColumn(ColumnMetadata column) {
        columnMetadata.put(column.getName(), column);
    }

    public Set<Map.Entry<String, ColumnMetadata>> extensionColumnsMetadata() {
        return Collections.unmodifiableSet(columnMetadata.entrySet());
    }

}
