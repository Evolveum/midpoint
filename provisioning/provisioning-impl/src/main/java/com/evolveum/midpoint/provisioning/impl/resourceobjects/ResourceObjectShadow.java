/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.Shadow;
import com.evolveum.midpoint.provisioning.ucf.api.UcfResourceObject;
import com.evolveum.midpoint.schema.processor.ShadowAttributesContainer;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * A resource object shadow: either actually residing on a resource (i.e., something that we learned about by calling UCF),
 * or just created or to-be created on a resource. In some cases (e.g., when dealing with resource object changes),
 * it may be almost empty, containing only selected identifiers.
 *
 * It may even come from the repository, providing it fulfills all the conditions. TODO which conditions?
 *
 * Properties:
 *
 * . it has a definition, and that definition is correctly applied
 *
 * TODO
 *  - `exists` flag
 *
 * @see ExistingResourceObjectShadow
 * @see RepoShadow
 */
public class ResourceObjectShadow implements Serializable, Cloneable, DebugDumpable, AbstractShadow {

    /**
     * TODO specify various levels of guarantees: what is there and what is not, like activation, associations, and so on.
     */
    @NotNull final ShadowType bean;

    /**
     * Real value of the object primary identifier (e.g. ConnId UID).
     * Usually not null (e.g. in ConnId 1.x), but this can change in the future.
     *
     * We assume it's immutable, like a {@link String}, {@link Long}, and so on.
     *
     * The reason it is here is because of malformed objects ... TODO is this still true?
     *
     * See {@link UcfResourceObject#primaryIdentifierValue}.
     */
    final Object primaryIdentifierValue;

    ResourceObjectShadow(@NotNull ShadowType bean, Object primaryIdentifierValue) {
        this.bean = bean;
        this.primaryIdentifierValue = primaryIdentifierValue;
        this.checkConsistence();
    }

    /** To be used only by informed clients! */
    public static ResourceObjectShadow fromBean(
            @NotNull ShadowType bean,
            boolean exists,
            @NotNull ResourceObjectDefinition objectDefinition)
            throws SchemaException {
        bean.setExists(exists);
        return new ResourceObjectShadow(bean, ShadowUtil.getPrimaryIdentifierValue(bean, objectDefinition));
    }

    public static ResourceObjectShadow fromRepoShadow(RepoShadow repoShadow) throws SchemaException {
        // TODO what about the "exists" flag?
        return new ResourceObjectShadow(
                repoShadow.getBean(),
                repoShadow.getPrimaryIdentifierValueFromAttributes());
    }

    public @NotNull ShadowType getBean() {
        return bean;
    }

    public Object getPrimaryIdentifierValue() {
        return primaryIdentifierValue;
    }

    public @NotNull ResourceObjectShadow withNewContent(@NotNull ShadowType newData) {
        return new ResourceObjectShadow(
                newData,
                primaryIdentifierValue);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ResourceObjectShadow clone() {
        return new ResourceObjectShadow(
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
                this.getClass().getSimpleName() + " [primary id: " + primaryIdentifierValue + "]", indent);
        sb.append('\n');
        DebugUtil.debugDumpWithLabel(sb, "bean", bean, indent + 1);
        return sb.toString();
    }

    public @Nullable PrismProperty<?> getSingleValuedPrimaryIdentifier() {
        ShadowAttributesContainer attributesContainer = getAttributesContainer();
        PrismProperty<?> identifier = attributesContainer.getPrimaryIdentifier();
        if (identifier == null) {
            return null;
        }

        checkSingleIdentifierValue(identifier);
        return identifier;
    }

    private static void checkSingleIdentifierValue(PrismProperty<?> identifier) {
        int identifierCount = identifier.getValues().size();
        // Only one value is supported for an identifier
        if (identifierCount > 1) {
            // TODO: This should probably be switched to checked exception later
            throw new IllegalArgumentException("More than one identifier value is not supported");
        }
        if (identifierCount < 1) {
            // TODO: This should probably be switched to checked exception later
            throw new IllegalArgumentException("The identifier has no value");
        }
    }

    public @NotNull Shadow asShadow(@NotNull ResourceType resource) {
        return Shadow.of(bean, Resource.of(resource));
    }

    public static @Nullable ShadowType getBean(@Nullable ResourceObjectShadow resourceObject) {
        return resourceObject != null ? resourceObject.getBean() : null;
    }

    public static @Nullable PrismObject<ShadowType> getPrismObject(@Nullable ResourceObjectShadow resourceObject) {
        return resourceObject != null ? resourceObject.getPrismObject() : null;
    }

    public void setOid(String oid) {
        bean.setOid(oid);
    }

    void applyOperations(EntitlementConverter.SubjectOperations subjectOperations) throws SchemaException {
        for (var operation : subjectOperations.getOperations()) {
            applyDelta(operation.getItemDelta());
        }
    }
}
