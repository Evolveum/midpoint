/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cases.workitem;

import java.util.Objects;
import java.util.function.BiFunction;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.*;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
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
        if (instanceAssignee == null) {
            instanceAssignee = new QCaseWorkItemReferenceMapping(
                    "m_case_wi_assignee", "mcwirefa", repositoryContext);
        }
        return getForCaseWorkItemAssignee();
    }

    public static QCaseWorkItemReferenceMapping
    getForCaseWorkItemAssignee() {
        //noinspection unchecked
        return Objects.requireNonNull(instanceAssignee);
    }

    public static QCaseWorkItemReferenceMapping
    initForCaseWorkItemCandidate(@NotNull SqaleRepoContext repositoryContext) {
        if (instanceCandidate == null) {
            instanceCandidate = new QCaseWorkItemReferenceMapping(
                    "m_case_wi_candidate", "mcwirefc", repositoryContext);
        }
        return getForCaseWorkItemCandidate();
    }

    public static QCaseWorkItemReferenceMapping
    getForCaseWorkItemCandidate() {
        //noinspection unchecked
        return Objects.requireNonNull(instanceCandidate);
    }



    private QCaseWorkItemReferenceMapping(
            String tableName,
            String defaultAliasName,
            @NotNull SqaleRepoContext repositoryContext) {
        super(tableName, defaultAliasName, QCaseWorkItemReference.class, repositoryContext);

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
    public BiFunction<QCaseWorkItem, QCaseWorkItemReference, Predicate> joinOnPredicate() {
        return (a, r) -> a.ownerOid.eq(r.ownerOid).and(a.cid.eq(r.workItemCid));
    }
}
