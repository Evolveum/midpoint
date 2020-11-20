/*
 * Copyright (C) 2014-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.refinery;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.AbstractFreezable;
import com.evolveum.midpoint.prism.Freezable;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RefinedAssociationDefinition extends AbstractFreezable
        implements Serializable, Visitable, Freezable {

    private static final long serialVersionUID = 1L;

    private final ResourceObjectAssociationType resourceObjectAssociationType;
    private RefinedObjectClassDefinition associationTarget;

    public RefinedAssociationDefinition(ResourceObjectAssociationType resourceObjectAssociationType) {
        super();
        this.resourceObjectAssociationType = resourceObjectAssociationType;
    }

    public ResourceObjectAssociationType getResourceObjectAssociationType() {
        return resourceObjectAssociationType;
    }

    public RefinedObjectClassDefinition getAssociationTarget() {
        return associationTarget;
    }

    void setAssociationTarget(RefinedObjectClassDefinition associationTarget) {
        checkMutable();
        this.associationTarget = associationTarget;
    }

    public ItemName getName() {
        return ItemPathTypeUtil.asSingleNameOrFail(resourceObjectAssociationType.getRef());
    }

    public ShadowKindType getKind() {
        return resourceObjectAssociationType.getKind();
    }

    public Collection<String> getIntents() {
        return resourceObjectAssociationType.getIntent();
    }

    public QName getAuxiliaryObjectClass() {
        return resourceObjectAssociationType.getAuxiliaryObjectClass();
    }

    public MappingType getOutboundMappingType() {
        return resourceObjectAssociationType.getOutbound();
    }

    public List<MappingType> getInboundMappingTypes() {
        return resourceObjectAssociationType.getInbound();
    }

    public boolean isExclusiveStrong() {
        return BooleanUtils.isTrue(resourceObjectAssociationType.isExclusiveStrong());
    }

    public boolean isIgnored() {
        return false;           // todo implement!
    }

    public boolean isIgnored(LayerType layer) {
        QName name = getAssociationAttribute();
        RefinedAttributeDefinition<?> associationAttributeDef = associationTarget.findAttributeDefinition(name);
        if (associationAttributeDef == null) {
            throw new IllegalStateException("No such attribute :" + name
                    + " in kind: " + associationTarget.getKind() + ", intent: " + associationTarget.getIntent()
                    + " as defined for association: " + resourceObjectAssociationType.getDisplayName());
        }

        return associationAttributeDef.isIgnored(layer);
    }

    private QName getAssociationAttribute() {
        ResourceObjectAssociationDirectionType direction = resourceObjectAssociationType.getDirection();
        if (ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT.equals(direction)) {
            return resourceObjectAssociationType.getAssociationAttribute();
        }

        return resourceObjectAssociationType.getValueAttribute();
    }

    public PropertyLimitations getLimitations(LayerType layer) {
        QName name = getAssociationAttribute();
        RefinedAttributeDefinition<?> associationAttributeDef = associationTarget.findAttributeDefinition(name);
        if (associationAttributeDef == null) {
            throw new IllegalStateException("No such attribute :" + name
                    + " in kind: " + associationTarget.getKind() + ", intent: " + associationTarget.getIntent()
                    + " as defined for association: " + resourceObjectAssociationType.getDisplayName());
        }

        return associationAttributeDef.getLimitations(layer);
    }

    public boolean isTolerant() {
        return BooleanUtils.isNotFalse(resourceObjectAssociationType.isTolerant());
    }

    @NotNull
    public List<String> getTolerantValuePattern() {
        return resourceObjectAssociationType.getTolerantValuePattern();
    }

    @NotNull
    public List<String> getIntolerantValuePattern() {
        return resourceObjectAssociationType.getIntolerantValuePattern();
    }

    public boolean requiresExplicitReferentialIntegrity() {
        return !BooleanUtils.isFalse(getResourceObjectAssociationType().isExplicitReferentialIntegrity());    // because default is TRUE
    }

    public QName getMatchingRule() {
        return getResourceObjectAssociationType().getMatchingRule();
    }

    public String getDisplayName() {
        return resourceObjectAssociationType.getDisplayName();
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public RefinedAssociationDefinition clone() {
        RefinedAssociationDefinition clone = new RefinedAssociationDefinition(resourceObjectAssociationType);
        copyValues(clone);
        return clone;
    }

    private void copyValues(RefinedAssociationDefinition clone) {
        clone.associationTarget = this.associationTarget;
    }
}
