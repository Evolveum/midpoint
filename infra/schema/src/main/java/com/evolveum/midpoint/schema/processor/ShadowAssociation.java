/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.io.Serial;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.EqualsChecker;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.impl.PrismContainerImpl;
import com.evolveum.midpoint.prism.impl.PrismContainerValueImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;

/**
 * Represents an association between shadows: one subject and zero or more objects.
 *
 * There are two major types of associations:
 *
 * . "Simple" associations, which are represented by a single reference to the target shadow.
 * Typical case here is Active Directory style group membership, where each object can belong to zero or more groups,
 * and each membership value consists only of a reference to the particular group shadow.
 *
 * . "Rich" associations, which are represented by a complex structure containing additional information, like own attributes,
 * activation, and the like, in addition to reference(s) to objects. Examples are
 *
 * .. `ri:contract` for HR systems, binding persons with org units, cost centers, and probably other kinds of objects
 * (with a lot of attributes and the validity information);
 * .. `ri:access` for systems with rich access management information, binding users with objects requiring access control
 * (with, e.g., the access level attribute).
 *
 * NOTE: It is present only in model-level shadows; never propagated to resources nor stored in the repository.
 * The lower-level representation of the association is {@link ShadowReferenceAttribute}.
 */
public class ShadowAssociation
        extends PrismContainerImpl<ShadowAssociationValueType> implements Cloneable {

    @Serial private static final long serialVersionUID = 0L;

    private static final EqualsChecker<ShadowAssociation> SEMANTIC_EQUALS_CHECKER =
            (o1, o2) -> {
                if (o1 == null || o2 == null) {
                    return o1 == null && o2 == null;
                }
                return MiscUtil.unorderedCollectionEquals(
                        o1.getAssociationValues(),
                        o2.getAssociationValues(),
                        ShadowAssociationValue.semanticEqualsChecker());
            };

    private ShadowAssociation(@NotNull QName name, @NotNull ShadowAssociationDefinition definition) {
        super(name, definition);
    }

    public static ShadowAssociation empty(@NotNull ShadowAssociationDefinition definition) {
        return new ShadowAssociation(definition.getItemName(), definition);
    }

    public static ShadowAssociation empty(@NotNull QName name, @NotNull ShadowAssociationDefinition definition) {
        return new ShadowAssociation(name, definition);
    }

    public static EqualsChecker<ShadowAssociation> semanticEqualsChecker() {
        return SEMANTIC_EQUALS_CHECKER;
    }

    @Override
    public @NotNull ShadowAssociationDefinition getDefinition() {
        return stateNonNull(
                (ShadowAssociationDefinition) super.getDefinition(),
                "No definition in %s", this);
    }

    public @NotNull ShadowAssociationDefinition getDefinitionRequired() {
        return getDefinition(); // TODO inline
    }

    @Override
    public ShadowAssociation clone() {
        return (ShadowAssociation) super.clone();
    }

    @Override
    public ShadowAssociation cloneComplex(CloneStrategy strategy) {
        ShadowAssociation clone = new ShadowAssociation(getElementName(), getDefinition());
        copyValues(strategy, clone);
        return clone;
    }

    /**
     * This method will clone the item and convert it to a {@link ShadowAssociation}.
     * (A typical use case is that the provided item is not a {@link ShadowAssociation}.)
     */
    static ShadowAssociation convertFromPrismItem(
            @NotNull Item<?, ?> item, @NotNull ShadowAssociationDefinition associationDef) {
        var association = new ShadowAssociation(item.getElementName(), associationDef);
        for (PrismValue value : item.getValues()) {
            if (value instanceof PrismContainerValue<?> pcv) {
                try {
                    association.addIgnoringEquivalents(
                            ShadowAssociationValue.fromBean(
                                    ((ShadowAssociationValueType) pcv.asContainerable()).clone(),
                                    associationDef));
                } catch (SchemaException e) {
                    throw new IllegalArgumentException("Couldn't add PCV: " + value, e);
                }
            } else {
                throw new IllegalArgumentException("Not a PCV: " + value);
            }
        }
        return association;
    }

    @Override
    protected boolean addInternalExecution(@NotNull PrismContainerValue<ShadowAssociationValueType> newValue) {
        if (newValue instanceof ShadowAssociationValue) {
            return super.addInternalExecution(newValue);
        } else {
            // FIXME we should have resolved this (for deltas) in applyDefinition call, but that's not possible now
            try {
                return super.addInternalExecution(ShadowAssociationValue.fromBean(
                        newValue.asContainerable(),
                        getDefinitionRequired()));
            } catch (SchemaException e) {
                throw new RuntimeException(e); // FIXME
            }
        }
    }

    @Override
    public ShadowAssociationValue createNewValue() {
        // Casting is safe here, as "createNewValueInternal" provides this type.
        return (ShadowAssociationValue) super.createNewValue();
    }

    @Override
    protected @NotNull PrismContainerValueImpl<ShadowAssociationValueType> createNewValueInternal() {
        return ShadowAssociationValue.empty(getDefinitionRequired());
    }

    public int size() {
        return values.size();
    }

    @Override
    protected String getDebugDumpClassName() {
        return "SA";
    }

    public @NotNull List<ShadowAssociationValue> getAssociationValues() {
        // IDE accepts the version without cast to List, but the compiler doesn't.
        //noinspection unchecked,rawtypes,RedundantCast
        return List.copyOf(
                (List<? extends ShadowAssociationValue>) (List) getValues());
    }

//    @Override
//    public void addValueSkipUniquenessCheck(ShadowReferenceAttributeValue value) throws SchemaException {
//        addIgnoringEquivalents(value);
//    }

    public boolean hasNoValues() {
        return getValues().isEmpty();
    }
}
