/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serial;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismContainerValueImpl;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * Represents a specific shadow association value - i.e. something that is put into {@link ShadowAssociation}.
 * For example, a single group membership for a given account: `joe` is a member of `admins`.
 *
 * NOTE: As an experiment, we try to keep instances as consistent as possible. E.g., we require correct `shadowRef` etc.
 * Any places where this is checked, will throw {@link IllegalStateException} instead of {@link SchemaException}.
 * We will simply not allow creating a non-compliant association object. At least we'll try to do this.
 * The exception are situations where the object exists between instantiation and providing the data.
 *
 * *Instantiation*
 *
 * In particular, we must provide reasonable CTD when instantiating this object.
 * Otherwise, {@link PrismContainerValue#asContainerable()} will fail.
 *
 * TODO check if it's possible to implement this approach regarding createNewValue in ShadowAssociation
 */
@Experimental
public class ShadowAssociationValue extends PrismContainerValueImpl<ShadowAssociationValueType> {

    @Serial private static final long serialVersionUID = 0L;

    private ShadowAssociationValue() {
        this(null, null, null, null,
                stateNonNull(
                        PrismContext.get().getSchemaRegistry()
                                .findComplexTypeDefinitionByCompileTimeClass(ShadowAssociationValueType.class),
                        "No CTD for ShadowAssociationValueType"));
    }

    private ShadowAssociationValue(
            OriginType type, Objectable source,
            PrismContainerable<?> container, Long id,
            ComplexTypeDefinition complexTypeDefinition) {
        super(type, source, container, id, complexTypeDefinition);
    }

    /**
     * Converts association value bean to wrapped {@link ShadowAssociationValue} basically by cloning its content
     * and selected properties (e.g., parent and ID).
     *
     * We should not use the original value any more, e.g. because of the copied "parent" value.
     */
    public static @NotNull ShadowAssociationValue of(@NotNull ShadowAssociationValueType bean) {
        PrismContainerValue<?> pcv = bean.asPrismContainerValue();
        var newValue = new ShadowAssociationValue();
        try {
            newValue.addAll(
                    CloneUtil.cloneCollectionMembers(
                            pcv.getItems()));
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when transferring association value items to a SAV");
        }
        newValue.setParent(pcv.getParent()); // TODO maybe temporary?
        newValue.setId(pcv.getId());

        return newValue;
    }

    /** Creates a new value from the full or ID-only shadow. No cloning here. */
    public static @NotNull ShadowAssociationValue of(@NotNull AbstractShadow shadow, boolean identifiersOnly) {
        var newValue = empty();
        var shadowRef = PrismContext.get().getSchemaRegistry()
                .findComplexTypeDefinitionByCompileTimeClass(ShadowAssociationValueType.class)
                .findReferenceDefinition(ShadowAssociationValueType.F_SHADOW_REF)
                .instantiate();
        try {
            shadowRef.add(
                    shadow.getRefWithEmbeddedObject().asReferenceValue());
            newValue.add(shadowRef);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when adding shadowRef to a SAV");
        }
        newValue.setIdentifiersOnly(identifiersOnly);
        return newValue;
    }

    public void setIdentifiersOnly(boolean value) {
        asContainerable().setIdentifiersOnly(value);
    }

    public boolean hasIdentifiersOnly() {
        return Boolean.TRUE.equals(asContainerable().isIdentifiersOnly());
    }

    public boolean hasFullObject() {
        return !hasIdentifiersOnly();
    }

    public static ShadowAssociationValue empty() {
        return new ShadowAssociationValue();
    }

    public static ShadowAssociationValue withReferenceTo(@NotNull String targetOid) {
        return of(
                new ShadowAssociationValueType()
                        .shadowRef(targetOid, ShadowType.COMPLEX_TYPE));
    }

    @Override
    public ShadowAssociationValue cloneComplex(CloneStrategy strategy) {
        ShadowAssociationValue clone = new ShadowAssociationValue(
                getOriginType(), getOriginObject(), getParent(), null, this.complexTypeDefinition);
        copyValues(strategy, clone);
        return clone;
    }

    public @NotNull ResourceAttributeContainer getAttributesContainerRequired() {
        return ShadowUtil.getAttributesContainerRequired(getShadowBean());
    }

    public @NotNull ShadowAssociationsContainer getAssociationsContainer() {
        return ShadowUtil.getAssociationsContainerRequired(getShadowBean());
    }

    /** Target object or its reference. TODO better name. */
    public @NotNull ShadowType getShadowBean() {
        var shadowRef = stateNonNull(asContainerable().getShadowRef(), "No shadowRef in %s", this);
        return (ShadowType) stateNonNull(shadowRef.getObjectable(), "No shadow in %s", this);
    }

    public @NotNull AbstractShadow getShadow() {
        return AbstractShadow.of(getShadowBean());
    }

    public QName getTargetObjectClassName() {
        return getAttributesContainerRequired().getDefinitionRequired().getTypeName();
    }

    /** Returns the identifiers of the referenced object. */
    public @NotNull ResourceAttributeContainer getIdentifiersContainerRequired() {
        return getAttributesContainerRequired();
    }

    public @NotNull ShadowAssociationClassDefinition getAssociationClassDefinition() {
        return stateNonNull((ShadowAssociationDefinition) getDefinition(), "No definition in %s", this)
                .getAssociationClassDefinition();
    }

    public @NotNull ResourceObjectIdentification<?> getIdentification() {
        return ResourceObjectIdentification.fromAttributes(
                getAssociatedObjectDefinition(), getShadowRequired().getAttributes());
    }

    public @NotNull ObjectReferenceType getShadowRef() {
        return stateNonNull(
                asContainerable().getShadowRef(),
                "No shadowRef in %s", this);
    }

    public @Nullable AbstractShadow getShadowIfPresent() {
        var shadow = getShadowRef().getObjectable();
        if (shadow != null) {
            stateCheck(shadow instanceof ShadowType, "Not a shadow in %s: %s", this, shadow);
            return AbstractShadow.of((ShadowType) shadow);
        } else {
            return null;
        }
    }

    public @NotNull AbstractShadow getShadowRequired() {
        return stateNonNull(getShadowIfPresent(), "No shadow in %s", this);
    }

    public @NotNull ResourceObjectDefinition getAssociatedObjectDefinition() {
        return getShadowRequired().getObjectDefinition();
    }

    public ResourceAttributeContainer getAttributesContainerIfPresent() {
        var shadow = getShadowIfPresent();
        return shadow != null ? shadow.getAttributesContainer() : null;
    }

    public void setShadow(@NotNull AbstractShadow shadow) {
        asContainerable()
                .shadowRef(ObjectTypeUtil.createObjectRefWithFullObject(shadow.getBean()))
                .identifiersOnly(false);
        // TODO check consistence
    }

    @Override
    protected boolean appendExtraHeaderDump(StringBuilder sb, int indent, boolean wasIndent) {
        wasIndent = super.appendExtraHeaderDump(sb, indent, wasIndent);
        if (!wasIndent) {
            DebugUtil.indentDebugDump(sb, indent);
        } else {
            sb.append("; ");
        }
        // TODO this should be a part of ShadowType dumping; but that code is automatically generated for now
        sb.append(getAssociatedObjectDefinition());
        return true;
    }
}
