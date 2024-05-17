/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.ucf.api.UcfResourceObject;
import com.evolveum.midpoint.provisioning.util.ErrorState;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
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
 *
 * @see #checkConsistence()
 */
public class ExistingResourceObject extends ResourceObject {

    @NotNull private final ErrorState errorState;

    private ExistingResourceObject(
            @NotNull ShadowType bean,
            @NotNull Object primaryIdentifierValue,
            @NotNull ErrorState errorState) {
        super(bean, primaryIdentifierValue);
        this.errorState = errorState;
    }

    /** To be used only by informed clients. */
    public static ExistingResourceObject of(
            @NotNull ShadowType bean,
            @NotNull Object primaryIdentifierValue,
            @NotNull ErrorState errorState) {
        return new ExistingResourceObject(bean, primaryIdentifierValue, errorState);
    }

    static ExistingResourceObject fromUcf(
            @NotNull UcfResourceObject ucfResourceObject, @NotNull ObjectReferenceType resourceRef) {
        return fromUcf(ucfResourceObject, resourceRef, true);
    }

    static ExistingResourceObject fromUcf(
            @NotNull UcfResourceObject ucfResourceObject, @NotNull ObjectReferenceType resourceRef, boolean exists) {
        ShadowType bean = ucfResourceObject.getBean();
        putRequiredInformationToShadows(bean, resourceRef, exists);
        return new ExistingResourceObject(
                bean,
                ucfResourceObject.getPrimaryIdentifierValue(),
                ErrorState.fromUcfErrorState(ucfResourceObject.getErrorState()));
    }

    /**
     * Updates the shadow and all embedded associated objects with resource reference and exists flag.
     *
     * TODO Currently we assume that all embedded objects live and die with the main shadow. So their "exists" flag is the same
     *  as the main shadow's "exists". This may change in the future when we will be able to e.g. fetch the associated groups
     *  explicitly.
     */
    private static void putRequiredInformationToShadows(
            @NotNull ShadowType shadowBean, @NotNull ObjectReferenceType resourceRef, boolean exists) {
        shadowBean.setResourceRef(resourceRef);
        shadowBean.setExists(exists);
        for (var value : ShadowUtil.getAssociationsCollection(shadowBean).getAllAssociationValues()) {
            if (value.hasFullObject()) {
                putRequiredInformationToShadows(value.getShadowBean(), resourceRef, exists);
            }
        }
    }

    /** TODO we should perhaps indicate that the source is repo! OR REMOVE THIS BRUTAL HACK SOMEHOW! */
    public static ExistingResourceObject fromRepoShadow(
            @NotNull RepoShadow repoShadow,
            Object primaryIdentifierValue) {
        return new ExistingResourceObject(repoShadow.getBean(), primaryIdentifierValue, ErrorState.ok());
    }

    /** TODO we should perhaps indicate that the source is repo! OR REMOVE THIS BRUTAL HACK SOMEHOW! */
    public static ExistingResourceObject fromRepoShadow(
            @NotNull RepoShadow repoShadow) throws SchemaException {
        return new ExistingResourceObject(
                repoShadow.getBean(),
                Objects.requireNonNull(repoShadow.getPrimaryIdentifierValueFromAttributes()),
                ErrorState.ok());
    }

    /** Only for informed clients! */
    public static ExistingResourceObject fromShadow(@NotNull AbstractShadow shadow) {
        return new ExistingResourceObject(
                shadow.getBean(),
                Objects.requireNonNull(shadow.getPrimaryIdentifierAttributeRequired().getRealValue()),
                ErrorState.ok());
    }


    public @NotNull ErrorState getErrorState() {
        return errorState;
    }

    public boolean isError() {
        return errorState.isError();
    }

    public @NotNull PrismProperty<?> getSingleValuedPrimaryIdentifier() {
        return Objects.requireNonNull(
                super.getSingleValuedPrimaryIdentifier(),
                () -> "No primary identifier value in " + this);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ExistingResourceObject clone() {
        return new ExistingResourceObject(bean.clone(), primaryIdentifierValue, errorState);
    }

    public @NotNull ExistingResourceObject withNewContent(@NotNull ShadowType newData) {
        // TODO shouldn't we check the consistence of new data vs. old metadata?
        return new ExistingResourceObject(newData, primaryIdentifierValue, errorState);
    }

    /** For creating shadows in emergency situations. */
    public @NotNull ExistingResourceObject withIdentifiersOnly() {
        var clone = clone();
        clone.removeAttributesExcept(getObjectDefinition().getAllIdentifiersNames());
        return clone;
    }

    /** For creating shadows in ultra emergency situations. */
    public @NotNull ExistingResourceObject withPrimaryIdentifierOnly() {
        var clone = clone();
        clone.removeAttributesExcept(getObjectDefinition().getPrimaryIdentifiersNames());
        return clone;
    }

    private void removeAttributesExcept(Collection<? extends QName> attributesToKeep) {
        for (ShadowSimpleAttribute<?> attribute : List.copyOf(getAttributesContainer().getAttributes())) {
            if (!QNameUtil.matchAny(attribute.getElementName(), attributesToKeep)) {
                getAttributesContainer().remove(attribute);
            }
        }
    }

    @Override
    public @NotNull ResourceObjectIdentification.WithPrimary getPrimaryIdentification() throws SchemaException {
        return Objects.requireNonNull(super.getPrimaryIdentification());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ExistingResourceObject[id: ");
        sb.append(primaryIdentifierValue);
        // TODO what if the getObjectDefinition itself throws an exception?
        sb.append(" (").append(getObjectDefinition().getShortIdentification()).append(") ");
        sb.append("@").append(getResourceOidRequired());
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

    @Override
    public void checkConsistence() {
        super.checkConsistence();
        stateNonNull(bean.isExists(), "The 'exists' flag is not present in %s", this);
    }
}
