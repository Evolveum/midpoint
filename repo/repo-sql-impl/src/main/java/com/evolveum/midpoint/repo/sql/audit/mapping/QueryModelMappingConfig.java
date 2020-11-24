/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit.mapping;

import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMappingRegistry;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Holds {@link QueryModelMapping} instances obtainable by various key (e.g. model type Q-name).
 * <p>
 * TODO: This should probably not be global, but different for audit-repo and main-repo.
 * It also should not be accessed statically from sqlbase classes (it can't actually).
 * Some form of injection is needed.
 */
public class QueryModelMappingConfig {

    public static final QueryModelMappingRegistry AUDIT_MAPPING = new QueryModelMappingRegistry()
            .register(AuditEventRecordType.COMPLEX_TYPE, QAuditEventRecordMapping.INSTANCE)
            .register(QAuditItemMapping.INSTANCE)
            .register(QAuditPropertyValueMapping.INSTANCE)
            .register(QAuditRefValueMapping.INSTANCE)
            .register(QAuditResourceMapping.INSTANCE)
            .register(QAuditDeltaMapping.INSTANCE)
            .seal();
}
