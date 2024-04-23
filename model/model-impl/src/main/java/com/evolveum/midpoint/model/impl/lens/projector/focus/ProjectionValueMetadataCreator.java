/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import static com.evolveum.midpoint.util.DebugUtil.debugDumpLazily;

import static java.util.Collections.emptySet;

import java.util.Collection;
import java.util.function.Supplier;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.common.util.PopulatorUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ChannelUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Creates value metadata for source projections: resource objects that are to be fed into inbound
 * mappings. It is a temporary/experimental solution: normally, such metadata should be provided by the connector
 * or provisioning module. But to optimize processing, let us create such metadata only for values that
 * are really used in inbound mappings.
 */
@Experimental
@Component
public class ProjectionValueMetadataCreator {

    private static final Trace LOGGER = TraceManager.getTrace(ProjectionValueMetadataCreator.class);

    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private SecurityContextManager securityContextManager;

    public <V extends PrismValue, D extends ItemDefinition<?>>
    void setValueMetadata(@NotNull Item<V, D> resourceObjectItem, @NotNull LensProjectionContext projectionCtx,
            MappingEvaluationEnvironment env, OperationResult result) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        apply(
                resourceObjectItem.getValues(),
                () -> createMetadata(projectionCtx, resourceObjectItem, env, result),
                resourceObjectItem::getPath);
    }

    public <D extends ItemDefinition<?>, V extends PrismValue>
    void setValueMetadata(@NotNull ItemDelta<V, D> itemDelta, @NotNull LensProjectionContext projectionCtx,
            MappingEvaluationEnvironment env, OperationResult result) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        apply(itemDelta.getValuesToAdd(), () -> createMetadata(projectionCtx, itemDelta, env, result), () -> "ADD set of" + itemDelta);
        apply(itemDelta.getValuesToReplace(), () -> createMetadata(projectionCtx, itemDelta, env, result), () -> "REPLACE set of " + itemDelta);
    }

    private <V extends PrismValue> void apply(Collection<V> values, MetadataSupplier metadataSupplier,
            Supplier<Object> descSupplier) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        ValueMetadataType metadata = null;
        if (CollectionUtils.isNotEmpty(values)) {
            int changed = 0;
            for (V value : values) {
                if (value.getValueMetadata().isEmpty()) {
                    if (metadata == null) {
                        metadata = metadataSupplier.get();
                    }
                    try {
                        // FIXME: Use different opion of setValueMetadata (PCV instead of Containerable?)
                        value.setValueMetadata(CloneUtil.clone(metadata));
                    } catch (SchemaException e) {
                        throw new SystemException("Unexpected schema exception", e);
                    }
                    changed++;
                }
            }
            LOGGER.trace("Value metadata set for {} out of {} value(s) of {}:\n{}", changed, values.size(), descSupplier.get(),
                    debugDumpLazily(metadata));
        }
    }

    @FunctionalInterface
    private interface MetadataSupplier {
        ValueMetadataType get() throws CommunicationException,
                ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
                ExpressionEvaluationException;
    }

    private ValueMetadataType createMetadata(@NotNull LensProjectionContext projectionCtx, Object desc,
            MappingEvaluationEnvironment env, OperationResult result) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {

        if (projectionCtx.getCachedValueMetadata() != null) {
            return projectionCtx.getCachedValueMetadata().clone();
        }

        String resourceOid = projectionCtx.getResourceOid();
        if (resourceOid == null) {
            LOGGER.trace("No resource OID for {}, not creating value metadata for {} in {}", projectionCtx, desc, env.contextDescription);
            return null;
        }

        ProvenanceFeedDefinitionType provenanceFeed = getProvenanceFeed(projectionCtx);

        boolean experimentalCodeEnabled = projectionCtx.getLensContext().isExperimentalCodeEnabled();
        if (provenanceFeed == null && !experimentalCodeEnabled) {
            // We require either (1) experimental code to be enabled or (2) provenance feed to be
            // explicitly set in order to generate provenance metadata for inbound values.
            return null;
        }

        ValueMetadataType valueMetadataBean = populateValueMetadata(provenanceFeed, projectionCtx, env, result);
        applyBuiltinPopulators(valueMetadataBean, provenanceFeed, resourceOid, projectionCtx);

        projectionCtx.setCachedValueMetadata(valueMetadataBean.clone());
        return valueMetadataBean;
    }

    private void applyBuiltinPopulators(ValueMetadataType valueMetadataBean, ProvenanceFeedDefinitionType provenanceFeed,
            String resourceOid, LensProjectionContext projectionCtx)
            throws SchemaException, SecurityViolationException {
        Boolean useBuiltinPopulators = provenanceFeed != null ? provenanceFeed.isUseBuiltinPopulators() : null;
        if (!BooleanUtils.isFalse(useBuiltinPopulators)) {
            boolean useAlways = BooleanUtils.isTrue(useBuiltinPopulators);
            if (valueMetadataBean.getProvenance() == null) {
                valueMetadataBean.setProvenance(new ProvenanceMetadataType());
            }
            ProvenanceAcquisitionType acquisition;
            if (valueMetadataBean.getProvenance().getAcquisition().size() > 1) {
                throw new SchemaException("More than one acquisition came from the populators. Either fix that or turn off builtin populators' use.");
            } else if (valueMetadataBean.getProvenance().getAcquisition().size() == 1) {
                acquisition = valueMetadataBean.getProvenance().getAcquisition().get(0);
            } else {
                acquisition = new ProvenanceAcquisitionType();
                valueMetadataBean.getProvenance().getAcquisition().add(acquisition);
            }
            addBuiltinAcquisitionValue(acquisition, useAlways,
                    ProvenanceAcquisitionType.F_TIMESTAMP,
                    XmlTypeConverter::createXMLGregorianCalendar);
            addBuiltinAcquisitionValue(acquisition, useAlways,
                    ProvenanceAcquisitionType.F_RESOURCE_REF,
                    () -> ObjectTypeUtil.createObjectRef(resourceOid, ObjectTypes.RESOURCE));
            addBuiltinAcquisitionValue(acquisition, useAlways,
                    ProvenanceAcquisitionType.F_ORIGIN_REF,
                    () -> provenanceFeed != null ? CloneUtil.clone(provenanceFeed.getOriginRef()) : null);
            addBuiltinAcquisitionValue(acquisition, useAlways,
                    ProvenanceAcquisitionType.F_ACTOR_REF,
                    this::getActorRef);
            addBuiltinAcquisitionValue(acquisition, useAlways,
                    ProvenanceAcquisitionType.F_CHANNEL,
                    () -> getChannel(projectionCtx));
        }
    }

    private String getChannel(LensProjectionContext projectionCtx) {
        return ChannelUtil.deflate(projectionCtx.getLensContext().getChannel());
    }

    private ObjectReferenceType getActorRef() throws SecurityViolationException {
        MidPointPrincipal principal = securityContextManager.getPrincipal();
        return principal != null ? principal.toObjectReference() : null;
    }

    @FunctionalInterface
    private interface ValueSupplier {
        Object get() throws SecurityViolationException;
    }

    private void addBuiltinAcquisitionValue(ProvenanceAcquisitionType acquisition, boolean useAlways, ItemName itemName,
            ValueSupplier valueSupplier) throws SchemaException, SecurityViolationException {
        if (useAlways || !hasAcquisitionValue(acquisition, itemName)) {
            setAcquisitionValue(acquisition, itemName, valueSupplier);
        }
    }

    private boolean hasAcquisitionValue(ProvenanceAcquisitionType acquisition, ItemName itemName) {
        Item<?, ?> item = acquisition.asPrismContainerValue().findItem(itemName);
        return item != null && !item.isEmpty();
    }

    private void setAcquisitionValue(ProvenanceAcquisitionType acquisition, ItemName itemName, ValueSupplier valueSupplier)
            throws SchemaException, SecurityViolationException {
        Object realValue = valueSupplier.get();
        ItemDelta<?, ?> delta = prismContext.deltaFor(ProvenanceAcquisitionType.class)
                .item(itemName).replace(realValue)
                .asItemDelta();
        delta.applyTo(acquisition.asPrismContainerValue());
    }

    @NotNull
    private ValueMetadataType populateValueMetadata(ProvenanceFeedDefinitionType provenanceFeed,
            LensProjectionContext projectionCtx, MappingEvaluationEnvironment env,
            OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        ValueMetadataType valueMetadataBean = new ValueMetadataType();
        if (provenanceFeed == null || provenanceFeed.getAcquisitionItemPopulator().isEmpty() && provenanceFeed.getMetadataItemPopulator().isEmpty()) {
            return valueMetadataBean;
        }

        VariablesMap variables = new VariablesMap();
        PrismObject<ShadowType> projection = projectionCtx.getObjectAny();
        variables.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, projection, projectionCtx.getObjectDefinition());
        // TODO other variables

        String localContextDescription = "metadata creation";
        String fullContextDescription = localContextDescription + " in " + env.contextDescription;
        var context = new ExpressionEvaluationContext(emptySet(), variables, fullContextDescription, env.task);
        context.setExpressionFactory(expressionFactory);
        context.setLocalContextDescription(localContextDescription);

        if (!provenanceFeed.getAcquisitionItemPopulator().isEmpty()) {
            ProvenanceAcquisitionType acquisition = new ProvenanceAcquisitionType();
            PrismContainerDefinition<ProvenanceAcquisitionType> acquisitionContainerDef =
                    prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(ProvenanceAcquisitionType.class);
            for (PopulateItemType acquisitionItemPopulator : provenanceFeed.getAcquisitionItemPopulator()) {
                ItemDelta<?, ?> acquisitionDelta = PopulatorUtil.evaluatePopulateExpression(
                        acquisitionItemPopulator, variables, context, acquisitionContainerDef, result);
                if (acquisitionDelta != null) {
                    acquisitionDelta.applyTo(acquisition.asPrismContainerValue());
                }
            }
            valueMetadataBean.beginProvenance()
                    .acquisition(acquisition);
        }

        if (!provenanceFeed.getMetadataItemPopulator().isEmpty()) {
            PrismContainerDefinition<ValueMetadataType> metadataContainerDef =
                    prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(ValueMetadataType.class);
            for (PopulateItemType metadataItemPopulator : provenanceFeed.getMetadataItemPopulator()) {
                ItemDelta<?, ?> metadataDelta = PopulatorUtil.evaluatePopulateExpression(
                        metadataItemPopulator, variables, context, metadataContainerDef, result);
                if (metadataDelta != null) {
                    metadataDelta.applyTo(valueMetadataBean.asPrismContainerValue());
                }
            }
        }

        LOGGER.trace("Populators created value metadata:\n{}", DebugUtil.debugDumpLazily(valueMetadataBean));
        return valueMetadataBean;
    }

    private ProvenanceFeedDefinitionType getProvenanceFeed(LensProjectionContext projectionCtx)
            throws SchemaException, ConfigurationException {
        ResourceObjectDefinition def = projectionCtx.getStructuralDefinitionIfNotBroken();
        return def != null ? def.getDefinitionBean().getProvenance() : null;
    }
}
