/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainerItemSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

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

    private ItemStatus objectStatus;
    private PrismObject<?> object;

    //Shadow related attributes
    private ResourceType resource;
    private ResourceShadowDiscriminator discriminator;

    //used e.g. for metadata - opertionsla attributes but want to create wrappers for them
    private boolean createOperational;

    private Collection<VirtualContainersSpecificationType> virtualContainers;
    private List<VirtualContainerItemSpecificationType>  virtualItemSpecification;

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

    public ResourceShadowDiscriminator getDiscriminator() {
        return discriminator;
    }

    public void setResource(ResourceType resource) {
        this.resource = resource;
    }

    public void setDiscriminator(ResourceShadowDiscriminator discriminator) {
        this.discriminator = discriminator;
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

    public Collection<VirtualContainersSpecificationType> getVirtualContainers() {
        return virtualContainers;
    }

    public void setVirtualContainers(Collection<VirtualContainersSpecificationType> virtualContainers) {
        this.virtualContainers = virtualContainers;
    }

    public void setVirtualItemSpecification(List<VirtualContainerItemSpecificationType> virtualItemSpecification) {
        this.virtualItemSpecification = virtualItemSpecification;
    }

    public List<VirtualContainerItemSpecificationType> getVirtualItemSpecification() {
        return virtualItemSpecification;
    }

    public PrismObject<?> getObject() {
        return object;
    }

    public void setObject(PrismObject<?> object) {
        this.object = object;
    }

    public WrapperContext clone() {
        WrapperContext ctx = new WrapperContext(task,result);
        ctx.setAuthzPhase(authzPhase);
        ctx.setCreateIfEmpty(createIfEmpty);
        ctx.setReadOnly(readOnly);
        ctx.setShowEmpty(showEmpty);
        ctx.setObjectStatus(objectStatus);
        ctx.setResource(resource);
        ctx.setDiscriminator(discriminator);
        ctx.setCreateOperational(createOperational);
        ctx.setVirtualContainers(virtualContainers);
        ctx.setVirtualItemSpecification(virtualItemSpecification);
        ctx.setObject(object);
        return ctx;
    }
}
