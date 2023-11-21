/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * A resource object that is believed to exist (or very recently existed - in the case of `DELETE` change) on the resource.
 *
 * Properties:
 *
 * . it has a primary identifier value
 * . it has a definition, and that definition is correctly applied (this is from the super-class)
 * . the `exists` flag is correctly set (usually `true`; but can be `false` for objects related to `DELETE` changes)
 *
 * NOTE: In some cases, the object may be "shadowed". (TODO is that ok?)
 */
public class ExistingResourceObject extends ResourceObject {

    private ExistingResourceObject(
            @NotNull ShadowType bean, @NotNull ResourceObjectDefinition objectDefinition, Object primaryIdentifierValue) {
        super(bean, objectDefinition, primaryIdentifierValue);
        MiscUtil.stateNonNull(bean.isExists(), "The 'exists' flag is not present in %s", this);
    }

    /** To be used only by informed clients, e.g. the adoption methods in {@link ProvisioningContext}. */
    public static ExistingResourceObject of(
            @NotNull ShadowType bean,
            @NotNull ResourceObjectDefinition objectDefinition,
            Object primaryIdentifierValue) {
        return new ExistingResourceObject(bean, objectDefinition, primaryIdentifierValue);
    }

    /** TODO we should perhaps indicate that the source is repo! OR REMOVE THIS BRUTAL HACK SOMEHOW! */
    public static ExistingResourceObject fromRepoShadow(
            @NotNull RepoShadow repoShadow,
            Object primaryIdentifierValue) {
        return new ExistingResourceObject(repoShadow.getBean(), repoShadow.getObjectDefinition(), primaryIdentifierValue);
    }

    /** TODO we should perhaps indicate that the source is repo! OR REMOVE THIS BRUTAL HACK SOMEHOW! */
    public static ExistingResourceObject fromRepoShadow(
            @NotNull RepoShadow repoShadow) throws SchemaException {
        return new ExistingResourceObject(
                repoShadow.getBean(),
                repoShadow.getObjectDefinition(),
                repoShadow.getPrimaryIdentifierValueFromAttributes());
    }

    public @NotNull PrismProperty<?> getSingleValuedPrimaryIdentifier() {
        return Objects.requireNonNull(
                super.getSingleValuedPrimaryIdentifier(),
                () -> "No primary identifier value in " + this);
    }

    public @NotNull ExistingResourceObject minimize() {
        ShadowType minimizedBean = bean.clone();
        ShadowUtil.removeAllAttributesExceptPrimaryIdentifier(minimizedBean, objectDefinition);
        if (ShadowUtil.hasPrimaryIdentifier(minimizedBean, objectDefinition)) {
            return new ExistingResourceObject(minimizedBean, objectDefinition, primaryIdentifierValue);
        } else {
            throw new IllegalStateException("No primary identifier in " + this);
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ExistingResourceObject clone() {
        return new ExistingResourceObject(
                bean.clone(),
                objectDefinition,
                primaryIdentifierValue);
    }

    public @NotNull ExistingResourceObject updateWith(@NotNull ShadowType newData) {
        // TODO shouldn't we check the consistence of new data vs. old metadata?
        return new ExistingResourceObject(
                newData,
                objectDefinition,
                primaryIdentifierValue);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ExistingResourceObject[");
        sb.append(primaryIdentifierValue);
        sb.append(" (").append(objectDefinition.getShortIdentification()).append(") ");
        sb.append("@").append(getResourceOid());
        var shadowOid = bean.getOid();
        if (shadowOid != null) {
            sb.append(" OID:").append(shadowOid);
        }
        var doesExist = doesExist();
        if (!doesExist) {
            sb.append(", not existing");
        }
        var dead = isDead();
        if (dead) {
            sb.append(", dead");
        }
        sb.append("]");
        return sb.toString();
    }
}
