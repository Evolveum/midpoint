/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import java.util.Objects;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

/**
 * TODO decide about this class
 *
 * Currently, it is used as a return value from "shadows" package, internally in `provisioning-impl` module.
 *
 * Conditions:
 *
 * . the object definition is known
 * . the resource is known
 * . the bean has the definitions applied
 * . the shadow state is up-to-date
 */
public class Shadow implements AbstractShadow {

    @NotNull private final ShadowType bean;
    @NotNull private final Resource resource;
    @NotNull private final ResourceObjectDefinition objectDefinition;

    private Shadow(
            @NotNull ShadowType bean,
            @NotNull Resource resource,
            @NotNull ResourceObjectDefinition objectDefinition) {
        Preconditions.checkNotNull(bean.getShadowLifecycleState(), "No shadow lifecycle state in %s", bean);
        this.bean = bean;
        this.resource = resource;
        this.objectDefinition = objectDefinition;
    }

    public static @NotNull Shadow of(
            @NotNull ShadowType bean,
            @NotNull Resource resource,
            @NotNull ResourceObjectDefinition objectDefinition) {
        return new Shadow(bean, resource, objectDefinition);
    }

    public static @NotNull Shadow of(
            @NotNull ResourceType resourceBean, @NotNull ShadowType shadowBean)
            throws SchemaException, ConfigurationException {
        Resource resource = Resource.of(resourceBean);
        var resourceSchema = resource.getCompleteSchemaRequired();
        var objectDefinition = MiscUtil.requireNonNull(
                resourceSchema.findDefinitionForShadow(shadowBean),
                () -> "No object definition for shadow " + shadowBean);
        return new Shadow(
                shadowBean,
                resource,
                objectDefinition);
    }

    @NotNull
    public ShadowLifecycleStateType getShadowLifecycleState() {
        return bean.getShadowLifecycleState();
    }

    public @NotNull ShadowType getBean() {
        return bean;
    }

    public @NotNull Resource getResource() {
        return resource;
    }

    public @NotNull ResourceObjectDefinition getObjectDefinition() {
        return objectDefinition;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (Shadow) obj;
        return Objects.equals(this.bean, that.bean)
                && Objects.equals(this.resource, that.resource)
                && Objects.equals(this.objectDefinition, that.objectDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bean, resource, objectDefinition);
    }

    @Override
    public String toString() {
        return "Shadow[" +
                "bean=" + bean + ", " +
                "resource=" + resource + ", " +
                "objectDefinition=" + objectDefinition + ']';
    }

    public String debugDump(int indent) {
        return bean.debugDump(indent); // FIXME temporary implementation
    }

    @Override
    public @NotNull String getResourceOid() {
        return resource.getOid();
    }

    @Override
    public @NotNull QName getObjectClass() throws SchemaException {
        return MiscUtil.stateNonNull(bean.getObjectClass(), "No object class in %s", this);
    }
}
