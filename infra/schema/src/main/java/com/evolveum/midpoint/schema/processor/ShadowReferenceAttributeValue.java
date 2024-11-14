/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serial;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.impl.PrismReferenceValueImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.EqualsChecker;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * Represents a value of a {@link ShadowReferenceAttribute}.
 * For example, a single group membership for a given account: `joe` is a member of `admins`.
 *
 * NOTE: As an experiment, we try to keep instances as consistent as possible.
 * Any places where this is checked, will throw {@link IllegalStateException} instead of {@link SchemaException}.
 * We will simply not allow creating a non-compliant reference attribute value. At least we'll try to do this.
 * The exception are situations where the object exists between instantiation and providing the data.
 */
public class ShadowReferenceAttributeValue extends PrismReferenceValueImpl {

    @Serial private static final long serialVersionUID = 0L;

    private static final EqualsChecker<ShadowReferenceAttributeValue> SEMANTIC_EQUALS_CHECKER =
            (o1, o2) -> {
                if (o1 == null || o2 == null) {
                    return o1 == null && o2 == null;
                }

                var oid1 = o1.getOid();
                var oid2 = o2.getOid();

                // Normally, we compare association values by OID. This works for pre-existing target shadows.
                if (oid1 != null && oid2 != null) {
                    return oid1.equals(oid2);
                }

                // However, (some of) these shadows can be newly created. So we have to compare the attributes.
                // (Comparing the whole shadows can be problematic, as there may be lots of generated data, not marked
                // as operational.)
                var s1 = o1.getShadowIfPresent();
                var s2 = o2.getShadowIfPresent();

                if (s1 == null || s2 == null) {
                    return s1 == null && s2 == null; // Actually we cannot do any better here.
                }

                return s1.equalsByContent(s2);
            };

    /** Whether this is really the full object obtained from the resource. For internal provisioning module use only! */
    private transient boolean fullObject;

    private ShadowReferenceAttributeValue() {
        this(null, null);
    }

    private ShadowReferenceAttributeValue(OriginType type, Objectable source) {
        super(null, type, source);
    }

    private ShadowReferenceAttributeValue(String oid, OriginType type, Objectable source) {
        super(oid, type, source);
    }

    /**
     * Converts (potentially raw) {@link PrismReferenceValue} to wrapped {@link ShadowReferenceAttributeValue}.
     *
     * We should not use the original value any more, e.g. because of values being copied.
     */
    public static @NotNull ShadowReferenceAttributeValue fromRefValue(@NotNull PrismReferenceValue refVal)
            throws SchemaException {
        if (refVal instanceof ShadowReferenceAttributeValue shadowReferenceAttributeValue) {
            return shadowReferenceAttributeValue;
        }

        var newVal = new ShadowReferenceAttributeValue(refVal.getOid(), refVal.getOriginType(), refVal.getOriginObject());
        newVal.setTargetType(refVal.getTargetType());
        var shadow = (ShadowType) refVal.getObjectable();
        if (shadow != null) {
            if (ShadowUtil.isRaw(shadow)) {
                var definition = SchemaService.get().resourceSchemaRegistry().getDefinitionForShadow(shadow);
                if (definition != null) {
                    ShadowDefinitionApplicator.strict(definition)
                            .applyToShadow(shadow);
                }
            }
            newVal.setObject(shadow.asPrismObject());
        }
        return newVal;
    }

    public static @NotNull ShadowReferenceAttributeValue fromReferencable(@NotNull Referencable referencable) throws SchemaException {
        return fromRefValue(
                referencable.asReferenceValue());
    }

    /** Creates a new value from the full or ID-only shadow. No cloning here. */
    public static @NotNull ShadowReferenceAttributeValue fromShadow(@NotNull AbstractShadow shadow, boolean full) throws SchemaException {
        var value = fromReferencable(
                ObjectTypeUtil.createObjectRefWithFullObject(shadow.getPrismObject()));
        value.fullObject = full;
        return value;
    }

    public static @NotNull ShadowReferenceAttributeValue fromShadowOid(@NotNull String oid) throws SchemaException {
        return fromReferencable(
                ObjectTypeUtil.createObjectRef(oid, ObjectTypes.SHADOW));
    }

    /** TODO better name */
    public static @NotNull EqualsChecker<ShadowReferenceAttributeValue> semanticEqualsChecker() {
        return SEMANTIC_EQUALS_CHECKER;
    }

    public boolean isFullObject() {
        return fullObject;
    }

    public void setFullObject(boolean fullObject) {
        this.fullObject = fullObject;
    }

    public static ShadowReferenceAttributeValue empty() {
        return new ShadowReferenceAttributeValue();
    }

    @Override
    public ShadowReferenceAttributeValue clone() {
        return (ShadowReferenceAttributeValue) super.clone();
    }

    @Override
    public ShadowReferenceAttributeValue cloneComplex(CloneStrategy strategy) {
        var clone = new ShadowReferenceAttributeValue(getOid(), getOriginType(), getOriginObject());
        copyValues(strategy, clone);
        return clone;
    }

    protected void copyValues(CloneStrategy strategy, ShadowReferenceAttributeValue clone) {
        super.copyValues(strategy, clone);
        clone.fullObject = fullObject;
    }

    public @NotNull ShadowAttributesContainer getAttributesContainerRequired() {
        return ShadowUtil.getAttributesContainerRequired(getShadowBean());
    }

    /** Target object or its reference. TODO better name. */
    public @NotNull ShadowType getShadowBean() {
        return (ShadowType) stateNonNull(getObjectable(), "No shadow in %s", this);
    }

    public @NotNull AbstractShadow getShadow() {
        return AbstractShadow.of(getShadowBean());
    }

    public QName getTargetObjectClassName() {
        return getAttributesContainerRequired().getDefinitionRequired().getTypeName();
    }

    public @NotNull ShadowReferenceAttributeDefinition getDefinitionRequired() {
        return stateNonNull((ShadowReferenceAttributeDefinition) getDefinition(), "No definition in %s", this);
    }

    public @NotNull ResourceObjectIdentification<?> getIdentification() {
        return ResourceObjectIdentification.fromAttributes(
                getTargetObjectDefinition(), getShadowRequired().getSimpleAttributes());
    }

    public @NotNull ObjectReferenceType asObjectReferenceType() {
        var ref = asReferencable();
        if (ref instanceof ObjectReferenceType ort) {
            return ort;
        } else {
            var ort = new ObjectReferenceType();
            ort.setupReferenceValue(this);
            return ort;
        }
    }

    public @Nullable AbstractShadow getShadowIfPresent() {
        var shadow = asObjectReferenceType().getObjectable();
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

    public @Nullable ResourceObjectDefinition getAssociatedObjectDefinitionIfPresent() {
        var shadow = getShadowIfPresent();
        return shadow != null ? shadow.getObjectDefinition() : null;
    }

    public @NotNull ResourceObjectDefinition getTargetObjectDefinition() {
        return getShadowRequired().getObjectDefinition();
    }

    public ShadowAttributesContainer getAttributesContainerIfPresent() {
        var shadow = getShadowIfPresent();
        return shadow != null ? shadow.getAttributesContainer() : null;
    }

    public void setShadow(@NotNull AbstractShadow shadow) {
        setObject(shadow.getPrismObject());
        // TODO check consistence
    }

//    protected boolean appendExtraHeaderDump(StringBuilder sb, int indent, boolean wasIndent) {
//        wasIndent = super.appendExtraHeaderDump(sb, indent, wasIndent);
//        if (!wasIndent) {
//            DebugUtil.indentDebugDump(sb, indent);
//        } else {
//            sb.append("; ");
//        }
//        // TODO this should be a part of ShadowType dumping; but that code is automatically generated for now
//        sb.append(getAssociatedObjectDefinitionIfPresent());
//        return true;
//    }

    public boolean matches(ShadowReferenceAttributeValue other) {
        return semanticEqualsChecker().test(this, other);
    }

    public @NotNull PrismPropertyValue<?> getSingleIdentifierValueRequired(@NotNull QName attrName, Object errorCtx)
            throws SchemaException {
        ShadowAttributesContainer attributesContainer =
                MiscUtil.requireNonNull(
                        getAttributesContainerIfPresent(),
                        "No attributes container in %s in %s", this, errorCtx);

        PrismProperty<?> valueAttr = attributesContainer.findProperty(ItemName.fromQName(attrName));
        if (valueAttr == null || valueAttr.isEmpty()) {
            throw new SchemaException(
                    "No value of attribute %s present in %s in %s".formatted(
                            attrName, this, errorCtx));
        } else if (valueAttr.size() > 1) {
            throw new SchemaException(
                    "Multiple values of attribute %s present in %s in %s: %s".formatted(
                            attrName, this, errorCtx, valueAttr.getValues()));
        } else {
            return valueAttr.getValue();
        }
    }

    public @NotNull String getOidRequired() {
        return stateNonNull(getOid(), "No OID in %s", this);
    }

    @Override
    public String toString() {
        return "SRAV: " + super.toString() + (fullObject ? " (full)" : "");
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(this); // FIXME
    }

    @Override
    public String toHumanReadableString() {
        StringBuilder sb = new StringBuilder();
        shortDump(sb); // FIXME
        return sb.toString();
    }
}
