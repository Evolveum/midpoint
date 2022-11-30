/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.AbstractFreezable;
import com.evolveum.midpoint.prism.Freezable;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;

public class ResourceAssociationDefinition extends AbstractFreezable
        implements Serializable, Visitable, Freezable, DebugDumpable {

    private static final long serialVersionUID = 1L;

    @NotNull private final ResourceObjectAssociationType definitionBean;

    private ResourceObjectTypeDefinition associationTarget;

    public ResourceAssociationDefinition(@NotNull ResourceObjectAssociationType definitionBean) {
        this.definitionBean = definitionBean;
    }

    public @NotNull ResourceObjectAssociationType getDefinitionBean() {
        return definitionBean;
    }

    public ResourceObjectTypeDefinition getAssociationTarget() {
        return associationTarget;
    }

    void setAssociationTarget(ResourceObjectTypeDefinition associationTarget) {
        checkMutable();
        this.associationTarget = associationTarget;
    }

    public @NotNull ItemName getName() {
        return ItemPathTypeUtil.asSingleNameOrFail(definitionBean.getRef());
    }

    public @NotNull ShadowKindType getKind() {
        return Objects.requireNonNullElse(
                definitionBean.getKind(), ShadowKindType.ENTITLEMENT);
    }

    public @NotNull ResourceObjectAssociationDirectionType getDirection() {
        return Objects.requireNonNull(
                definitionBean.getDirection(),
                () -> "No association direction provided in association definition: " + this);
    }

    public @NotNull Collection<String> getIntents() {
        return definitionBean.getIntent();
    }

    /** We rely on the assumptions about multiple intents described for {@link ResourceObjectAssociationType#getIntent()}. */
    public @NotNull String getAnyIntent() throws ConfigurationException {
        Collection<String> intents = getIntents();
        configCheck(!intents.isEmpty(), "No intent(s) defined for %s", this);
        return intents.iterator().next();
    }

    public QName getAuxiliaryObjectClass() {
        return definitionBean.getAuxiliaryObjectClass();
    }

    public MappingType getOutboundMappingType() {
        return definitionBean.getOutbound();
    }

    public List<InboundMappingType> getInboundMappingTypes() {
        return definitionBean.getInbound();
    }

    public boolean isExclusiveStrong() {
        return BooleanUtils.isTrue(definitionBean.isExclusiveStrong());
    }

    public boolean isIgnored() {
        return false;           // todo implement!
    }

    public boolean isIgnored(LayerType layer) {
        QName name = getAssociationAttribute();
        ResourceAttributeDefinition<?> associationAttributeDef = associationTarget.findAttributeDefinition(name);
        if (associationAttributeDef == null) {
            throw new IllegalStateException("No such attribute :" + name
                    + " in kind: " + associationTarget.getKind() + ", intent: " + associationTarget.getIntent()
                    + " as defined for association: " + definitionBean.getDisplayName());
        }

        return associationAttributeDef.isIgnored(layer);
    }

    private QName getAssociationAttribute() {
        ResourceObjectAssociationDirectionType direction = definitionBean.getDirection();
        if (ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT.equals(direction)) {
            return definitionBean.getAssociationAttribute();
        }

        return definitionBean.getValueAttribute();
    }

    public PropertyLimitations getLimitations(LayerType layer) {
        QName name = getAssociationAttribute();
        ResourceAttributeDefinition<?> associationAttributeDef = associationTarget.findAttributeDefinition(name);
        if (associationAttributeDef == null) {
            throw new IllegalStateException("No such attribute :" + name
                    + " in kind: " + associationTarget.getKind() + ", intent: " + associationTarget.getIntent()
                    + " as defined for association: " + definitionBean.getDisplayName());
        }

        return associationAttributeDef.getLimitations(layer);
    }

    public boolean isTolerant() {
        return BooleanUtils.isNotFalse(definitionBean.isTolerant());
    }

    @NotNull
    public List<String> getTolerantValuePattern() {
        return definitionBean.getTolerantValuePattern();
    }

    @NotNull
    public List<String> getIntolerantValuePattern() {
        return definitionBean.getIntolerantValuePattern();
    }

    public boolean requiresExplicitReferentialIntegrity() {
        return !BooleanUtils.isFalse(getDefinitionBean().isExplicitReferentialIntegrity()); // because default is TRUE
    }

    public QName getMatchingRule() {
        return getDefinitionBean().getMatchingRule();
    }

    public String getDisplayName() {
        return definitionBean.getDisplayName();
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public ResourceAssociationDefinition clone() {
        ResourceAssociationDefinition clone = new ResourceAssociationDefinition(definitionBean);
        copyValues(clone);
        return clone;
    }

    private void copyValues(ResourceAssociationDefinition clone) {
        clone.associationTarget = this.associationTarget;
    }

    @Override
    public String toString() {
        return "ResourceAssociationDefinition{" +
                "ref=" + definitionBean.getRef() +
                ", associationTarget=" + associationTarget +
                "}";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "definition", definitionBean, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "associationTarget", String.valueOf(associationTarget), indent + 1);
        return sb.toString();
    }
}
