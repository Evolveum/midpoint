/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.cleanup;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.run.*;
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
public final class CleanupPartialActivityRun<CP>
        extends LocalActivityRun<CleanupWorkDefinition, CleanupActivityHandler, AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(CleanupPartialActivityRun.class);

    @NotNull private final CleanupActivityHandler.Part part;
    @NotNull private final Function<CleanupPoliciesType, CP> policyGetter;
    @NotNull private final CleanupActivityHandler.Cleaner<CP> cleaner;

    CleanupPartialActivityRun(
            @NotNull ActivityRunInstantiationContext<CleanupWorkDefinition, CleanupActivityHandler> context,
            @NotNull CleanupActivityHandler.Part part,
            @NotNull Function<CleanupPoliciesType, CP> policyGetter,
            @NotNull CleanupActivityHandler.Cleaner<CP> cleaner) {
        super(context);
        this.part = part;
        this.policyGetter = policyGetter;
        this.cleaner = cleaner;
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .progressSupported(part.supportsProgress);
    }

    @Override
    public boolean shouldCreateWorkStateOnInitialization() {
        return false;
    }

    @Override
    protected @NotNull ActivityRunResult runLocally(OperationResult result) throws ActivityRunException {
        ensureFullExecution();
        CP policy = policyGetter.apply(
                getCleanupPolicies(result));
        if (policy != null) {
            try {
                getRunningTask().setExecutionSupport(this);
                cleaner.cleanup(policy, getRunningTask(), result);
                LOGGER.info("{}: Finished", part.label);
            } catch (Exception e) {
                result.recordFatalError(e);
                LOGGER.error("{}: {}", part.label, e.getMessage(), e);
                // Error is fatal w.r.t. this activity, and partial for the whole cleanup.
                // But because the status aggregation does not treat this correctly yet (fatal is propagated to the root),
                // let us report this as partial error for now. FIXME
                return ActivityRunResult.exception(PARTIAL_ERROR, FINISHED, e);
            } finally {
                getRunningTask().setExecutionSupport(null);
            }
        } else {
            LOGGER.trace(part.label + ": No clean up policy for this kind of items present.");
        }
        return standardRunResult();
    }

    private @NotNull CleanupPoliciesType getCleanupPolicies(OperationResult opResult) throws ActivityRunException {
        CleanupPoliciesType explicitCleanupPolicies = getActivity().getWorkDefinition().getCleanupPolicies();
        if (explicitCleanupPolicies != null) {
            LOGGER.info("Using explicit cleanup policies cleanupPolicies: {}", explicitCleanupPolicies);
            return explicitCleanupPolicies;
        }

        CleanupPoliciesType cleanupPolicies = getSystemCleanupPolicies(opResult);
        if (cleanupPolicies == null) {
            LOGGER.info("No clean up policies specified. Finishing clean up task.");
            throw new ActivityRunException("", SUCCESS, FINISHED);
        }
        return cleanupPolicies;
    }

    private CleanupPoliciesType getSystemCleanupPolicies(OperationResult opResult) throws ActivityRunException {
        try {
            PrismObject<SystemConfigurationType> systemConfiguration =
                    getModelBeans().systemObjectCache.getSystemConfiguration(opResult);
            return systemConfiguration != null ? systemConfiguration.asObjectable().getCleanupPolicy() : null;
        } catch (SchemaException e) {
            throw new ActivityRunException("Couldn't get system configuration", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    private @NotNull ModelBeans getModelBeans() {
        return getActivityHandler().getModelBeans();
    }
}
