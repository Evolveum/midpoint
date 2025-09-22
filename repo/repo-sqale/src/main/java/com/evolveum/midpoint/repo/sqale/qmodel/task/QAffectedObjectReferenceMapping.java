/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.task;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;

import com.evolveum.midpoint.repo.sqale.qmodel.simulation.QProcessedObjectEventMarkReferenceMapping;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.*;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Mapping between {@link QAffectedObjectReference} and {@link ObjectReferenceType}.
 */
public class QAffectedObjectReferenceMapping
        extends QReferenceMapping<QAffectedObjectReference, MAffectedObjectReference, QAffectedObjects, MAffectedObjects> {

    public static final Class<QAffectedObjectReference> TYPE = QAffectedObjectReference.class;

    public static QAffectedObjectReferenceMapping instance;

    public static QAffectedObjectReferenceMapping
    init(SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QAffectedObjectReferenceMapping(
                    "m_ref_task_affected_object", "reftao", repositoryContext,
                    QObjectMapping::getObjectMapping);
        }
        return getInstance();
    }

    public static QAffectedObjectReferenceMapping
    getInstance() {
        //noinspection unchecked
        return (QAffectedObjectReferenceMapping)
                Objects.requireNonNull(instance);
    }

    private <TQ extends QObject<TR>, TR extends MObject> QAffectedObjectReferenceMapping(
            String tableName,
            String defaultAliasName,
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier) {
        super(tableName, defaultAliasName, TYPE, repositoryContext, targetMappingSupplier);
    }

    @Override
    protected QAffectedObjectReference newAliasInstance(String alias) {
        return new QAffectedObjectReference(alias, tableName());
    }

    @Override
    public MAffectedObjectReference newRowObject(MAffectedObjects ownerRow) {
        MAffectedObjectReference row = new MAffectedObjectReference();
        row.ownerOid = ownerRow.ownerOid;
        row.ownerType = ownerRow.type;
        // This should be task, not sure why ownerRow.type is empty
        row.ownerType = MObjectType.TASK;
        row.affectedObjectCid = ownerRow.cid;
        return row;
    }

    @Override
    public BiFunction<QAffectedObjects, QAffectedObjectReference, Predicate> correlationPredicate() {
        return (a, r) -> a.ownerOid.eq(r.ownerOid)
                .and(a.cid.eq(r.affectedObjectCid)
                );
    }
}
