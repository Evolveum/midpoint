/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cases.workitem;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocusMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Mapping between {@link QCaseWorkItemReference} and {@link ObjectReferenceType}.
 * The mapping is the same for all subtypes, see various static `get*()` methods below.
 * Both mapping instances are initialized (`init*()` methods) in {@link QCaseWorkItemMapping}.
 * Init methods can be called multiple times, only one instance for each sub-tables is created.
 */
public class QCaseWorkItemReferenceMapping
        extends QReferenceMapping<QCaseWorkItemReference, MCaseWorkItemReference, QCaseWorkItem, MCaseWorkItem> {

    private static QCaseWorkItemReferenceMapping instanceAssignee;
    private static QCaseWorkItemReferenceMapping instanceCandidate;

    public static QCaseWorkItemReferenceMapping
    initForCaseWorkItemAssignee(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceAssignee, repositoryContext)) {
            instanceAssignee = new QCaseWorkItemReferenceMapping(
                    "m_case_wi_assignee", "cwirefa", repositoryContext,
                    QUserMapping::getUserMapping);
        }
        return getForCaseWorkItemAssignee();
    }

    public static QCaseWorkItemReferenceMapping getForCaseWorkItemAssignee() {
        return Objects.requireNonNull(instanceAssignee);
    }

    public static QCaseWorkItemReferenceMapping
    initForCaseWorkItemCandidate(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceCandidate, repositoryContext)) {
            instanceCandidate = new QCaseWorkItemReferenceMapping(
                    "m_case_wi_candidate", "cwirefc", repositoryContext,
                    QFocusMapping::getFocusMapping);
        }
        return getForCaseWorkItemCandidate();
    }

    public static QCaseWorkItemReferenceMapping getForCaseWorkItemCandidate() {
        return Objects.requireNonNull(instanceCandidate);
    }

    private <TQ extends QObject<TR>, TR extends MObject> QCaseWorkItemReferenceMapping(
            String tableName,
            String defaultAliasName,
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier) {
        super(tableName, defaultAliasName, QCaseWorkItemReference.class,
                repositoryContext, targetMappingSupplier);

        // workItemCid probably can't be mapped directly
    }

    @Override
    protected QCaseWorkItemReference newAliasInstance(String alias) {
        return new QCaseWorkItemReference(alias, tableName());
    }

    @Override
    public MCaseWorkItemReference newRowObject(MCaseWorkItem ownerRow) {
        MCaseWorkItemReference row = new MCaseWorkItemReference();
        row.ownerOid = ownerRow.ownerOid;
        row.ownerType = MObjectType.CASE;
        row.workItemCid = ownerRow.cid;
        return row;
    }

    @Override
    public BiFunction<QCaseWorkItem, QCaseWorkItemReference, Predicate> correlationPredicate() {
        return (a, r) -> a.ownerOid.eq(r.ownerOid)
                .and(a.cid.eq(r.workItemCid));
    }
}
