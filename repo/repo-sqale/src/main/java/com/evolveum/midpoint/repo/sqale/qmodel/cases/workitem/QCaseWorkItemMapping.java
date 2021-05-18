/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cases.workitem;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.cases.MCase;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerType;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType.*;

/**
 * Mapping between {@link QCaseWorkItem} and {@link CaseWorkItemType}.
 *
 * @param <OR> type of the owner row
 */
public class QCaseWorkItemMapping<OR extends MCase>
        extends QContainerMapping<CaseWorkItemType, QCaseWorkItem<OR>, MCaseWorkItem, OR> {

    public static final String DEFAULT_ALIAS_NAME = "cswi";

    private final MContainerType containerType;

    // We can't declare Class<QCaseWorkItem<OR>>.class, so we cheat a bit.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private QCaseWorkItemMapping(
            @NotNull MContainerType containerType,
            @NotNull SqaleRepoContext repositoryContext) {
        super(QCaseWorkItem.TABLE_NAME, DEFAULT_ALIAS_NAME,
                CaseWorkItemType.class, (Class) QCaseWorkItem.class, repositoryContext);
        this.containerType = containerType;

        addItemMapping(F_CLOSE_TIMESTAMP, timestampMapper(q -> q.closeTimestamp));
        addItemMapping(F_CREATE_TIMESTAMP, timestampMapper(q -> q.createTimestamp));
        addItemMapping(F_DEADLINE, timestampMapper(q -> q.deadline));

        addItemMapping(F_ORIGINAL_ASSIGNEE_REF, refMapper(
                q -> q.originalAssigneeRefTargetOid,
                q -> q.originalAssigneeRefTargetType,
                q -> q.originalAssigneeRefRelationId));

        // TODO: OUTCOME
//        addItemMapping(F_OUTCOME, stringMapper(q -> q.outcome));

        addItemMapping(F_PERFORMER_REF, refMapper(
                q -> q.performerRefTargetOid,
                q -> q.performerRefTargetType,
                q -> q.performerRefRelationId));

        addItemMapping(F_STAGE_NUMBER, integerMapper(q -> q.stageNumber));

    }

    @Override
    protected QCaseWorkItem<OR> newAliasInstance(String alias) {
        return new QCaseWorkItem<>(alias);
    }

    @Override
    public MCaseWorkItem newRowObject() {
        MCaseWorkItem row = new MCaseWorkItem();
        row.containerType = this.containerType;
        return row;
    }

    @Override
    public MCaseWorkItem newRowObject(OR ownerRow) {
        MCaseWorkItem row = newRowObject();
        row.ownerOid = ownerRow.oid;
        return row;
    }

    // about duplication see the comment in QObjectMapping.toRowObjectWithoutFullObject
    @SuppressWarnings("DuplicatedCode")
    @Override
    public MCaseWorkItem insert(CaseWorkItemType workItem, OR ownerRow, JdbcSession jdbcSession) {
        MCaseWorkItem row = initRowObject(workItem, ownerRow);

        row.closeTimestamp = MiscUtil.asInstant(workItem.getCloseTimestamp());
        row.createTimestamp = MiscUtil.asInstant(workItem.getCreateTimestamp());
        row.deadline = MiscUtil.asInstant(workItem.getDeadline());
        setReference(workItem.getOriginalAssigneeRef(),
                o -> row.originalAssigneeRefTargetOid = o,
                t -> row.originalAssigneeRefTargetType = t,
                r -> row.originalAssigneeRefRelationId = r);

        // TODO: Outcome

        setReference(workItem.getPerformerRef(),
                o -> row.performerRefTargetOid = o,
                t -> row.performerRefTargetType = t,
                r -> row.performerRefRelationId = r);
        row.stageNumber = workItem.getStageNumber();

        insert(row, jdbcSession);

        return row;
    }
}
