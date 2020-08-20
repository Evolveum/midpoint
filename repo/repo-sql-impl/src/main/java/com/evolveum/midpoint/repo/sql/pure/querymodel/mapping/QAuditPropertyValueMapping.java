/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.querymodel.mapping;

import static com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditPropertyValue.TABLE_NAME;

import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditPropertyValue;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditPropertyValue;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordPropertyType;

/**
 * Mapping for {@link QAuditPropertyValue}.
 */
public class QAuditPropertyValueMapping
        extends QueryModelMapping<AuditEventRecordPropertyType, QAuditPropertyValue, MAuditPropertyValue> {

    public static final String DEFAULT_ALIAS_NAME = "apv";

    public static final QAuditPropertyValueMapping INSTANCE = new QAuditPropertyValueMapping();

    private QAuditPropertyValueMapping() {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                AuditEventRecordPropertyType.class, QAuditPropertyValue.class);
    }

    @Override
    protected QAuditPropertyValue newAliasInstance(String alias) {
        return new QAuditPropertyValue(alias);
    }
}
