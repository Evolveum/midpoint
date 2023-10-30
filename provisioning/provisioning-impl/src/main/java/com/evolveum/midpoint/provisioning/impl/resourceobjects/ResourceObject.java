/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.ucf.api.UcfObjectFound;
import com.evolveum.midpoint.provisioning.ucf.api.UcfResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;

/**
 * A resource object: either actually residing on a resource (i.e., something that we learned about by calling UCF),
 * or just created or to-be created on a resource.
 *
 * A special case: a resource object that is not on the resource, but it is in the repository. TODO To be clarified.
 *
 * In some cases (e.g., when dealing with resource object changes), it may be almost empty, containing only selected identifiers.
 */
public class ResourceObject implements Serializable, Cloneable, DebugDumpable {

    /**
     * TODO specify various levels of guarantees: what is there and what is not, like activation, associations, and so on.
     */
    @NotNull private final ShadowType bean;

    /**
     * Real value of the object primary identifier (e.g. ConnId UID).
     * Usually not null (e.g. in ConnId 1.x), but this can change in the future.
     *
     * We assume it's immutable, like a {@link String}, {@link Long}, and so on.
     *
     * See {@link UcfResourceObject#primaryIdentifierValue}.
     */
    private final Object primaryIdentifierValue;

    private ResourceObject(@NotNull ShadowType bean, Object primaryIdentifierValue) {
        this.bean = bean;
        this.primaryIdentifierValue = primaryIdentifierValue;
    }

    public static ResourceObject from(@NotNull UcfObjectFound ucfObject) {
        return new ResourceObject(
                ucfObject.getPrismObject().clone().asObjectable(),
                ucfObject.getPrimaryIdentifierValue());
    }

    public static ResourceObject from(@NotNull UcfResourceObject ucfResourceObject) {
        return new ResourceObject(
                ucfResourceObject.bean(),
                ucfResourceObject.primaryIdentifierValue());
    }

    public static ResourceObject fromNullable(@Nullable UcfResourceObject ucfResourceObject) {
        return ucfResourceObject != null ? from(ucfResourceObject) : null;
    }

    public static @Nullable ShadowType getBean(@Nullable ResourceObject resourceObject) {
        return resourceObject != null ? resourceObject.getBean() : null;
    }

    // TODO we should perhaps indicate that the source is repo!
    public static ResourceObject fromRepoPrismObject(@NotNull PrismObject<ShadowType> object, Object primaryIdentifierValue) {
        return new ResourceObject(object.asObjectable(), primaryIdentifierValue);
    }

    public static ResourceObject fromPrismObject(@NotNull PrismObject<ShadowType> object, Object primaryIdentifierValue) {
        return new ResourceObject(object.asObjectable(), primaryIdentifierValue);
    }

    public static ResourceObject fromBean(@NotNull ShadowType bean, Object primaryIdentifierValue) {
        return new ResourceObject(bean, primaryIdentifierValue);
    }

    public @NotNull ShadowType getBean() {
        return bean;
    }

    public @NotNull PrismObject<ShadowType> getPrismObject() {
        return bean.asPrismObject();
    }

    public Object getPrimaryIdentifierValue() {
        return primaryIdentifierValue;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ResourceObject clone() {
        return new ResourceObject(
                bean.clone(),
                primaryIdentifierValue);
    }

    @Override
    public String toString() {
        return "ResourceObject[%s: %s]".formatted(primaryIdentifierValue, bean);
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilder(
                this.getClass().getSimpleName() + " [" + primaryIdentifierValue + "]", indent);
        DebugUtil.debugDumpWithLabel(sb, "bean", bean, indent + 1);
        return sb.toString();
    }

    public ResourceAttributeContainer getAttributesContainer() {
        return ShadowUtil.getAttributesContainer(bean);
    }

    public Collection<ResourceAttribute<?>> getAttributes() {
        return ShadowUtil.getAttributes(bean);
    }

    public PolyString determineShadowName() throws SchemaException {
        return ShadowUtil.determineShadowName(bean);
    }
}
