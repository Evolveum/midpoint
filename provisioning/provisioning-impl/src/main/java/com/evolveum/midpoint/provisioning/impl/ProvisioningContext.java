/*
 * Copyright (c) 2015-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.common.ResourceObjectPattern;
import com.evolveum.midpoint.common.refinery.*;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author semancik
 */
public class ProvisioningContext extends StateReporter {

    private static final Trace LOGGER = TraceManager.getTrace(ProvisioningContext.class);

    @NotNull private final ResourceManager resourceManager;
    private OperationResult parentResult;       // Use only when absolutely necessary!
    private Collection<SelectorOptions<GetOperationOptions>> getOperationOptions;

    private PrismObject<ShadowType> originalShadow;
    private ResourceShadowDiscriminator shadowCoordinates;
    private Collection<QName> additionalAuxiliaryObjectClassQNames;
    private boolean useRefinedDefinition = true;
    private boolean isPropagation;

    private RefinedObjectClassDefinition objectClassDefinition;

    private ResourceType resource;
    private Map<Class<? extends CapabilityType>,ConnectorInstance> connectorMap;
    private RefinedResourceSchema refinedSchema;

    private String channelOverride;

    Collection<ResourceObjectPattern> protectedAccountPatterns;

    public ProvisioningContext(@NotNull ResourceManager resourceManager, OperationResult parentResult) {
        this.resourceManager = resourceManager;
        this.parentResult = parentResult;
    }

    public void setResourceOid(String resourceOid) {
        super.setResourceOid(resourceOid);
        this.resource = null;
        this.connectorMap = null;
        this.refinedSchema = null;
    }

    public Collection<SelectorOptions<GetOperationOptions>> getGetOperationOptions() {
        return getOperationOptions;
    }

    public void setGetOperationOptions(Collection<SelectorOptions<GetOperationOptions>> getOperationOptions) {
        this.getOperationOptions = getOperationOptions;
    }

    public ResourceShadowDiscriminator getShadowCoordinates() {
        return shadowCoordinates;
    }

    public void setShadowCoordinates(ResourceShadowDiscriminator shadowCoordinates) {
        this.shadowCoordinates = shadowCoordinates;
    }

    public PrismObject<ShadowType> getOriginalShadow() {
        return originalShadow;
    }

    public void setOriginalShadow(PrismObject<ShadowType> originalShadow) {
        this.originalShadow = originalShadow;
    }

    public Collection<QName> getAdditionalAuxiliaryObjectClassQNames() {
        return additionalAuxiliaryObjectClassQNames;
    }

    public void setAdditionalAuxiliaryObjectClassQNames(Collection<QName> additionalAuxiliaryObjectClassQNames) {
        this.additionalAuxiliaryObjectClassQNames = additionalAuxiliaryObjectClassQNames;
    }

    public boolean isUseRefinedDefinition() {
        return useRefinedDefinition;
    }

    public void setUseRefinedDefinition(boolean useRefinedDefinition) {
        this.useRefinedDefinition = useRefinedDefinition;
    }

    public boolean isPropagation() {
        return isPropagation;
    }

    public void setPropagation(boolean isPropagation) {
        this.isPropagation = isPropagation;
    }

    public void setObjectClassDefinition(RefinedObjectClassDefinition objectClassDefinition) {
        this.objectClassDefinition = objectClassDefinition;
    }

    public void setResource(ResourceType resource) {
        this.resource = resource;
    }

    public ResourceType getResource() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (resource == null) {
            if (getResourceOid() == null) {
                throw new SchemaException("Null resource OID "+getDesc());
            }
            GetOperationOptions options = GetOperationOptions.createReadOnly();
            resource = resourceManager.getResource(getResourceOid(), options, getTask(), parentResult).asObjectable();
            updateResourceName();
        }
        return resource;
    }

    private void updateResourceName() {
        if (resource != null && resource.getName() != null) {
            super.setResourceName(resource.getName().getOrig());
        }
    }

    public RefinedResourceSchema getRefinedSchema() throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        if (refinedSchema == null) {
            refinedSchema = ProvisioningUtil.getRefinedSchema(getResource());
        }
        return refinedSchema;
    }

    public RefinedObjectClassDefinition getObjectClassDefinition() throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        if (objectClassDefinition == null) {
            if (useRefinedDefinition) {
                if (originalShadow != null) {
                    objectClassDefinition = getRefinedSchema().determineCompositeObjectClassDefinition(originalShadow, additionalAuxiliaryObjectClassQNames);
                } else if (shadowCoordinates != null && !shadowCoordinates.isWildcard()) {
                    objectClassDefinition = getRefinedSchema().determineCompositeObjectClassDefinition(shadowCoordinates);
                }
            } else {
                if (shadowCoordinates.getObjectClass() == null) {
                    throw new IllegalStateException("No objectclass");
                }
                ObjectClassComplexTypeDefinition origObjectClassDefinition = getRefinedSchema().getOriginalResourceSchema().findObjectClassDefinition(shadowCoordinates.getObjectClass());
                if (origObjectClassDefinition == null) {
                    throw new SchemaException("No object class definition for "+shadowCoordinates.getObjectClass()+" in original resource schema for "+getResource());
                } else {
                    objectClassDefinition = RefinedObjectClassDefinitionImpl.parseFromSchema(origObjectClassDefinition, getResource(), getRefinedSchema(), getResource().asPrismObject().getPrismContext(),
                        "objectclass "+origObjectClassDefinition+" in "+getResource());
                }
            }
        }
        return objectClassDefinition;
    }

    public Collection<ResourceObjectPattern> getProtectedAccountPatterns(ExpressionFactory expressionFactory, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException, SecurityViolationException {
        if (protectedAccountPatterns != null) {
            return protectedAccountPatterns;
        }

        protectedAccountPatterns = new ArrayList<>();

        RefinedObjectClassDefinition objectClassDefinition = getObjectClassDefinition();
        Collection<ResourceObjectPattern> patterns = objectClassDefinition.getProtectedObjectPatterns();
        for (ResourceObjectPattern pattern : patterns) {
            ObjectFilter filter = pattern.getObjectFilter();
            if (filter == null) {
                continue;
            }
            ExpressionVariables variables = new ExpressionVariables();
            variables.put(ExpressionConstants.VAR_RESOURCE, resource, ResourceType.class);
            variables.put(ExpressionConstants.VAR_CONFIGURATION, resourceManager.getSystemConfiguration(), SystemConfigurationType.class);
            ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(filter, variables, MiscSchemaUtil.getExpressionProfile(), expressionFactory, getPrismContext(), "protected filter", getTask(), result);
            pattern.addFilter(evaluatedFilter);
            protectedAccountPatterns.add(pattern);
        }

        return protectedAccountPatterns;
    }

    // we don't use additionalAuxiliaryObjectClassQNames as we don't know if they are initialized correctly [med] TODO: reconsider this
    public CompositeRefinedObjectClassDefinition computeCompositeObjectClassDefinition(@NotNull Collection<QName> auxObjectClassQNames)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        RefinedObjectClassDefinition structuralObjectClassDefinition = getObjectClassDefinition();
        if (structuralObjectClassDefinition == null) {
            return null;
        }
        Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions = new ArrayList<>(auxObjectClassQNames.size());
        for (QName auxObjectClassQName : auxObjectClassQNames) {
            RefinedObjectClassDefinition auxObjectClassDef = refinedSchema.getRefinedDefinition(auxObjectClassQName);
            if (auxObjectClassDef == null) {
                throw new SchemaException("Auxiliary object class " + auxObjectClassQName + " specified in " + this + " does not exist");
            }
            auxiliaryObjectClassDefinitions.add(auxObjectClassDef);
        }
        return new CompositeRefinedObjectClassDefinitionImpl(structuralObjectClassDefinition, auxiliaryObjectClassDefinitions);
    }

    public RefinedObjectClassDefinition computeCompositeObjectClassDefinition(PrismObject<ShadowType> shadow)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return computeCompositeObjectClassDefinition(shadow.asObjectable().getAuxiliaryObjectClass());
    }

    public String getChannel() {
        Task task = getTask();
        if (task != null && channelOverride == null) {
            return task.getChannel();
        } else {
            return channelOverride;
        }
    }

    public <T extends CapabilityType> ConnectorInstance getConnector(Class<T> operationCapabilityClass, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (connectorMap == null) {
            connectorMap = new HashMap<>();
        }
        ConnectorInstance connector = connectorMap.get(operationCapabilityClass);
        if (connector == null) {
            connector = getConnectorInstance(operationCapabilityClass, parentResult);
            connectorMap.put(operationCapabilityClass, connector);
        }
        return connector;
    }

    public boolean isWildcard() {
        return (shadowCoordinates == null && originalShadow == null) || (shadowCoordinates != null && shadowCoordinates.isWildcard());
    }

    /**
     * Creates a context for a different object class on the same resource.
     */
    public ProvisioningContext spawn(ShadowKindType kind, String intent) {
        ProvisioningContext ctx = spawnSameResource();
        ctx.shadowCoordinates = new ResourceShadowDiscriminator(getResourceOid(), kind, intent, null, false);
        return ctx;
    }

    /**
     * Creates a context for a different object class on the same resource.
     */
    public ProvisioningContext spawn(QName objectClassQName, Task workerTask) {
        ProvisioningContext child = spawn(objectClassQName);
        child.setTask(workerTask);
        return child;
    }

    public ProvisioningContext spawn(QName objectClassQName) {
        ProvisioningContext ctx = spawnSameResource();
        ctx.shadowCoordinates = new ResourceShadowDiscriminator(getResourceOid(), null, null, null, false);
        ctx.shadowCoordinates.setObjectClass(objectClassQName);
        return ctx;
    }

    public ProvisioningContext spawn(PrismObject<ShadowType> shadow, Task workerTask) {
        ProvisioningContext child = spawn(shadow);
        child.setTask(workerTask);
        return child;
    }

    /**
     * Creates a context for a different object class on the same resource.
     */
    public ProvisioningContext spawn(PrismObject<ShadowType> shadow) {
        ProvisioningContext ctx = spawnSameResource();
        ctx.setOriginalShadow(shadow);
        return ctx;
    }

    private ProvisioningContext spawnSameResource() {
        ProvisioningContext ctx = new ProvisioningContext(resourceManager, parentResult);
        ctx.setTask(this.getTask());
        ctx.setResourceOid(getResourceOid());
        ctx.resource = this.resource;
        ctx.updateResourceName(); // TODO eliminate this mess - check if we need StateReporter any more
        ctx.connectorMap = this.connectorMap;
        ctx.refinedSchema = this.refinedSchema;
        ctx.channelOverride = this.channelOverride;
        return ctx;
    }

    public void assertDefinition(String message) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        if (getObjectClassDefinition() == null) {
            throw new SchemaException(message + " " + getDesc());
        }
    }

    public void assertDefinition() throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        assertDefinition("Cannot locate object class definition");
    }

    public String getDesc() {
        if (originalShadow != null) {
            return "for " + originalShadow + " in " + (resource==null?("resource "+getResourceOid()):resource);
        } else if (shadowCoordinates != null && !shadowCoordinates.isWildcard()) {
            return "for " + shadowCoordinates + " in " + (resource==null?("resource "+getResourceOid()):resource);
        } else {
            return "for wildcard in " + (resource==null?("resource "+getResourceOid()):resource);
        }
    }

    private <T extends CapabilityType> ConnectorInstance getConnectorInstance(Class<T> operationCapabilityClass, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        OperationResult connectorResult = parentResult.createMinorSubresult(ProvisioningContext.class.getName() + ".getConnectorInstance");
        try {
            ConnectorInstance connector = resourceManager.getConfiguredConnectorInstance(getResource().asPrismObject(), operationCapabilityClass, false, connectorResult);
            connectorResult.recordSuccess();
            return connector;
        } catch (ObjectNotFoundException | SchemaException e) {
            connectorResult.recordPartialError("Could not get connector instance " + getDesc() + ": " +  e.getMessage(),  e);
            // Wrap those exceptions to a configuration exception. In the context of the provisioning operation we really cannot throw
            // ObjectNotFoundException exception. If we do that then the consistency code will interpret that as if the resource object
            // (shadow) is missing. But that's wrong. We do not have connector therefore we do not know anything about the shadow. We cannot
            // throw ObjectNotFoundException here.
            throw new ConfigurationException(e.getMessage(), e);
        } catch (CommunicationException | ConfigurationException | RuntimeException e) {
            connectorResult.recordPartialError("Could not get connector instance " + getDesc() + ": " +  e.getMessage(),  e);
            throw e;
        }
    }


    //check connector capabilities in this order :
    // 1. take additional connector capabilieis if exist, if not, take resource capabilities
    // 2. apply object class specific capabilities to the one selected in step 1.
    // 3. in the returned capabilieties, check first configured capabilities and then native capabilities
    public <T extends CapabilityType> T getEffectiveCapability(Class<T> capabilityClass) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        CapabilitiesType capabilitiesType = getConnectorCapabilities(capabilityClass);
        if (capabilitiesType == null) {
            return null;
        }
        return CapabilityUtil.getEffectiveCapability(capabilitiesType, capabilityClass);
    }

    public <T extends  CapabilityType> boolean hasNativeCapability(Class<T> capabilityClass) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        CapabilitiesType connectorCapabilities = getConnectorCapabilities(capabilityClass);
        if (connectorCapabilities == null) {
            return false;
        }
        return CapabilityUtil.hasNativeCapability(connectorCapabilities, capabilityClass);
    }

    public <T extends  CapabilityType> boolean hasConfiguredCapability(Class<T> capabilityClass) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        CapabilitiesType connectorCapabilities = getConnectorCapabilities(capabilityClass);
        if (connectorCapabilities == null) {
            return false;
        }
        return CapabilityUtil.hasConfiguredCapability(connectorCapabilities, capabilityClass);
    }

    private <T extends CapabilityType> CapabilitiesType getConnectorCapabilities(Class<T> operationCapabilityClass) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return resourceManager.getConnectorCapabilities(getResource(), getObjectClassDefinition(), operationCapabilityClass);
    }

    @Override
    public String toString() {
        return "ProvisioningContext("+getDesc()+")";
    }

    @NotNull
    public PrismContext getPrismContext() {
        return resourceManager.getPrismContext();
    }

    public ItemPath path(Object... components) {
        return ItemPath.create(components);
    }

    public CachingStategyType getCachingStrategy()
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return ProvisioningUtil.getCachingStrategy(this);
    }

    public String getChannelOverride() {
        return channelOverride;
    }

    public void setChannelOverride(String channelOverride) {
        this.channelOverride = channelOverride;
    }

    public String toHumanReadableDescription() {
        StringBuilder sb = new StringBuilder();
        if (shadowCoordinates != null) {
            if (shadowCoordinates.getKind() == null) {
                sb.append(PrettyPrinter.prettyPrint(shadowCoordinates.getObjectClass()));
            } else {
                sb.append(shadowCoordinates.getKind() == null ? "null" : shadowCoordinates.getKind().value());
                sb.append(" (").append(shadowCoordinates.getIntent());
                if (shadowCoordinates.getTag() != null) {
                    sb.append("/").append(shadowCoordinates.getTag());
                }
                sb.append(")");
            }
        }
        sb.append(" @");
        if (resource != null) {
            sb.append(resource);
        } else if (shadowCoordinates != null) {
            sb.append(shadowCoordinates.getResourceOid());
        }
        if (shadowCoordinates != null && shadowCoordinates.isTombstone()) {
            sb.append(" TOMBSTONE");
        }
        return sb.toString();
    }

    public Object toHumanReadableDescriptionLazy() {
        return new Object() {
            @Override
            public String toString() {
                return toHumanReadableDescription();
            }
        };
    }
}
