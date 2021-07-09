/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cases.workitem;

import static com.evolveum.midpoint.util.MiscUtil.asXMLGregorianCalendar;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType.*;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.cases.MCase;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

/**
 * Mapping between {@link QCaseWorkItem} and {@link CaseWorkItemType}.
 */
public class QCaseWorkItemMapping
        extends QContainerMapping<CaseWorkItemType, QCaseWorkItem, MCaseWorkItem, MCase> {

    public static final String DEFAULT_ALIAS_NAME = "cswi";

    private static QCaseWorkItemMapping instance;

    public static QCaseWorkItemMapping init(
            @NotNull SqaleRepoContext repositoryContext) {
        if (instance == null) {
            instance = new QCaseWorkItemMapping(repositoryContext);
        }
        return get();
    }

    public static QCaseWorkItemMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QCaseWorkItemMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QCaseWorkItem.TABLE_NAME, DEFAULT_ALIAS_NAME,
                CaseWorkItemType.class, QCaseWorkItem.class, repositoryContext);

        addItemMapping(F_CLOSE_TIMESTAMP, timestampMapper(q -> q.closeTimestamp));
        addItemMapping(F_CREATE_TIMESTAMP, timestampMapper(q -> q.createTimestamp));
        addItemMapping(F_DEADLINE, timestampMapper(q -> q.deadline));

        addItemMapping(F_ORIGINAL_ASSIGNEE_REF, refMapper(
                q -> q.originalAssigneeRefTargetOid,
                q -> q.originalAssigneeRefTargetType,
                q -> q.originalAssigneeRefRelationId));

        addNestedMapping(F_OUTPUT, AbstractWorkItemOutputType.class)
                .addItemMapping(AbstractWorkItemOutputType.F_OUTCOME, stringMapper(q -> q.outcome));

        addItemMapping(F_PERFORMER_REF, refMapper(
                q -> q.performerRefTargetOid,
                q -> q.performerRefTargetType,
                q -> q.performerRefRelationId));

        addRefMapping(F_ASSIGNEE_REF,
                QCaseWorkItemReferenceMapping.initForCaseWorkItemAssignee(repositoryContext));
        addRefMapping(F_CANDIDATE_REF,
                QCaseWorkItemReferenceMapping.initForCaseWorkItemCandidate(repositoryContext));

        addItemMapping(F_STAGE_NUMBER, integerMapper(q -> q.stageNumber));

    }

    @Override
    public CaseWorkItemType toSchemaObject(MCaseWorkItem row) {
        CaseWorkItemType cwi = new CaseWorkItemType(prismContext())
                .closeTimestamp(asXMLGregorianCalendar(row.closeTimestamp))
                .createTimestamp(asXMLGregorianCalendar(row.createTimestamp))
                .deadline(asXMLGregorianCalendar(row.deadline))
                .originalAssigneeRef(objectReference(row.originalAssigneeRefTargetOid,
                        row.originalAssigneeRefTargetType, row.originalAssigneeRefRelationId))
                .performerRef(objectReference(row.performerRefTargetOid,
                        row.performerRefTargetType, row.performerRefRelationId))
                .stageNumber(row.stageNumber);

        if (row.outcome != null) {
            cwi.output(new AbstractWorkItemOutputType(prismContext()).outcome(row.outcome));
        }

        return cwi;
    }

    @Override
    protected QCaseWorkItem newAliasInstance(String alias) {
        return new QCaseWorkItem(alias);
    }

    @Override
    public MCaseWorkItem newRowObject() {
        return new MCaseWorkItem();
    }

    @Override
    public MCaseWorkItem newRowObject(MCase ownerRow) {
        MCaseWorkItem row = newRowObject();
        row.ownerOid = ownerRow.oid;
        return row;
    }

    // about duplication see the comment in QObjectMapping.toRowObjectWithoutFullObject
    @SuppressWarnings("DuplicatedCode")
    @Override
    public MCaseWorkItem insert(CaseWorkItemType workItem, MCase ownerRow, JdbcSession jdbcSession) {
        MCaseWorkItem row = initRowObject(workItem, ownerRow);

        row.closeTimestamp = MiscUtil.asInstant(workItem.getCloseTimestamp());
        row.createTimestamp = MiscUtil.asInstant(workItem.getCreateTimestamp());
        row.deadline = MiscUtil.asInstant(workItem.getDeadline());
        setReference(workItem.getOriginalAssigneeRef(),
                o -> row.originalAssigneeRefTargetOid = o,
                t -> row.originalAssigneeRefTargetType = t,
                r -> row.originalAssigneeRefRelationId = r);

        AbstractWorkItemOutputType output = workItem.getOutput();
        if (output != null) {
            row.outcome = output.getOutcome();
        }

        setReference(workItem.getPerformerRef(),
                o -> row.performerRefTargetOid = o,
                t -> row.performerRefTargetType = t,
                r -> row.performerRefRelationId = r);
        row.stageNumber = workItem.getStageNumber();

        insert(row, jdbcSession);

        storeRefs(row, workItem.getAssigneeRef(),
                QCaseWorkItemReferenceMapping.getForCaseWorkItemAssignee(), jdbcSession);
        storeRefs(row, workItem.getCandidateRef(),
                QCaseWorkItemReferenceMapping.getForCaseWorkItemCandidate(), jdbcSession);

        return row;
    }
}
