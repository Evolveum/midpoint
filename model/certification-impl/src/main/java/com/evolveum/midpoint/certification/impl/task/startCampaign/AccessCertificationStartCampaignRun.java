/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task.startCampaign;

import static com.evolveum.midpoint.certification.api.OutcomeUtils.toUri;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.certification.impl.task.AccessCertificationStageManagementRun;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Execution of a certification campaign creation.
 */
public final class AccessCertificationStartCampaignRun
        extends AccessCertificationStageManagementRun
        <AssignmentHolderType, AccessCertificationStartCampaignWorkDefinition,
                        AccessCertificationStartCampaignActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationStartCampaignRun.class);

    AccessCertificationStartCampaignRun(
            @NotNull ActivityRunInstantiationContext<AccessCertificationStartCampaignWorkDefinition, AccessCertificationStartCampaignActivityHandler> context) {
        super(context, "");
        setInstanceReady();
    }

    @Override
    public boolean processItem(@NotNull AssignmentHolderType item, @NotNull ItemProcessingRequest<AssignmentHolderType> request, RunningTask workerTask, OperationResult result) throws CommonException {
        Task task = getRunningTask();
        List<AccessCertificationCaseType> caseList = new ArrayList<>(getCertificationHandler().createCasesForObject(request.getItem().asPrismObject(), getCampaign(), task, result));

        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        for (AccessCertificationCaseType aCase : caseList) {
            ContainerDelta<AccessCertificationCaseType> caseDelta = PrismContext.get().deltaFactory().container().createDelta(F_CASE,
                    AccessCertificationCampaignType.class);
            aCase.setIteration(1);
            aCase.setStageNumber(1);
            aCase.setCurrentStageCreateTimestamp(getStage().getStartTimestamp());
            aCase.setCurrentStageDeadline(getStage().getDeadline());


            List<ObjectReferenceType> reviewers = getReviewersHelper().getReviewersForCase(aCase, getCampaign(), getReviewerSpec(), task, result);
            aCase.getWorkItem().addAll(createWorkItems(reviewers, 1, 1, aCase));

            AccessCertificationResponseType currentStageOutcome = getComputationHelper().computeOutcomeForStage(aCase, getCampaign(), 1);
            aCase.setCurrentStageOutcome(toUri(currentStageOutcome));
            aCase.setOutcome(toUri(getComputationHelper().computeOverallOutcome(aCase, getCampaign(), 1, currentStageOutcome)));

            @SuppressWarnings({ "raw", "unchecked" })
            PrismContainerValue<AccessCertificationCaseType> caseCVal = aCase.asPrismContainerValue();
            caseDelta.addValueToAdd(caseCVal);
            LOGGER.trace("Adding certification case:\n{}", caseCVal.debugDumpLazily());
            modifications.add(caseDelta);
        }
        getUpdateHelper().modifyObjectPreAuthorized(AccessCertificationCampaignType.class, getCampaign().getOid(), modifications, task, result);

        return true;
    }

    @NotNull
    protected ObjectQuery prepareObjectQuery() throws SchemaException {

        AccessCertificationObjectBasedScopeType objectBasedScope = getObjectBasedScope();
        // TODO derive search filter from certification handler (e.g. select only objects having assignments with the proper policySituation)
        // It is only an optimization but potentially a very strong one. Workaround: enter query filter manually into scope definition.
        final SearchFilterType searchFilter = objectBasedScope != null ? objectBasedScope.getSearchFilter() : null;
        ObjectQuery query = PrismContext.get().queryFactory().createQuery();
        if (searchFilter != null) {
            query.setFilter(PrismContext.get().getQueryConverter().parseFilter(searchFilter, determineObjectType()));
        }
        return query;
    }

    private Class<AssignmentHolderType> determineObjectType() {
        String campaignShortName = toShortString(getCampaign());
        QName objectType = getObjectType(getObjectBasedScope(), campaignShortName);

        //noinspection unchecked
        return (Class<AssignmentHolderType>) PrismContext.get().getSchemaRegistry().getCompileTimeClassForObjectTypeRequired(objectType);
    }

    private AccessCertificationObjectBasedScopeType getObjectBasedScope() {
        String campaignShortName = toShortString(getCampaign());
        AccessCertificationScopeType scope = getCampaign().getScopeDefinition();
        LOGGER.trace("Creating cases for scope {} in campaign {}", scope, campaignShortName);
        if (scope != null && !(scope instanceof AccessCertificationObjectBasedScopeType)) {
            throw new IllegalStateException("Unsupported access certification scope type: " + scope.getClass() + " for campaign " + campaignShortName);
        }
        return (AccessCertificationObjectBasedScopeType) scope;

    }

    @NotNull
    private QName getObjectType(AccessCertificationObjectBasedScopeType objectBasedScope, String campaignShortName) {
        QName scopeDeclaredObjectType;
        if (objectBasedScope != null) {
            scopeDeclaredObjectType = objectBasedScope.getObjectType();
        } else {
            scopeDeclaredObjectType = null;
        }
        QName objectType;
        if (scopeDeclaredObjectType != null) {
            objectType = scopeDeclaredObjectType;
        } else {
            objectType = getCertificationHandler().getDefaultObjectType();
        }
        if (objectType == null) {
            throw new IllegalStateException("Unspecified object type (and no default one provided) for campaign " + campaignShortName);
        }
        return objectType;
    }

    @Override
    protected Class<AssignmentHolderType> getType() {
        return determineObjectType();
    }
}
