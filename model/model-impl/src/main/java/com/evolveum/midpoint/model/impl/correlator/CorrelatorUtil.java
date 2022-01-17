/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.api.correlator.CorrelatorConfiguration;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class CorrelatorUtil {

    @NotNull
    public static CorrelationResult createCorrelationResult(List<? extends ObjectType> candidates) {
        if (candidates.isEmpty()) {
            return CorrelationResult.noOwner();
        } else if (candidates.size() == 1) {
            return CorrelationResult.existingOwner(candidates.get(0));
        } else {
            return CorrelationResult.uncertain();
        }
    }

    public static <F extends ObjectType> void addCandidates(List<F> allCandidates, List<F> candidates, Trace logger) {
        for (F candidate : candidates) {
            if (!containsOid(allCandidates, candidate.getOid())) {
                logger.trace("Found candidate owner {}", candidate);
                allCandidates.add(candidate);
            } else {
                logger.trace("Candidate owner {} already processed", candidate);
            }
        }
    }

    public static <F extends ObjectType> boolean containsOid(List<F> allCandidates, String oid) {
        for (F existing : allCandidates) {
            if (existing.getOid().equals(oid)) {
                return true;
            }
        }
        return false;
    }

    public static VariablesMap getVariablesMap(
            ObjectType focus,
            ShadowType resourceObject,
            CorrelationContext correlationContext) {
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(
                focus,
                resourceObject,
                correlationContext.getResource(),
                correlationContext.getSystemConfiguration());
        variables.put(ExpressionConstants.VAR_CORRELATION_CONTEXT, correlationContext, CorrelationContext.class);
        variables.put(ExpressionConstants.VAR_CORRELATION_STATE, correlationContext.getCorrelationState(),
                AbstractCorrelationStateType.class);
        return variables;
    }

    /**
     * Extracts {@link CorrelatorConfiguration} objects from given "correlators" structure (both typed and untyped).
     */
    public static @NotNull Collection<CorrelatorConfiguration> getConfigurations(@NotNull CorrelatorsType correlation) {
        List<CorrelatorConfiguration> configurations =
                Stream.of(
                                correlation.getNone().stream(),
                                correlation.getFilter().stream(),
                                correlation.getExpression().stream(),
                                correlation.getIdMatch().stream())
                        .flatMap(s -> s)
                        .map(CorrelatorConfiguration.TypedCorrelationConfiguration::new)
                        .collect(Collectors.toCollection(ArrayList::new));

        if (correlation.getExtension() != null) {
            //noinspection unchecked
            Collection<Item<?, ?>> items = correlation.getExtension().asPrismContainerValue().getItems();
            for (Item<?, ?> item : items) {
                for (PrismValue value : item.getValues()) {
                    // TODO better type safety checks (provide specific exceptions)
                    if (value instanceof PrismContainerValue) {
                        //noinspection unchecked
                        configurations.add(
                                new CorrelatorConfiguration.UntypedCorrelationConfiguration(
                                        item.getElementName(),
                                        ((PrismContainerValue<AbstractCorrelatorType>) value)));
                    }
                }
            }
        }
        return configurations;
    }

    public static @NotNull ShadowType getShadowFromCorrelationCase(@NotNull PrismObject<CaseType> aCase) throws SchemaException {
        return MiscUtil.requireNonNull(
                MiscUtil.castSafely(
                        ObjectTypeUtil.getObjectFromReference(aCase.asObjectable().getObjectRef()),
                        ShadowType.class),
                () -> new IllegalStateException("No shadow object in " + aCase));
    }

    /**
     * Finds the appropriate object type definition, and then the correlators definition.
     *
     * Temporary and very crude implementation.
     */
    @SuppressWarnings("WeakerAccess")
    public static @NotNull CorrelatorsType getCorrelatorsBeanForShadow(
            @NotNull ShadowType shadow,
            @NotNull ModelBeans beans,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        String resourceOid = ShadowUtil.getResourceOidRequired(shadow);
        ResourceType resource =
                beans.provisioningService.getObject(ResourceType.class, resourceOid, null, task, result).asObjectable();

        // We expect that the shadow is classified + reasonably fresh (= not legacy), so it has kind+intent present.
        ShadowKindType kind = MiscUtil.requireNonNull(shadow.getKind(), () -> new IllegalStateException("No kind in " + shadow));
        String intent = MiscUtil.requireNonNull(shadow.getIntent(), () -> new IllegalStateException("No intent in " + shadow));
        // TODO check for "unknown" ?

        // We'll look for type definition in the future (after synchronization is integrated into it).
//        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resource);
//        ResourceObjectTypeDefinition typeDefinition = schema.findObjectTypeDefinitionRequired(kind, intent);

        for (ObjectSynchronizationType config : resource.getSynchronization().getObjectSynchronization()) {
            if (config.getKind() == kind && intent.equals(config.getIntent())) {
                return MiscUtil.requireNonNull(
                        config.getCorrelators(),
                        () -> new IllegalStateException("No correlators in " + config));
            }
        }
        throw new IllegalStateException(
                "No " + kind + "/" + intent + " (kind/intent) definition in " + resource + " (for " + shadow + ")");
    }
}
