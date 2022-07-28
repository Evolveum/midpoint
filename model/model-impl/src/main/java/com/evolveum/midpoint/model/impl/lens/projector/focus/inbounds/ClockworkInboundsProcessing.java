/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import static com.evolveum.midpoint.prism.PrismContainerValue.getId;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.*;
import java.util.function.Function;

import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.identities.IdentityManagementConfiguration;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.delta.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentitiesType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.ClockworkContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.ClockworkShadowInboundsPreparation;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Evaluation of inbound mappings from all projections in given lens context.
 *
 * Responsibility of this class:
 *
 * 1. collects inbound mappings to be evaluated
 * 2. evaluates them
 * 3. consolidates the results into deltas
 */
public class ClockworkInboundsProcessing<F extends FocusType> extends AbstractInboundsProcessing<F> {

    private static final Trace LOGGER = TraceManager.getTrace(ClockworkInboundsProcessing.class);

    private static final String OP_NORMALIZE_CHANGED_FOCUS_IDENTITY_DATA =
            ClockworkInboundsProcessing.class.getName() + ".normalizeChangedFocusIdentityData";

    @NotNull private final LensContext<F> context;

    public ClockworkInboundsProcessing(
            @NotNull LensContext<F> context,
            @NotNull ModelBeans beans,
            @NotNull MappingEvaluationEnvironment env,
            @NotNull OperationResult result) {
        super(beans, env, result);
        this.context = context;
    }

    /**
     * Collects all the mappings from all the projects, sorted by target property.
     *
     * Original motivation (is it still valid?): we need to evaluate them together, e.g. in case that there are several mappings
     * from several projections targeting the same property.
     */
    void collectMappings()
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {

        for (LensProjectionContext projectionContext : context.getProjectionContexts()) {

            // Preliminary checks. (Before computing apriori delta and other things.)

            if (projectionContext.isGone()) {
                LOGGER.trace("Skipping processing of inbound expressions for projection {} because is is gone",
                        lazy(projectionContext::getHumanReadableName));
                continue;
            }
            if (!projectionContext.isCanProject()) {
                LOGGER.trace("Skipping processing of inbound expressions for projection {}: "
                                + "there is a limit to propagate changes only from resource {}",
                        lazy(projectionContext::getHumanReadableName), context.getTriggeringResourceOid());
                continue;
            }

            try {
                PrismObject<F> objectCurrentOrNew = context.getFocusContext().getObjectCurrentOrNew();
                new ClockworkShadowInboundsPreparation<>(
                        projectionContext,
                        context,
                        mappingsMap,
                        itemDefinitionMap,
                        new ClockworkContext(context, env, result, beans),
                        objectCurrentOrNew,
                        getFocusDefinition(objectCurrentOrNew))
                        .collectOrEvaluate();
            } catch (StopProcessingProjectionException e) {
                LOGGER.debug("Inbound processing on {} interrupted because the projection is broken", projectionContext);
            }
        }
    }

    @Override
    @Nullable PrismObject<F> getFocusNew() {
        return context.getFocusContext().getObjectNew();
    }

    @Override
    protected @Nullable ObjectDelta<F> getFocusAPrioriDelta() {
        return context.getFocusContextRequired().getCurrentDelta();
    }

    @Override
    @NotNull
    Function<ItemPath, Boolean> getFocusPrimaryItemDeltaExistsProvider() {
        return context::primaryFocusItemDeltaExists;
    }

    @Override
    @NotNull PrismObjectDefinition<F> getFocusDefinition(@Nullable PrismObject<F> focus) {
        if (focus != null && focus.getDefinition() != null) {
            return focus.getDefinition();
        } else {
            return context.getFocusContextRequired().getObjectDefinition();
        }
    }

    @Override
    void applyComputedDeltas(Collection<ItemDelta<?, ?>> itemDeltas) throws SchemaException {
        context.getFocusContextRequired().swallowToSecondaryDelta(itemDeltas);
    }

    @Override
    @Nullable LensContext<?> getLensContextIfPresent() {
        return context;
    }

    @Override
    void normalizeChangedFocusIdentityData() throws ConfigurationException, SchemaException, ExpressionEvaluationException {
        OperationResult identityUpdateResult = result.subresult(OP_NORMALIZE_CHANGED_FOCUS_IDENTITY_DATA)
                .setMinor()
                .build();
        try {
            IdentityManagementConfiguration configuration =
                    beans.identitiesManager.getIdentityManagementConfiguration(context);
            if (configuration == null) {
                LOGGER.trace("No identity management configuration for {}; identity data will not be normalized", context);
                identityUpdateResult.recordNotApplicable("No identity management configuration");
                return;
            }

            LOGGER.trace("Normalizing focus identity data from inbound mapping output(s)");
            LensFocusContext<F> focusContext = context.getFocusContextRequired();
            ObjectDelta<F> secondaryDelta = focusContext.getSecondaryDelta();
            if (ObjectDelta.isEmpty(secondaryDelta)) {
                LOGGER.trace("No secondary delta -> nothing to normalize");
                return;
            }

            PrismObject<F> objectNew = focusContext.getObjectNew();
            if (objectNew == null) {
                LOGGER.trace("No 'object new' -> nothing to normalize (should not occur!)");
                return;
            }

            ItemPath identityPrefix = ItemPath.create(FocusType.F_IDENTITIES, FocusIdentitiesType.F_IDENTITY);
            stateCheck(secondaryDelta.isModify(), "Not a modify delta?");
            Set<Long> changedIds = new HashSet<>();
            for (ItemDelta<?, ?> modification : secondaryDelta.getModifications()) {
                ItemPath modifiedItemPath = modification.getPath();
                if (modifiedItemPath.startsWith(identityPrefix)) {
                    ItemPath rest = modifiedItemPath.rest(2);
                    if (rest.startsWithId()) {
                        changedIds.add(rest.firstToId());
                    } else if (rest.isEmpty()) {
                        for (PrismValue value : emptyIfNull(modification.getValuesToAdd())) {
                            changedIds.add(getId(value));
                        }
                        for (PrismValue value : emptyIfNull(modification.getValuesToReplace())) {
                            changedIds.add(getId(value));
                        }
                    }
                }
            }
            focusContext.swallowToSecondaryDelta(
                    beans.identitiesManager.computeNormalizationDeltas(objectNew.asObjectable(), changedIds, configuration));
        } catch (Throwable t) {
            identityUpdateResult.recordFatalError(t);
            throw t;
        } finally {
            identityUpdateResult.close();
        }
    }
}
