/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.factory.wrapper;

import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.model.api.MetadataItemProcessingSpec;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.ItemSecurityConstraints;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author katka
 *
 */
public class WrapperContext {

    private AuthorizationPhaseType authzPhase = AuthorizationPhaseType.REQUEST;
    private Task task;
    private OperationResult result;

    private boolean createIfEmpty;

    private Boolean readOnly;
    private boolean showEmpty;
    private boolean isMetadata;

    private ItemStatus objectStatus;
    private PrismObject<?> object;

    //Shadow related attributes
    private ResourceType resource;
    private ResourceShadowCoordinates coordinates;

    //Association related attributes
    private Collection<ResourceAssociationDefinition> resourceAssociationDefinitions;

    //used e.g. for metadata - opertionsla attributes but want to create wrappers for them
    private boolean createOperational;

    private List<VirtualContainerItemSpecificationType>  virtualItemSpecification;

    private MetadataItemProcessingSpec metadataItemProcessingSpec;

    //to avoid duplicate loading of the same lookuptable, maybe later we will need this
    //for different types of objects
     @Experimental
    private Map<String, LookupTableType> lookupTableCache = new HashMap();

     private List<? extends ContainerPanelConfigurationType> detailsPageTypeConfiguration;
    private Collection<VirtualContainersSpecificationType> virtualContainers = new ArrayList<>();

    private ItemSecurityConstraints securityConstraints;

    private MappingDirection attributeMappingType;
    private boolean configureMappingType;

    private boolean isShowedByWizard;

    public WrapperContext(Task task, OperationResult result) {
        this.task = task;
        this.result = result != null ? result : new OperationResult("temporary");       // TODO !!!
    }

    public AuthorizationPhaseType getAuthzPhase() {
        return authzPhase;
    }
    public Task getTask() {
        return task;
    }
    public OperationResult getResult() {
        return result;
    }
    public void setAuthzPhase(AuthorizationPhaseType authzPhase) {
        this.authzPhase = authzPhase;
    }
    public void setTask(Task task) {
        this.task = task;
    }
    public void setResult(OperationResult result) {
        this.result = result;
    }

    public boolean isCreateIfEmpty() {
        return createIfEmpty;
    }

    public void setCreateIfEmpty(boolean createIfEmpty) {
        this.createIfEmpty = createIfEmpty;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }


    public ResourceType getResource() {
        return resource;
    }

    public ResourceShadowCoordinates getCoordinates() {
        return coordinates;
    }

    public void setResource(ResourceType resource) {
        this.resource = resource;
    }

    public Collection<ResourceAssociationDefinition> getRefinedAssociationDefinitions() {
        return resourceAssociationDefinitions;
    }

    public void setRefinedAssociationDefinitions(Collection<ResourceAssociationDefinition> resourceAssociationDefinitions) {
        this.resourceAssociationDefinitions = resourceAssociationDefinitions;
    }

    public void setCoordinates(ResourceShadowCoordinates coordinates) {
        this.coordinates = coordinates;
    }

    public boolean isShowEmpty() {
        return showEmpty;
    }

    public void setShowEmpty(boolean showEmpty) {
        this.showEmpty = showEmpty;
    }

    public ItemStatus getObjectStatus() {
        return objectStatus;
    }

    public void setObjectStatus(ItemStatus objectStatus) {
        this.objectStatus = objectStatus;
    }

    public boolean isCreateOperational() {
        return createOperational;
    }

    public void setCreateOperational(boolean createOperational) {
        this.createOperational = createOperational;
    }

//    public Collection<VirtualContainersSpecificationType> getVirtualContainers() {
//        return virtualContainers;
//    }

//    public void setVirtualContainers(Collection<VirtualContainersSpecificationType> virtualContainers) {
//        this.virtualContainers = virtualContainers;
//    }

    public void setVirtualItemSpecification(List<VirtualContainerItemSpecificationType> virtualItemSpecification) {
        this.virtualItemSpecification = virtualItemSpecification;
    }

    public List<VirtualContainerItemSpecificationType> getVirtualItemSpecification() {
        return virtualItemSpecification;
    }

    public void setMetadataItemProcessingSpec(MetadataItemProcessingSpec metadataItemProcessingSpec) {
        this.metadataItemProcessingSpec = metadataItemProcessingSpec;
    }

    public boolean isProcessMetadataFor(ItemPath path) throws SchemaException {
        if (metadataItemProcessingSpec == null) {
            return false;
        }

        return metadataItemProcessingSpec.isFullProcessing(path);
    }

    public PrismObject<?> getObject() {
        return object;
    }

    public void setObject(PrismObject<?> object) {
        this.object = object;
    }

    public void setMetadata(boolean metadata) {
        isMetadata = metadata;
    }

    public boolean isMetadata() {
        return isMetadata;
    }

    public void rememberLookuptable(LookupTableType lookupTableType) {
        lookupTableCache.put(lookupTableType.getOid(), lookupTableType);
    }

    public LookupTableType getLookuptableFromCache(String oid) {
        return lookupTableCache.get(oid);
    }

    public Collection<VirtualContainersSpecificationType> getVirtualContainers() {
        if (!virtualContainers.isEmpty()) {
            return virtualContainers;
        }

        if (detailsPageTypeConfiguration == null) {
            return virtualContainers;
        }
        List<? extends ContainerPanelConfigurationType> containerPanelConfigurationTypes = detailsPageTypeConfiguration;
        if (containerPanelConfigurationTypes.isEmpty()) {
            return virtualContainers;
        }

        collectVirtualContainers(containerPanelConfigurationTypes, virtualContainers);
        return virtualContainers;
    }

    protected void collectVirtualContainers(@NotNull Collection<? extends ContainerPanelConfigurationType> panelConfigs, Collection<VirtualContainersSpecificationType> virtualContainers) {
        for (ContainerPanelConfigurationType panelConfig : panelConfigs) {
            if ((isShowedByWizard && OperationTypeType.WIZARD.equals(panelConfig.getApplicableForOperation()))
                    || (!isShowedByWizard
                        && (objectStatus == null || panelConfig.getApplicableForOperation() == null
                            || (ItemStatus.NOT_CHANGED.equals(objectStatus)
                                && OperationTypeType.MODIFY.equals(panelConfig.getApplicableForOperation()))
                            || (ItemStatus.ADDED.equals(objectStatus)
                                && OperationTypeType.ADD.equals(panelConfig.getApplicableForOperation()))))) {
                virtualContainers.addAll(panelConfig.getContainer());
                collectVirtualContainers(panelConfig.getPanel(), virtualContainers);
            }
        }
    }

    public VirtualContainersSpecificationType findVirtualContainerConfiguration(ItemPath path) {
        for (VirtualContainersSpecificationType virtualContainer : getVirtualContainers()) {
            if (virtualContainer.getPath() != null && path.equivalent(virtualContainer.getPath().getItemPath())) {
                return virtualContainer;
            }
        }
        return null;
    }

    public void setDetailsPageTypeConfiguration(List<? extends ContainerPanelConfigurationType> detailsPageTypeConfiguration) {
        this.detailsPageTypeConfiguration = detailsPageTypeConfiguration;
    }

    public void setAttributeMappingType(MappingDirection attributeMappingType) {
        this.attributeMappingType = attributeMappingType;
    }

    public MappingDirection getAttributeMappingType() {
        return attributeMappingType;
    }

    public void setConfigureMappingType(boolean configureMappingType) {
        this.configureMappingType = configureMappingType;
    }

    public boolean isConfigureMappingType() {
        return configureMappingType;
    }

    public void setShowedByWizard(boolean showedByWizard) {
        isShowedByWizard = showedByWizard;
    }

    public boolean isShowedByWizard() {
        return isShowedByWizard;
    }

    public void setSecurityConstraints(ItemSecurityConstraints securityConstraints) {
        this.securityConstraints = securityConstraints;
    }

    public ItemSecurityConstraints getSecurityConstraints() {
        return securityConstraints;
    }

    public WrapperContext clone() {
        WrapperContext ctx = new WrapperContext(task,result);
        ctx.setAuthzPhase(authzPhase);
        ctx.setCreateIfEmpty(createIfEmpty);
        ctx.setReadOnly(readOnly);
        ctx.setShowEmpty(showEmpty);
        ctx.setObjectStatus(objectStatus);
        ctx.setResource(resource);
        ctx.setCoordinates(coordinates);
        ctx.setCreateOperational(createOperational);
        ctx.setVirtualItemSpecification(virtualItemSpecification);
        ctx.setObject(object);
        ctx.setMetadata(isMetadata);
        ctx.setMetadataItemProcessingSpec(metadataItemProcessingSpec);
        ctx.lookupTableCache = lookupTableCache;
        ctx.setDetailsPageTypeConfiguration(detailsPageTypeConfiguration);
        ctx.setAttributeMappingType(attributeMappingType);
        ctx.setConfigureMappingType(configureMappingType);
        ctx.setShowedByWizard(isShowedByWizard);
        ctx.setSecurityConstraints(securityConstraints);
        return ctx;
    }
}
