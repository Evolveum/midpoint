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

import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * TODO decide about this class; probably should be removed
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

    private Shadow(
            @NotNull ShadowType bean,
            @NotNull Resource resource) {
        Preconditions.checkNotNull(bean.getShadowLifecycleState(), "No shadow lifecycle state in %s", bean);
        this.bean = bean;
        this.resource = resource;

        checkConsistence();
    }

    public static @NotNull Shadow of(
            @NotNull ShadowType bean, @NotNull Resource resource) {
        return new Shadow(bean, resource);
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

    @Override
    public @NotNull AbstractShadow withNewContent(@NotNull ShadowType newBean) {
        return new Shadow(newBean, resource);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Shadow clone() {
        return new Shadow(bean.clone(), resource);
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
                && Objects.equals(this.resource, that.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bean, resource);
    }

    @Override
    public String toString() {
        return "Shadow[" +
                "bean=" + bean + ", " +
                "resource=" + resource + ']';
    }

    public String debugDump(int indent) {
        return bean.debugDump(indent); // FIXME temporary implementation
    }

    @Override
    public String getResourceOid() {
        return resource.getOid();
    }
}
