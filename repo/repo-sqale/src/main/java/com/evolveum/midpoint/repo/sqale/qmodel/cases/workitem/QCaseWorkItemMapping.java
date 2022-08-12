/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cases.workitem;

import static com.evolveum.midpoint.util.MiscUtil.asXMLGregorianCalendar;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.cases.MCase;
import com.evolveum.midpoint.repo.sqale.qmodel.cases.QCaseMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.ResultListRowTransformer;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

/**
 * Mapping between {@link QCaseWorkItem} and {@link CaseWorkItemType}.
 */
public class QCaseWorkItemMapping
        extends QContainerMapping<CaseWorkItemType, QCaseWorkItem, MCaseWorkItem, MCase> {

    public static final String DEFAULT_ALIAS_NAME = "cswi";

    private static QCaseWorkItemMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QCaseWorkItemMapping initCaseWorkItemMapping(
            @NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QCaseWorkItemMapping(repositoryContext);
        }
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QCaseWorkItemMapping getCaseWorkItemMapping() {
        return Objects.requireNonNull(instance);
    }

    private QCaseWorkItemMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QCaseWorkItem.TABLE_NAME, DEFAULT_ALIAS_NAME,
                CaseWorkItemType.class, QCaseWorkItem.class, repositoryContext);

        addRelationResolver(PrismConstants.T_PARENT,
                // mapping supplier is used to avoid cycles in the initialization code
                TableRelationResolver.usingJoin(
                        QCaseMapping::getCaseMapping,
                        (q, p) -> q.ownerOid.eq(p.oid)));

        addItemMapping(F_CLOSE_TIMESTAMP, timestampMapper(q -> q.closeTimestamp));
        addItemMapping(F_CREATE_TIMESTAMP, timestampMapper(q -> q.createTimestamp));
        addItemMapping(F_DEADLINE, timestampMapper(q -> q.deadline));

        addRefMapping(F_ORIGINAL_ASSIGNEE_REF,
                q -> q.originalAssigneeRefTargetOid,
                q -> q.originalAssigneeRefTargetType,
                q -> q.originalAssigneeRefRelationId,
                QUserMapping::getUserMapping);

        addNestedMapping(F_OUTPUT, AbstractWorkItemOutputType.class)
                .addItemMapping(AbstractWorkItemOutputType.F_OUTCOME, stringMapper(q -> q.outcome));

        addRefMapping(F_PERFORMER_REF,
                q -> q.performerRefTargetOid,
                q -> q.performerRefTargetType,
                q -> q.performerRefRelationId,
                QUserMapping::getUserMapping);

        addRefMapping(F_ASSIGNEE_REF,
                QCaseWorkItemReferenceMapping.initForCaseWorkItemAssignee(repositoryContext));
        addRefMapping(F_CANDIDATE_REF,
                QCaseWorkItemReferenceMapping.initForCaseWorkItemCandidate(repositoryContext));

        addItemMapping(F_STAGE_NUMBER, integerMapper(q -> q.stageNumber));

    }

    @Override
    public CaseWorkItemType toSchemaObject(MCaseWorkItem row) {
        CaseWorkItemType cwi = new CaseWorkItemType()
                .id(row.cid)
                .closeTimestamp(asXMLGregorianCalendar(row.closeTimestamp))
                .createTimestamp(asXMLGregorianCalendar(row.createTimestamp))
                .deadline(asXMLGregorianCalendar(row.deadline))
                .originalAssigneeRef(objectReference(row.originalAssigneeRefTargetOid,
                        row.originalAssigneeRefTargetType, row.originalAssigneeRefRelationId))
                .performerRef(objectReference(row.performerRefTargetOid,
                        row.performerRefTargetType, row.performerRefRelationId))
                .stageNumber(row.stageNumber);

        if (row.outcome != null) {
            cwi.output(new AbstractWorkItemOutputType().outcome(row.outcome));
        }
        return cwi;
    }

    @Override
    public ResultListRowTransformer<CaseWorkItemType, QCaseWorkItem, MCaseWorkItem> createRowTransformer(
            SqlQueryContext<CaseWorkItemType, QCaseWorkItem, MCaseWorkItem> sqlQueryContext, JdbcSession jdbcSession) {
        Map<UUID, PrismObject<CaseType>> casesCache = new HashMap<>();

        return (tuple, entityPath, options) -> {
            MCaseWorkItem row = Objects.requireNonNull(tuple.get(entityPath));
            UUID caseOid = row.ownerOid;
            PrismObject<CaseType> aCase = casesCache.get(caseOid);
            if (aCase == null) {
                aCase = ((SqaleQueryContext<CaseWorkItemType, QCaseWorkItem, MCaseWorkItem>) sqlQueryContext)
                        .loadObject(jdbcSession, CaseType.class, caseOid, options);
                casesCache.put(caseOid, aCase);
            }

            PrismContainer<CaseWorkItemType> workItemContainer = aCase.findContainer(CaseType.F_WORK_ITEM);
            if (workItemContainer == null) {
                throw new SystemException("Case " + aCase + " has no work items even if it should have " + tuple);
            }
            PrismContainerValue<CaseWorkItemType> workItemPcv = workItemContainer.findValue(row.cid);
            if (workItemPcv == null) {
                throw new SystemException("Case " + aCase + " has no work item with ID " + row.cid);
            }
            return workItemPcv.asContainerable();
        };
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
