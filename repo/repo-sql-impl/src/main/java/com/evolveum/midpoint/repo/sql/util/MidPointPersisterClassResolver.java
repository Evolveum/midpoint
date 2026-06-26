/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.util;

import java.util.Set;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.internal.StandardPersisterClassResolver;
import org.hibernate.persister.spi.PersisterClassResolver;

/**
 * Hibernate 7 replacement for removed @Persister mapping annotations used by the old generic repository.
 */
public class MidPointPersisterClassResolver implements PersisterClassResolver {

    private static final Set<String> JOINED_PERSISTER_ENTITIES = Set.of(
            "com.evolveum.midpoint.repo.sql.data.common.RAbstractRole",
            "com.evolveum.midpoint.repo.sql.data.common.RAccessCertificationDefinition",
            "com.evolveum.midpoint.repo.sql.data.common.RArchetype",
            "com.evolveum.midpoint.repo.sql.data.common.RCase",
            "com.evolveum.midpoint.repo.sql.data.common.RConnector",
            "com.evolveum.midpoint.repo.sql.data.common.RConnectorHost",
            "com.evolveum.midpoint.repo.sql.data.common.RDashboard",
            "com.evolveum.midpoint.repo.sql.data.common.RForm",
            "com.evolveum.midpoint.repo.sql.data.common.RFunctionLibrary",
            "com.evolveum.midpoint.repo.sql.data.common.RGenericObject",
            "com.evolveum.midpoint.repo.sql.data.common.RLookupTable",
            "com.evolveum.midpoint.repo.sql.data.common.RMessageTemplate",
            "com.evolveum.midpoint.repo.sql.data.common.RNode",
            "com.evolveum.midpoint.repo.sql.data.common.RObjectCollection",
            "com.evolveum.midpoint.repo.sql.data.common.RReport",
            "com.evolveum.midpoint.repo.sql.data.common.RReportData",
            "com.evolveum.midpoint.repo.sql.data.common.RRole",
            "com.evolveum.midpoint.repo.sql.data.common.RSecurityPolicy",
            "com.evolveum.midpoint.repo.sql.data.common.RSequence",
            "com.evolveum.midpoint.repo.sql.data.common.RService",
            "com.evolveum.midpoint.repo.sql.data.common.RSystemConfiguration",
            "com.evolveum.midpoint.repo.sql.data.common.RUser",
            "com.evolveum.midpoint.repo.sql.data.common.RValuePolicy");

    private static final Set<String> SINGLE_TABLE_PERSISTER_ENTITIES = Set.of(
            "com.evolveum.midpoint.repo.sql.data.common.RObjectReference",
            "com.evolveum.midpoint.repo.sql.data.common.container.RAssignmentReference",
            "com.evolveum.midpoint.repo.sql.data.common.container.RCaseWorkItemReference",
            "com.evolveum.midpoint.repo.sql.data.common.container.RCertWorkItemReference");

    private final StandardPersisterClassResolver delegate = new StandardPersisterClassResolver();

    @Override
    public Class<? extends EntityPersister> getEntityPersisterClass(PersistentClass model) {
        String entityName = model.getClassName();
        if (JOINED_PERSISTER_ENTITIES.contains(entityName)) {
            return MidPointJoinedPersister.class;
        }
        if (SINGLE_TABLE_PERSISTER_ENTITIES.contains(entityName)) {
            return MidPointSingleTablePersister.class;
        }
        return delegate.getEntityPersisterClass(model);
    }

    @Override
    public Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata) {
        return delegate.getCollectionPersisterClass(metadata);
    }
}
