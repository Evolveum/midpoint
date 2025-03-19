/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.schema.merger.resource.ResourceMergeOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorInstanceSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * TODO better name
 *
 * Represents an operation where the concrete (incomplete) resource is expanded into its full form by resolving
 * its ancestor(s). This feature is called resource inheritance, and is used to implement e.g. resource templates.
 *
 * = Handling resource configuration data
 *
 * The configuration can be interpreted (and therefore merged) only when its schema is known. Therefore, we should
 * do the resolution process in two passes: first, we resolve the connector(s), second, we resolve everything, including
 * the connector-specific configurations.
 */
class ResourceExpansionOperation {

    private static final String OP_EXPAND = ResourceExpansionOperation.class.getName() + ".expand";

    private static final Trace LOGGER = TraceManager.getTrace(ResourceExpansionOperation.class);

    private static final List<ItemPath> PATHS_FOR_FIRST_PASS = List.of(
            F_NAME, // because it's technically considered obligatory + for diagnostic purposes
            F_CONNECTOR_REF,
            F_SUPER,
            ItemPath.create(F_ADDITIONAL_CONNECTOR, ConnectorInstanceSpecificationType.F_NAME),
            ItemPath.create(F_ADDITIONAL_CONNECTOR, ConnectorInstanceSpecificationType.F_CONNECTOR_REF));

    /**
     * The resource being expanded. This is the object that will be really used as the result of the operation.
     */
    @NotNull private final ResourceType expandedResource;

    /** Useful beans. */
    @NotNull private final CommonBeans beans;

    /**
     * Cached super-resources. Stored as immutables, because the resolution process heavily modifies the resources used.
     * Keyed by OID.
     */
    @NotNull private final Map<String, ResourceType> resourceCache = new HashMap<>();

    /**
     * Definitions for configuration containers for individual connectors. Keyed by (local) connector name.
     */
    @NotNull private final Map<String, PrismContainerDefinition<ConnectorConfigurationType>> connectorConfigurationDefinitions =
            new HashMap<>();

    ResourceExpansionOperation(@NotNull ResourceType expandedResource, @NotNull CommonBeans beans) {
        expandedResource.checkMutable();
        this.expandedResource = expandedResource;
        this.beans = beans;
    }

    /**
     * Executes the expansion operation. Fails hard if e.g. `connectorRef` cannot be resolved.
     */
    public void execute(OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException {
        OperationResult result = parentResult.createMinorSubresult(OP_EXPAND);
        try {
            resolveConnectorsOnly(result);
            resolveInFull(result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private void resolveConnectorsOnly(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {

        ResourceType resourceForConnectors = createLimitedCopy(expandedResource);
        new ExpansionPass(resourceForConnectors, true)
                .execute(result);
        LOGGER.trace("Resource after connectors resolution:\n{}", resourceForConnectors.debugDumpLazily());

        obtainConnectorSchemas(resourceForConnectors, result);
    }

    /**
     * Keeps only the information necessary to know the connectors (for the first pass). See {@link #PATHS_FOR_FIRST_PASS}.
     */
    private @NotNull ResourceType createLimitedCopy(@NotNull ResourceType resource) {
        ResourceType copy = new ResourceType();
        copy.setName(
                CloneUtil.cloneCloneable(resource.getName()));
        copy.setConnectorRef(
                CloneUtil.cloneCloneable(resource.getConnectorRef()));
        for (ConnectorInstanceSpecificationType additionalConnector : resource.getAdditionalConnector()) {
            copy.getAdditionalConnector().add(
                    new ConnectorInstanceSpecificationType()
                            .name(additionalConnector.getName())
                            .connectorRef(
                                    CloneUtil.cloneCloneable(additionalConnector.getConnectorRef())));
        }
        copy.setSuper(
                CloneUtil.cloneCloneable(resource.getSuper()));
        return copy;
    }

    private void obtainConnectorSchemas(ResourceType resourceForConnectors, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        for (ConnectorSpec connectorSpec : ConnectorSpec.all(resourceForConnectors)) {
            var connectorWithSchema = beans.connectorManager.getConnectorWithSchema(connectorSpec, result);
            LOGGER.trace("Stored configuration definition for {}", connectorSpec);
            connectorConfigurationDefinitions.put(
                    connectorSpec.getConnectorName(),
                    connectorWithSchema.getConfigurationContainerDefinition());
        }
    }

    /** Second pass: expands the full content, having the connector configuration definitions. */
    private void resolveInFull(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        applyConnectorDefinitions(expandedResource);
        new ExpansionPass(expandedResource, false)
                .execute(result);
    }

    /** Applies all known connector definitions to given resource object. */
    private void applyConnectorDefinitions(@NotNull ResourceType resource) throws SchemaException, ConfigurationException {
        for (ConnectorSpec connectorSpec : ConnectorSpec.all(resource)) {
            var configurationContainer = connectorSpec.getConnectorConfigurationContainer();
            if (configurationContainer == null) {
                continue;
            }
            String connectorName = connectorSpec.getConnectorName();
            PrismContainerDefinition<ConnectorConfigurationType> definitionFromConnector =
                    MiscUtil.requireNonNull(
                            connectorConfigurationDefinitions.get(connectorName),
                            () -> new IllegalStateException("No connector schema for '" + connectorName + "' in " + resource));
            configurationContainer.applyDefinition(definitionFromConnector);
        }
    }

    public @NotNull Set<String> getAncestorsOids() {
        String expandedResourceOid = expandedResource.getOid();
        if (expandedResourceOid != null) {
            return Sets.difference(
                    resourceCache.keySet(),
                    Set.of(expandedResourceOid));
        } else {
            return resourceCache.keySet();
        }
    }

    @NotNull ResourceType getExpandedResource() {
        return expandedResource;
    }

    /**
     * The single pass of the expansion operation, from the leaf resource up to the top of the hierarchy.
     * Assumes single parent and no cycles (obviously).
     */
    private class ExpansionPass {

        @NotNull private final ResourceType resource;

        /**
         * Are we trying to determine the connectors only (i.e. first pass),
         * or do we do the full resolution (second pass)?
         */
        private final boolean firstPass;

        /** Just for logging purposes. */
        private final int pass;

        ExpansionPass(@NotNull ResourceType resource, boolean firstPass) {
            this.resource = resource;
            this.firstPass = firstPass;
            this.pass = firstPass ? 1 : 2;
        }

        public void execute(OperationResult result)
                throws SchemaException, ConfigurationException, ObjectNotFoundException {

            if (resource.getSuper() == null) {
                LOGGER.trace("Expansion done for {}, as there is no super-resource for it [pass: {}]", resource, pass);
            } else {
                LOGGER.trace("Doing expansion for {} having a super-resource declaration [pass: {}]", resource, pass);
                ResourceType expandedSuperResource = getExpandedSuperResource(result);
                new ResourceMergeOperation(
                        resource,
                        expandedSuperResource)
                        .execute();
                LOGGER.trace("Super-resource {} of {} is expanded [pass: {}]", expandedSuperResource, resource, pass);
            }
        }

        private @NotNull ResourceType getExpandedSuperResource(OperationResult result)
                throws ConfigurationException, ObjectNotFoundException, SchemaException {
            String superOid = getSuperResourceOid();
            ResourceType superResource = getResource(superOid, result).clone();
            if (firstPass) {
                removeUnneededPaths(superResource);
            } else {
                applyConnectorDefinitions(superResource);
            }
            new ExpansionPass(superResource, firstPass)
                    .execute(result);
            return superResource;
        }

        private void removeUnneededPaths(ResourceType resource) throws SchemaException {
            //noinspection unchecked
            resource.asPrismContainerValue().keepPaths(PATHS_FOR_FIRST_PASS);
        }

        private String getSuperResourceOid() throws ConfigurationException {
            ObjectReferenceType superRef = MiscUtil.requireNonNull(
                    resource.getSuper().getResourceRef(),
                    () -> new ConfigurationException("No resourceRef in super-resource declaration"));

            return MiscUtil.requireNonNull(
                    superRef.getOid(),
                    () -> new ConfigurationException("No OID in super-resource reference (filters are not supported there, yet)"));
        }

        private @NotNull ResourceType getResource(String oid, OperationResult result)
                throws ObjectNotFoundException, SchemaException, ConfigurationException {
            if (firstPass) {
                ResourceType resource = beans.repositoryService
                        .getObject(ResourceType.class, oid, readOnly(), result)
                        .asObjectable();
                if (resourceCache.put(oid, resource) != null) {
                    // If multiple inheritance is allowed, we'd need to relax this check.
                    throw new ConfigurationException("There is a cycle in resource 'super' references for " + resource);
                }
                return resource;
            } else {
                return MiscUtil.requireNonNull(
                        resourceCache.get(oid),
                        () -> new IllegalStateException("Resource " + oid + " is not in the resource cache. Huh?"));
            }
        }
    }
}
