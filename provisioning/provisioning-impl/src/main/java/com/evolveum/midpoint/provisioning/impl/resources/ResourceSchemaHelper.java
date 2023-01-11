/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import static com.evolveum.midpoint.prism.PrismObject.asPrismObject;
import static com.evolveum.midpoint.prism.Referencable.getOid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceSchema;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.MutablePrismSchema;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Helps {@link ResourceManager} with schema-related aspects, like application of the definitions to resource object.
 * (It's a Spring bean for now.)
 *
 * To be used only from the local package only. All external access should be through {@link ResourceManager}.
 */
@Component
class ResourceSchemaHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceManager.class);

    private static final String OP_APPLY_DEFINITION_TO_DELTA = ResourceSchemaHelper.class + ".applyDefinitionToDelta";

    @Autowired private ResourceManager resourceManager;
    @Autowired private ConnectorManager connectorManager;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;

    /**
     * Applies a definition on a resource we know nothing about - i.e. it may be unexpanded.
     * So, expanding if (presumably) needed.
     */
    void applyConnectorSchemasToResource(
            @NotNull ResourceType resource,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, ConfigurationException {
        ResourceType expanded;
        if (ResourceTypeUtil.doesNeedExpansion(resource)) {
            expanded = resource.clone();
            resourceManager.expandResource(expanded, result);
        } else {
            expanded = resource;
        }
        applyConnectorSchemasToResource(resource, expanded, result);
    }

    /** Use this if the resource is already expanded. */
    void applyConnectorSchemasToExpandedResource(
            @NotNull ResourceType resource,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, ConfigurationException {
        applyConnectorSchemasToResource(resource, resource, result);
    }

    /**
     * Applies proper definition (connector schema) to the resource.
     *
     * @param target Resource on which we need to apply the definition
     * @param source A variant of `resource` used to derive the definition (e.g. expanded version - to be able to
     * obtain connector OIDs); may be the resource itself.
     */
    private void applyConnectorSchemasToResource(
            @NotNull ResourceType target,
            @NotNull ResourceType source,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, ConfigurationException {
        checkMutable(target.asPrismObject());
        PrismObjectDefinition<ResourceType> newResourceDefinition = target.asPrismObject().getDefinition().clone();
        for (ConnectorSpec sourceConnectorSpec : ConnectorSpec.all(source)) {
            try {
                ConnectorSpec targetConnectorSpec = ConnectorSpec.find(target, sourceConnectorSpec.getConnectorName());
                applyConnectorSchemaToResource(targetConnectorSpec, sourceConnectorSpec, newResourceDefinition, result);
            } catch (CommunicationException | SecurityViolationException e) {
                throw new IllegalStateException("Unexpected exception: " + e.getMessage(), e); // fixme temporary solution
            }
        }
        newResourceDefinition.freeze();
        target.asPrismObject().setDefinition(newResourceDefinition);
    }

    /**
     * Applies proper definition (connector schema) to the resource - to the definition and particular connector spec.
     *
     * @param targetConnectorSpec Connector spec that should be updated (may be null if not present)
     * @param sourceConnectorSpec Connector spec that is used as the definition source - usually the same as target,
     * or an expanded version of it.
     * @param targetDefinition Resource-level definition that should be updated
     */
    void applyConnectorSchemaToResource(
            @Nullable ConnectorSpec targetConnectorSpec,
            @NotNull ConnectorSpec sourceConnectorSpec,
            @NotNull PrismObjectDefinition<ResourceType> targetDefinition,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        var connectorWithSchema = connectorManager.getConnectorWithSchema(sourceConnectorSpec, result);
        PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDefinition =
                connectorWithSchema.getConfigurationContainerDefinition().clone();

        PrismContainer<ConnectorConfigurationType> sourceConfigurationContainer = sourceConnectorSpec.getConnectorConfiguration();
        // We want element name, minOccurs/maxOccurs and similar definition to be taken from the original, not the schema
        // the element is global in the connector schema. therefore it does not have correct maxOccurs
        if (sourceConfigurationContainer != null) {
            configurationContainerDefinition.adoptElementDefinitionFrom(sourceConfigurationContainer.getDefinition());
        } else if (sourceConnectorSpec.isMain()) {
            configurationContainerDefinition.adoptElementDefinitionFrom(
                    targetDefinition.findContainerDefinition(ResourceType.F_CONNECTOR_CONFIGURATION));
        }

        PrismContainer<ConnectorConfigurationType> targetConfigurationContainer =
                targetConnectorSpec != null ? targetConnectorSpec.getConnectorConfiguration() : null;
        if (targetConfigurationContainer != null) {
            targetConfigurationContainer.applyDefinition(configurationContainerDefinition, true);
        }

        if (sourceConnectorSpec.isMain()) {
            // Default connector, for compatibility
            // It does not make sense to update this for any other connectors.
            // We cannot have one definition for additionalConnector[1]/connectorConfiguration and
            // different definition for additionalConnector[2]/connectorConfiguration in the object definition.
            // The way to go is to set up definitions on the container level.
            targetDefinition.replaceDefinition(ResourceType.F_CONNECTOR_CONFIGURATION, configurationContainerDefinition);
        }
    }

    /**
     * Evaluates expressions in configuration properties (of all connectors). Assumes the definitions have been already applied.
     */
    void evaluateExpressionsInConfigurationProperties(
            ResourceType resource,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, ConfigurationException {
        for (ConnectorSpec connectorSpec : ConnectorSpec.all(resource)) {
            evaluateExpressionsInConfigurationProperties(connectorSpec, resource, task, result);
        }
    }

    /**
     * Evaluates expressions in configuration properties. Assumes the definitions have been already applied.
     */
    void evaluateExpressionsInConfigurationProperties(
            ConnectorSpec connectorSpec,
            ResourceType resource,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, ConfigurationException {
        PrismContainer<ConnectorConfigurationType> configurationContainer = connectorSpec.getConnectorConfiguration();
        if (configurationContainer != null) {
            evaluateExpressions(configurationContainer, resource, task, result);
        }
    }

    private void evaluateExpressions(
            PrismContainer<ConnectorConfigurationType> configurationContainer,
            ResourceType resource,
            Task task,
            OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            ConfigurationException {
        try {
            //noinspection unchecked
            configurationContainer.accept(visitable -> {
                if ((visitable instanceof PrismProperty<?>)) {
                    try {
                        evaluateExpression((PrismProperty<?>)visitable, resource.asPrismObject(), task, result);
                    } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException |
                            ConfigurationException | SecurityViolationException e) {
                        throw new TunnelException(e);
                    }
                }
                // TODO treat configuration items that are containers themselves
            });
        } catch (TunnelException te) {
            Throwable e = te.getCause();
            if (e instanceof SchemaException) {
                throw (SchemaException)e;
            } else if (e instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException)e;
            } else if (e instanceof ExpressionEvaluationException) {
                throw (ExpressionEvaluationException)e;
            } else if (e instanceof ConfigurationException) {
                throw (ConfigurationException)e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            } else if (e instanceof Error) {
                throw (Error)e;
            } else {
                // CommunicationException and SecurityViolationException are handled here (temporary solution)
                throw new SystemException(e);
            }
        }
    }

    private <T> void evaluateExpression(
            PrismProperty<T> configurationProperty,
            PrismObject<ResourceType> resource,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        PrismPropertyDefinition<T> propDef = configurationProperty.getDefinition();
        String shortDesc = "connector configuration property "+configurationProperty+" in "+resource;
        List<PrismPropertyValue<T>> extraValues = new ArrayList<>();
        for (PrismPropertyValue<T> configurationPropertyValue: configurationProperty.getValues()) {
            ExpressionWrapper expressionWrapper = configurationPropertyValue.getExpression();
            if (expressionWrapper == null) {
                return;
            }
            Object expressionObject = expressionWrapper.getExpression();
            if (!(expressionObject instanceof ExpressionType)) {
                throw new IllegalStateException("Expected that expression in " + configurationPropertyValue
                        + " will be ExpressionType, but it was " + expressionObject);
            }
            ExpressionType expressionType = (ExpressionType) expressionWrapper.getExpression();

            Expression<PrismPropertyValue<T>, PrismPropertyDefinition<T>> expression =
                    expressionFactory.makeExpression(
                            expressionType, propDef, MiscSchemaUtil.getExpressionProfile(), shortDesc, task, result);
            VariablesMap variables = new VariablesMap();

            PrismObject<?> configuration = asPrismObject(resourceManager.getSystemConfiguration());
            variables.put(ExpressionConstants.VAR_CONFIGURATION, configuration, SystemConfigurationType.class);
            variables.put(ExpressionConstants.VAR_RESOURCE, resource, ResourceType.class);

            // TODO: are there other variables that should be populated?

            ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null, variables, shortDesc, task);
            eeContext.setExpressionFactory(expressionFactory);
            PrismValueDeltaSetTriple<PrismPropertyValue<T>> expressionOutputTriple = expression.evaluate(eeContext, result);
            Collection<PrismPropertyValue<T>> expressionOutputValues = expressionOutputTriple.getNonNegativeValues();
            if (!expressionOutputValues.isEmpty()) {
                Iterator<PrismPropertyValue<T>> iterator = expressionOutputValues.iterator();
                PrismPropertyValue<T> firstValue = iterator.next();
                configurationPropertyValue.setValue(firstValue.getValue());
                while (iterator.hasNext()) {
                    extraValues.add(iterator.next());
                }
            }
        }
        for (PrismPropertyValue<T> extraValue: extraValues) {
            configurationProperty.add(extraValue);
        }
    }

    /**
     * Applies the definitions to items in resource object delta. Currently, those are definitions for configuration properties;
     * for both main and additional connectors. (For some reason, the `resourceWhenNoOid` is updated as well - if present.)
     *
     * Limitations:
     *
     * - If there's a connector OID change, this method takes the definitions from the new connector. The old one is ignored.
     */
    void applyDefinition(
            ObjectDelta<ResourceType> delta,
            ResourceType resourceWhenNoOid,
            GetOperationOptions options,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, ConfigurationException {

        if (delta.isAdd()) {
            ResourceType resource = delta.getObjectToAdd().asObjectable();
            applyConnectorSchemasToResource(resource, result);
        } else if (delta.isModify()) {
            applyDefinitionToModifyDelta(delta, resourceWhenNoOid, options, task, result);
        } else {
            // Delete delta - nothing to do there
        }
    }

    private void applyDefinitionToModifyDelta(
            ObjectDelta<ResourceType> delta,
            ResourceType resourceWhenNoOid,
            GetOperationOptions options,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, ConfigurationException {

        if (delta.hasCompleteDefinition()) {
            // nothing to do, all modifications have definitions
            return;
        }

        ResourceType resource = getResource(delta, resourceWhenNoOid, options, task, result);

        for (ConnectorSpec connectorSpec : ConnectorSpec.all(resource)) {
            applyDefinitionToDeltaForConnector(delta, connectorSpec, result);
        }
        applyDefinitionsForNewConnectors(delta, result);
    }

    private @NotNull ResourceType getResource(
            ObjectDelta<ResourceType> delta,
            ResourceType resourceWhenNoOid,
            GetOperationOptions options,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, ConfigurationException {
        String resourceOid = delta.getOid();
        if (resourceOid == null) {
            Validate.notNull(resourceWhenNoOid, "Resource oid not specified in the object delta, "
                    + "and resource is not specified as well. Could not apply definition.");
            return resourceWhenNoOid;
        } else {
            return resourceManager.getCompletedResource(resourceOid, options, task, result);
        }
    }

    private void applyDefinitionToDeltaForConnector(
            @NotNull ObjectDelta<ResourceType> delta, @NotNull ConnectorSpec connectorSpec, @NotNull OperationResult parentResult)
            throws SchemaException {
        OperationResult result = parentResult.subresult(OP_APPLY_DEFINITION_TO_DELTA)
                .addArbitraryObjectAsParam("connector", connectorSpec)
                .setMinor()
                .build();
        try {
            String connectorOid = getConnectorOid(delta, connectorSpec);
            if (connectorOid == null) {
                result.recordFatalError("Connector OID is missing");
                return;
            }

            PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDef =
                    getConfigurationContainerDefinition(connectorOid, result);
            if (configurationContainerDef == null) {
                return;
            }

            PrismContainer<ConnectorConfigurationType> connectorConfiguration = connectorSpec.getConnectorConfiguration();
            if (connectorConfiguration != null) {
                connectorConfiguration.applyDefinition(configurationContainerDef);
            }

            for (ItemDelta<?,?> itemDelta : delta.getModifications()){
                applyItemDefinition(itemDelta, connectorSpec, configurationContainerDef, result);
            }

        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private void applyDefinitionsForNewConnectors(ObjectDelta<ResourceType> delta, OperationResult result)
            throws SchemaException {
        List<PrismValue> newConnectors = delta.getNewValuesFor(ResourceType.F_ADDITIONAL_CONNECTOR);
        for (PrismValue newConnectorPcv : newConnectors) {
            //noinspection unchecked
            ConnectorInstanceSpecificationType newConnector =
                    ((PrismContainerValue<ConnectorInstanceSpecificationType>) newConnectorPcv).asContainerable();
            String connectorOid = getOid(newConnector.getConnectorRef());
            if (connectorOid == null) {
                continue;
            }
            PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDef =
                    getConfigurationContainerDefinition(connectorOid, result);
            if (configurationContainerDef == null) {
                continue;
            }
            ConnectorConfigurationType connectorConfiguration = newConnector.getConnectorConfiguration();
            if (connectorConfiguration != null) {
                connectorConfiguration.asPrismContainerValue().applyDefinition(configurationContainerDef);
            }
        }
    }

    private PrismContainerDefinition<ConnectorConfigurationType> getConfigurationContainerDefinition(
            String connectorOid, OperationResult result)
            throws SchemaException {

        ConnectorType connector;
        try {
            connector =
                    repositoryService
                            .getObject(ConnectorType.class, connectorOid, null, result)
                            .asObjectable();
        } catch (ObjectNotFoundException e) {
            // No connector, no fun. We can't apply the definition.
            result.recordFatalError(
                    "Connector (OID:" + connectorOid + ") referenced from the resource is not in the repository", e);
            return null;
        } catch (SchemaException e) {
            // Probably a malformed connector.
            result.recordPartialError("Connector (OID:" + connectorOid + ") referenced from the resource "
                    + "has schema problems: " + e.getMessage(), e);
            LOGGER.error("Connector (OID:{}) has schema problems: {}-{}", connectorOid, e.getMessage(), e);
            return null;
        }

        Element connectorSchemaElement = ConnectorTypeUtil.getConnectorXsdSchema(connector);
        if (connectorSchemaElement == null) {
            // No schema to apply to
            return null;
        }
        MutablePrismSchema connectorSchema;
        try {
            connectorSchema = prismContext.schemaFactory().createPrismSchema(
                    DOMUtil.getSchemaTargetNamespace(connectorSchemaElement));
            connectorSchema.parseThis(
                    connectorSchemaElement, true, "schema for " + connector, prismContext);
        } catch (SchemaException e) {
            throw new SchemaException("Error parsing connector schema for " + connector + ": " + e.getMessage(), e);
        }
        QName configContainerQName = new QName(connector.getNamespace(), ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart());
        PrismContainerDefinition<ConnectorConfigurationType> configContainerDef =
                connectorSchema.findContainerDefinitionByElementName(configContainerQName);
        if (configContainerDef == null) {
            throw new SchemaException("Definition of configuration container " + configContainerQName
                    + " not found in the schema of of " + connector);
        }
        return configContainerDef;
    }

    private String getConnectorOid(ObjectDelta<ResourceType> delta, ConnectorSpec connectorSpec) throws SchemaException {

        // Note: strict=true means that we are looking for this delta defined straight for the property.
        // We do so because here we don't try to apply definitions for additional connectors that are being added.
        ReferenceDelta connectorRefDelta =
                ItemDeltaCollectionsUtil.findItemDelta(
                        delta.getModifications(),
                        connectorSpec.getBasePath().append(ResourceType.F_CONNECTOR_REF),
                        ReferenceDelta.class,
                        true);

        if (connectorRefDelta != null) {
            Item<PrismReferenceValue,PrismReferenceDefinition> connectorRefNew =
                    connectorRefDelta.getItemNewMatchingPath(null);
            if (connectorRefNew.getValues().size() == 1) {
                PrismReferenceValue connectorRefValue = connectorRefNew.getValues().iterator().next();
                if (connectorRefValue.getOid() != null) {
                    // TODO what if there is a dynamic reference?
                    return connectorRefValue.getOid();
                }
            }
        }

        return connectorSpec.getConnectorOid();
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void applyItemDefinition(
            ItemDelta<V,D> itemDelta,
            ConnectorSpec connectorSpec,
            PrismContainerDefinition<ConnectorConfigurationType> configContainerDef,
            OperationResult result) throws SchemaException {
        if (itemDelta.getDefinition() != null) {
            return;
        }
        ItemPath changedItemPath = itemDelta.getPath();
        ItemPath configurationContainerPath = connectorSpec.getConfigurationItemPath();
        if (!changedItemPath.startsWith(configurationContainerPath)) {
            return;
        }

        ItemPath remainder = changedItemPath.remainder(configurationContainerPath);
        if (remainder.isEmpty()) {
            // The delta is for the whole configuration container
            //noinspection unchecked
            itemDelta.applyDefinition((D) configContainerDef);
        } else {
            // The delta is for individual configuration property
            D itemDef = configContainerDef.findItemDefinition(remainder);
            if (itemDef == null) {
                LOGGER.warn("No definition found for item {}. Check your namespaces?", changedItemPath);
                result.recordWarning("No definition found for item delta: " + itemDelta +". Check your namespaces?" );
            } else {
                itemDelta.applyDefinition(itemDef);
            }
        }
    }

    private void checkMutable(PrismObject<ResourceType> resource) {
        if (resource.isImmutable()) {
            throw new IllegalArgumentException("Got immutable resource object, while expecting mutable one: " + resource);
        }
    }

    /**
     * TODO is this method correct?
     */
    void updateSchemaToConnectors(ResourceType resource, ResourceSchema rawResourceSchema, OperationResult result)
            throws ConfigurationException, SchemaException, CommunicationException, ObjectNotFoundException {
        for (ConnectorSpec connectorSpec : ConnectorSpec.all(resource)) {
            connectorManager
                    .getConfiguredAndInitializedConnectorInstance(connectorSpec, false, result)
                    .updateSchema(rawResourceSchema);
        }
    }
}
