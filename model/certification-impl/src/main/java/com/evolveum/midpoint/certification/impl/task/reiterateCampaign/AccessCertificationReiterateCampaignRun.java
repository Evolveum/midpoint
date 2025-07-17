/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task.reiterateCampaign;

import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CREATED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.*;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.cases.api.util.QueryUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.certification.impl.AccCertUpdateHelper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Execution of a certification campaign creation.
 */
public final class AccessCertificationReiterateCampaignRun
        extends SearchBasedActivityRun
        <AccessCertificationCaseType, AccessCertificationReiterateCampaignWorkDefinition,
                AccessCertificationReiterateCampaignActivityHandler,
                AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationReiterateCampaignRun.class);

    private AccessCertificationCampaignType campaign;
    private int newIteration;

    private ObjectQuery query;


    AccessCertificationReiterateCampaignRun(
            @NotNull ActivityRunInstantiationContext<AccessCertificationReiterateCampaignWorkDefinition, AccessCertificationReiterateCampaignActivityHandler> context) {
        super(context, "");
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .actionsExecutedStatisticsSupported(true)
                .skipWritingOperationExecutionRecords(true);
    }

    @Override
    public boolean beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        String campaignOid = getWorkDefinition().getCertificationCampaignRef().getOid();
        campaign = getBeans().repositoryService.getObject(AccessCertificationCampaignType.class, campaignOid, null, result).asObjectable();

        LOGGER.info("Reiterating campaign {}", ObjectTypeUtil.toShortString(campaign));
        if (campaign.getState() != CLOSED) {
            result.recordFatalError("Campaign is not in CLOSED state");
            throw new IllegalStateException("Campaign is not in CLOSED state");
        }
        if (campaign.getReiterationDefinition() != null && campaign.getReiterationDefinition().getLimit() != null
                && norm(campaign.getIteration()) >= campaign.getReiterationDefinition().getLimit()) {
            result.recordFatalError("Campaign cannot be reiterated: maximum number of iterations ("
                    + campaign.getReiterationDefinition().getLimit() + ") was reached.");
            throw new IllegalStateException("Campaign cannot be reiterated: maximum number of iterations ("
                    + campaign.getReiterationDefinition().getLimit() + ") was reached.");
        }

        newIteration = norm(campaign.getIteration()) + 1;

        query = prepareObjectQuery();

        return super.beforeRun(result);
    }

    @Override
    public void afterRun(OperationResult result) throws CommonException, ActivityRunException {

        AccCertUpdateHelper updateHelper = getActivityHandler().getUpdateHelper();

        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        modifications.add(updateHelper.createStageNumberDelta(0));
        modifications.add(updateHelper.createStateDelta(CREATED));
        modifications.add(updateHelper.createTriggerDeleteDelta());
        modifications.add(updateHelper.createStartTimeDelta(null));
        modifications.add(updateHelper.createEndTimeDelta(null));
        modifications.add(PrismContext.get().deltaFor(AccessCertificationCampaignType.class)
                .item(AccessCertificationCampaignType.F_ITERATION).replace(newIteration)
                .asItemDelta());

        updateHelper.modifyObjectPreAuthorized(AccessCertificationCampaignType.class, campaign.getOid(), modifications, getRunningTask(), result);

        super.afterRun(result);
    }


    @Override
    public @Nullable SearchSpecification<AccessCertificationCaseType> createCustomSearchSpecification(OperationResult result) {
        return new SearchSpecification<>(AccessCertificationCaseType.class, query, null, false);
    }

    @Override
    protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
        return getWorkDefinition().getCertificationCampaignRef();
    }

    @Override
    public boolean processItem(@NotNull AccessCertificationCaseType item, @NotNull ItemProcessingRequest<AccessCertificationCaseType> request, RunningTask workerTask, OperationResult result) throws CommonException {
        Collection<ItemDelta<?, ?>> modifications = PrismContext.get().deltaFor(AccessCertificationCampaignType.class)
                .item(F_CASE, item.getId(), F_ITERATION).replace(newIteration)
                .item(F_CASE, item.getId(), F_STAGE_NUMBER).replace(0)
                .item(F_CASE, item.getId(), F_CURRENT_STAGE_OUTCOME).replace()
                .item(F_CASE, item.getId(), F_CURRENT_STAGE_DEADLINE).replace()
                .item(F_CASE, item.getId(), F_CURRENT_STAGE_CREATE_TIMESTAMP).replace()
                .item(F_CASE, item.getId(), F_REVIEW_FINISHED_TIMESTAMP).replace()
                .asItemDeltas();

        getActivityHandler().getUpdateHelper().modifyObjectPreAuthorized(AccessCertificationCampaignType.class, campaign.getOid(), modifications, getRunningTask(), result);
        return true;
    }

    @NotNull
    private ObjectQuery prepareObjectQuery() {
        ObjectQuery noResponseQuery = PrismContext.get().queryFor(AccessCertificationCaseType.class)
                .item(AccessCertificationCaseType.F_OUTCOME).eq(SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE)
                .build();
        return QueryUtils.addFilter(
                noResponseQuery,
                PrismContext.get().queryFactory().createOwnerHasOidIn(campaign.getOid()));

    }
}
