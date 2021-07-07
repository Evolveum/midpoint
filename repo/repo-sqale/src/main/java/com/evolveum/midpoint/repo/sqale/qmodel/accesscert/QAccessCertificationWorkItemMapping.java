/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import static com.evolveum.midpoint.util.MiscUtil.asXMLGregorianCalendar;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType.*;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

/**
 * Mapping between {@link QAccessCertificationWorkItem} and {@link AccessCertificationWorkItemType}.
 */
public class QAccessCertificationWorkItemMapping
        extends QContainerMapping<AccessCertificationWorkItemType, QAccessCertificationWorkItem, MAccessCertificationWorkItem, MAccessCertificationCampaign> {

    public static final String DEFAULT_ALIAS_NAME = "acwi";

    private static QAccessCertificationWorkItemMapping instance;

    public static QAccessCertificationWorkItemMapping init(
            @NotNull SqaleRepoContext repositoryContext) {
        if (instance == null) {
            instance = new QAccessCertificationWorkItemMapping(repositoryContext);
        }
        return get();
    }

    public static QAccessCertificationWorkItemMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QAccessCertificationWorkItemMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QAccessCertificationWorkItem.TABLE_NAME, DEFAULT_ALIAS_NAME,
                AccessCertificationWorkItemType.class, QAccessCertificationWorkItem.class, repositoryContext);

        addItemMapping(F_CLOSE_TIMESTAMP, timestampMapper(q -> q.closeTimestamp));
        // TODO: iteration -> campaignIteration
        addItemMapping(F_ITERATION, integerMapper(q -> q.campaignIteration));
        addNestedMapping(F_OUTPUT, AbstractWorkItemOutputType.class)
                .addItemMapping(AbstractWorkItemOutputType.F_OUTCOME, stringMapper(q -> q.outcome));
        addItemMapping(F_OUTPUT_CHANGE_TIMESTAMP, timestampMapper(q -> q.outputChangeTimestamp));
        addItemMapping(F_PERFORMER_REF, refMapper(
                q -> q.performerRefTargetOid,
                q -> q.performerRefTargetType,
                q -> q.performerRefRelationId));

        addRefMapping(F_ASSIGNEE_REF,
                QAccessCertificationWorkItemReferenceMapping.initForCaseWorkItemAssignee(repositoryContext));
        addRefMapping(F_CANDIDATE_REF,
                QAccessCertificationWorkItemReferenceMapping.initForCaseWorkItemCandidate(repositoryContext));

        addItemMapping(F_STAGE_NUMBER, integerMapper(q -> q.stageNumber));

    }

    @Override
    public AccessCertificationWorkItemType toSchemaObject(MAccessCertificationWorkItem row) {
        AccessCertificationWorkItemType acwi = new AccessCertificationWorkItemType(prismContext())
                .closeTimestamp(asXMLGregorianCalendar(row.closeTimestamp))
                .iteration(row.campaignIteration)
                .outputChangeTimestamp(asXMLGregorianCalendar(row.outputChangeTimestamp))
                .performerRef(objectReference(row.performerRefTargetOid,
                        row.performerRefTargetType, row.performerRefRelationId))
                .stageNumber(row.stageNumber);

        if (row.outcome != null) {
            acwi.output(new AbstractWorkItemOutputType(prismContext()).outcome(row.outcome));
        }

        return acwi;
    }

    @Override
    protected QAccessCertificationWorkItem newAliasInstance(String alias) {
        return new QAccessCertificationWorkItem(alias);
    }

    @Override
    public MAccessCertificationWorkItem newRowObject() {
        return new MAccessCertificationWorkItem();
    }

    @Override
    public MAccessCertificationWorkItem newRowObject(MAccessCertificationCampaign ownerRow) {
        MAccessCertificationWorkItem row = newRowObject();
        row.ownerOid = ownerRow.oid;
        return row;
    }

    // about duplication see the comment in QObjectMapping.toRowObjectWithoutFullObject
    @SuppressWarnings("DuplicatedCode")
    public MAccessCertificationWorkItem insert(
            AccessCertificationWorkItemType workItem,
            MAccessCertificationCampaign campaignRow,
            MAccessCertificationCase caseRow,
            JdbcSession jdbcSession) {
        MAccessCertificationWorkItem row = initRowObject(workItem, campaignRow);
        row.accessCertCaseCid = caseRow.cid;

        row.closeTimestamp = MiscUtil.asInstant(workItem.getCloseTimestamp());
        // TODO: iteration -> campaignIteration
        row.campaignIteration = workItem.getIteration();

        AbstractWorkItemOutputType output = workItem.getOutput();
        if (output != null) {
            row.outcome = output.getOutcome();
        }

        row.outputChangeTimestamp = MiscUtil.asInstant(workItem.getOutputChangeTimestamp());

        setReference(workItem.getPerformerRef(),
                o -> row.performerRefTargetOid = o,
                t -> row.performerRefTargetType = t,
                r -> row.performerRefRelationId = r);

        row.stageNumber = workItem.getStageNumber();

        insert(row, jdbcSession);

        storeRefs(row, workItem.getAssigneeRef(),
                QAccessCertificationWorkItemReferenceMapping.getForCaseWorkItemAssignee(), jdbcSession);
        storeRefs(row, workItem.getCandidateRef(),
                QAccessCertificationWorkItemReferenceMapping.getForCaseWorkItemCandidate(), jdbcSession);

        return row;
    }
}
