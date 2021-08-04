/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.cleanup;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ActivityExecutionResult;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.execution.LocalActivityExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.*;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.*;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

/**
 * Activity execution for an elementary cleanup part.
 *
 * TODO this class is not finished, e.g. statistics reporting is not complete
 *
 * @param <CP> Cleanup policy type
 */
public class CleanupPartialActivityExecution<CP>
        extends LocalActivityExecution<CleanupWorkDefinition, CleanupActivityHandler, AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(CleanupPartialActivityExecution.class);

    @NotNull private final CleanupActivityHandler.Part part;
    @NotNull private final Function<CleanupPoliciesType, CP> policyGetter;
    @NotNull private final CleanupActivityHandler.Cleaner<CP> cleaner;

    CleanupPartialActivityExecution(
            @NotNull ExecutionInstantiationContext<CleanupWorkDefinition, CleanupActivityHandler> context,
            @NotNull CleanupActivityHandler.Part part,
            @NotNull Function<CleanupPoliciesType, CP> policyGetter,
            @NotNull CleanupActivityHandler.Cleaner<CP> cleaner) {
        super(context);
        this.part = part;
        this.policyGetter = policyGetter;
        this.cleaner = cleaner;
    }

    @Override
    public boolean doesSupportStatistics() {
        return true;
    }

    @Override
    public boolean doesSupportSynchronizationStatistics() {
        return false;
    }

    @Override
    public boolean doesSupportActionsExecuted() {
        return false; // Low-level direct repo operations are not reported in this way
    }

    @Override
    public boolean doesSupportProgress() {
        return part.supportsProgress;
    }

    @Override
    public boolean shouldCreateWorkStateOnInitialization() {
        return false;
    }

    @Override
    protected @NotNull ActivityExecutionResult executeLocal(OperationResult result) throws ActivityExecutionException {
        CP policy = policyGetter.apply(
                getCleanupPolicies(result));
        if (policy != null) {
            try {
                cleaner.cleanup(policy, getRunningTask(), result);
                LOGGER.info("{}: Finished", part.label);
            } catch (Exception e) {
                result.recordFatalError(e);
                LOGGER.error("{}: {}", part.label, e.getMessage(), e);
                return ActivityExecutionResult.exception(PARTIAL_ERROR, FINISHED, e);
            }
        } else {
            LOGGER.trace(part.label + ": No clean up policy for this kind of items present.");
        }
        return standardExecutionResult();
    }

    private @NotNull CleanupPoliciesType getCleanupPolicies(OperationResult opResult) throws ActivityExecutionException {
        CleanupPoliciesType explicitCleanupPolicies = getActivity().getWorkDefinition().getCleanupPolicies();
        if (explicitCleanupPolicies != null) {
            LOGGER.info("Using explicit cleanup policies cleanupPolicies: {}", explicitCleanupPolicies);
            return explicitCleanupPolicies;
        }

        CleanupPoliciesType cleanupPolicies = getSystemCleanupPolicies(opResult);
        if (cleanupPolicies == null) {
            LOGGER.info("No clean up policies specified. Finishing clean up task.");
            throw new ActivityExecutionException("", SUCCESS, FINISHED);
        }
        return cleanupPolicies;
    }

    private CleanupPoliciesType getSystemCleanupPolicies(OperationResult opResult) throws ActivityExecutionException {
        try {
            PrismObject<SystemConfigurationType> systemConfiguration =
                    getModelBeans().systemObjectCache.getSystemConfiguration(opResult);
            return systemConfiguration != null ? systemConfiguration.asObjectable().getCleanupPolicy() : null;
        } catch (SchemaException e) {
            throw new ActivityExecutionException("Couldn't get system configuration", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    private @NotNull ModelBeans getModelBeans() {
        return getActivityHandler().getModelBeans();
    }
}
