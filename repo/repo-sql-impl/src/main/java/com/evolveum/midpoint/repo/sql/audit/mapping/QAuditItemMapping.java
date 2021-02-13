/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit.mapping;

import static com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditItem.TABLE_NAME;

import com.evolveum.midpoint.repo.sql.audit.beans.MAuditItem;
import com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditItem;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Mapping for {@link QAuditItem}, model type is non-containerable {@link ItemPathType}.
 */
public class QAuditItemMapping
        extends QueryTableMapping<ItemPathType, QAuditItem, MAuditItem> {

    public static final String DEFAULT_ALIAS_NAME = "ai";

    public static final QAuditItemMapping INSTANCE = new QAuditItemMapping();

    private QAuditItemMapping() {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                ItemPathType.class, QAuditItem.class);
    }

    @Override
    protected QAuditItem newAliasInstance(String alias) {
        return new QAuditItem(alias);
    }
}
