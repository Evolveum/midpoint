/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import static com.evolveum.midpoint.util.QNameUtil.qNameToUri;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.task.api.Task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.api.correlator.ManualCorrelationContext;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Manages built-in correlation cases.
 *
 * TODO better name
 */
@Component
public class BuiltInCaseManager {

    private static final Trace LOGGER = TraceManager.getTrace(BuiltInCaseManager.class);

    @Autowired private ModelBeans beans;

    public <F extends FocusType> CorrelationResult createCorrelationResultOrCase(
            @NotNull ShadowType resourceObject,
            @NotNull FocusType preFocus,
            @NotNull List<F> candidates,
            @NotNull CorrelationContext correlationContext,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException {

        if (shouldCreateCorrelationCase(candidates, correlationContext)) {
            LOGGER.trace("Going to create or update correlation case for {}", resourceObject);
            beans.correlationCaseManager.createOrUpdateCase(
                    resourceObject,
                    preFocus,
                    createCaseContextBean(candidates, correlationContext),
                    task,
                    result);
            return CorrelationResult.uncertain();
        } else {
            LOGGER.trace("No correlation case should be there - closing any cases that are open for {}", resourceObject);
            beans.correlationCaseManager.closeCaseIfExists(resourceObject, result);
            return CorrelatorUtil.createCorrelationResult(candidates);
        }
    }

    private <F extends FocusType> PotentialOwnersType createCaseContextBean(
            List<F> candidates, CorrelationContext correlationContext) {
        PotentialOwnersType context = new PotentialOwnersType(PrismContext.get());
        if (correlationContext.getManualCorrelationContext().getPotentialMatches() != null) {
            context.getPotentialOwner().addAll(
                    CloneUtil.cloneCollectionMembers(
                            correlationContext.getManualCorrelationContext().getPotentialMatches()));
        } else {
            createDefaultPotentialMatches(context, candidates);
        }
        return context;
    }

    private <F extends FocusType> void createDefaultPotentialMatches(PotentialOwnersType context, List<F> candidates) {
        for (F candidate : candidates) {
            context.getPotentialOwner().add(
                    createPotentialMatch(candidate));
        }
        context.getPotentialOwner().add(
                createPotentialMatch(null));
    }

    private PotentialOwnerType createPotentialMatch(@Nullable FocusType candidate) {
        String optionUri = candidate != null ?
                qNameToUri(
                        new QName(SchemaConstants.CORRELATION_NS,
                                SchemaConstants.CORRELATION_OPTION_PREFIX + candidate.getOid())) :
                SchemaConstants.CORRELATION_NONE_URI;
        return new PotentialOwnerType(PrismContext.get())
                .uri(optionUri)
                .candidateOwnerRef(ObjectTypeUtil.createObjectRef(candidate));
    }

    private <F extends FocusType> boolean shouldCreateCorrelationCase(List<F> candidates, CorrelationContext correlationContext) {
        ManualCorrelationContext context = correlationContext.getManualCorrelationContext();
        ManualCorrelationEnabledType enabled = getManualCorrelationEnabled(context);
        LOGGER.trace("Manual correlation enabled: {}", enabled);

        switch (enabled) {
            case ALWAYS:
                return !candidates.isEmpty();
            case WHEN_MORE_CANDIDATES:
                return candidates.size() > 1 || context.isRequested();
            case WHEN_REQUESTED:
                return context.isRequested();
            case NEVER:
                return false;
            default:
                throw new AssertionError(enabled);
        }
    }

    private @NotNull ManualCorrelationEnabledType getManualCorrelationEnabled(ManualCorrelationContext manualCorrelationContext) {
        ManualCorrelationConfigurationType configuration = manualCorrelationContext.getConfiguration();
        return configuration != null && configuration.getEnabled() != null ?
                configuration.getEnabled() : ManualCorrelationEnabledType.WHEN_REQUESTED;
    }
}
