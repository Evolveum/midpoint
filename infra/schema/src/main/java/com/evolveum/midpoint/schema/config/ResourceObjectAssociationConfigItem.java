/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.SimulatedAssociationClassParticipantDelineation;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT;

/**
 * Used for both association definitions and resource object construction with associations.
 *
 * FIXME split these two uses!
 */
public class ResourceObjectAssociationConfigItem
        extends ConfigurationItem<ResourceObjectAssociationType>
        implements AssociationConfigItem {

    @SuppressWarnings("unused") // called dynamically
    public ResourceObjectAssociationConfigItem(@NotNull ConfigurationItem<ResourceObjectAssociationType> original) {
        super(original);
    }

    public static ResourceObjectAssociationConfigItem of(
            @NotNull ResourceObjectAssociationType value, @NotNull ConfigurationItemOrigin origin) {
        return ConfigurationItem.of(value, origin)
                .as(ResourceObjectAssociationConfigItem.class);
    }

    public @NotNull ItemName getAssociationName() throws ConfigurationException {
        return singleNameRequired(value().getRef(), "association name (ref)");
    }

    public @NotNull ShadowKindType getKind() {
        return Objects.requireNonNullElse(
                value().getKind(), ShadowKindType.ENTITLEMENT);
    }

    /** The empty list is legal here. */
    public @NotNull List<String> getIntents() {
        return value().getIntent();
    }

    public boolean hasInbounds() {
        return !value().getInbound().isEmpty();
    }

    public @Nullable MappingConfigItem getOutboundMapping() {
        return child(
                value().getOutbound(),
                MappingConfigItem.class,
                ResourceObjectAssociationType.F_OUTBOUND);
    }

    public @NotNull MappingConfigItem getOutboundMappingRequired() throws ConfigurationException {
        return nonNull(getOutboundMapping(), "outbound mapping");
    }

    @Override
    public @NotNull List<InboundMappingConfigItem> getInboundMappings() {
        return children(
                value().getInbound(),
                InboundMappingConfigItem.class,
                ResourceObjectAssociationType.F_INBOUND);
    }

    public @NotNull ItemName getItemName() throws ConfigurationException {
        return singleNameRequired(value().getRef(), "association name (ref)");
    }

    public boolean isExclusiveStrong() {
        return Boolean.TRUE.equals(value().isExclusiveStrong());
    }

    @Override
    public boolean isDeprecated() throws ConfigurationException {
        return Boolean.TRUE.equals(value().isDeprecated());
    }

    @Override
    public DisplayHintType getDisplayHint() {
        return value().getDisplayHint();
    }

    public @NotNull AttributeBinding getPrimaryAttributeBinding() throws ConfigurationException {
        QName valueAttribute = nonNull(value().getValueAttribute(), "value attribute");
        QName associationAttribute = nonNull(value().getAssociationAttribute(), "association attribute");
        if (getDirection() == OBJECT_TO_SUBJECT) {
            return new AttributeBinding(valueAttribute, associationAttribute);
        } else {
            return new AttributeBinding(associationAttribute, valueAttribute);
        }
    }

    public @Nullable AttributeBinding getSecondaryAttributeBinding() throws ConfigurationException {
        var shortcutValueAttribute = value().getShortcutValueAttribute(); // for o->s this is e.g. ri:dn (on group)
        var shortcutAssociationAttribute = value().getShortcutAssociationAttribute(); // for o->s: e.g. ri:memberOf (on account)
        if (shortcutValueAttribute != null && shortcutAssociationAttribute != null) {
            if (getDirection() == OBJECT_TO_SUBJECT) {
                return new AttributeBinding(shortcutAssociationAttribute, shortcutValueAttribute);
            } else {
                return new AttributeBinding(shortcutValueAttribute, shortcutAssociationAttribute);
            }
        } else if (shortcutValueAttribute == null && shortcutAssociationAttribute == null) {
            return null;
        } else {
            throw configException("Shortcut attribute is defined only on one side; in %s", DESC);
        }
    }

    public @NotNull ResourceObjectAssociationDirectionType getDirection() throws ConfigurationException {
        return nonNull(value().getDirection(), "association direction");
    }

    @Override
    public boolean isTolerant() {
        return BooleanUtils.isNotFalse(value().isTolerant());
    }

    @Override
    public @NotNull List<String> getTolerantValuePatterns() {
        return value().getTolerantValuePattern();
    }

    @Override
    public @NotNull List<String> getIntolerantValuePatterns() {
        return value().getIntolerantValuePattern();
    }

    @Override
    public String getDisplayName() {
        return value().getDisplayName();
    }

    @Override
    public Integer getDisplayOrder() {
        return value().getDisplayOrder();
    }

    @Override
    public String getHelp() {
        return value().getHelp();
    }

    @Override
    public String getDocumentation() {
        return value().getDocumentation();
    }

    @Override
    public String getLifecycleState() {
        return value().getLifecycleState();
    }

    @Override
    public String debugDump(int indent) {
        return value().debugDump(indent);
    }

    public boolean isExplicitReferentialIntegrity() {
        return BooleanUtils.isNotFalse(value().isExplicitReferentialIntegrity());
    }

    public QName getMatchingRule() {
        return value().getMatchingRule();
    }

    /** Always non-empty. */
    public Collection<SimulatedAssociationClassParticipantDelineation> getSubjectDelineations(
            @NotNull ResourceObjectTypeDefinition referentialSubjectDefinition) {
        return List.of(
                SimulatedAssociationClassParticipantDelineation.fromObjectTypeDefinition(
                        referentialSubjectDefinition,
                        value().getAuxiliaryObjectClass()));
    }

    /** Always non-empty. */
    public Collection<SimulatedAssociationClassParticipantDelineation> getObjectDelineations(
            @NotNull CompleteResourceSchema resourceSchema, @NotNull ResourceObjectTypeDefinition referentialObjectDefinition)
            throws ConfigurationException {
        // There must be at least one intent. If none is provided, we derive it from the object definition.
        var configuredIntents = value().getIntent();
        var realIntents = configuredIntents.isEmpty() ? List.of(referentialObjectDefinition.getIntent()) : configuredIntents;

        List<SimulatedAssociationClassParticipantDelineation> delineations = new ArrayList<>();
        for (String intent : realIntents) {
            var typeDef = configNonNull(
                    resourceSchema.getObjectTypeDefinition(getKind(), intent),
            "No object type definition for kind %s and intent %s in %s", getKind(), intent, DESC);
            delineations.add(
                    new SimulatedAssociationClassParticipantDelineation(
                            typeDef.getTypeName(),
                            typeDef.getDelineation().getBaseContext(),
                            typeDef.getDelineation().getSearchHierarchyScope(),
                            typeDef,
                            null));
        }
        return delineations;
    }

    @Override
    public @NotNull String localDescription() {
        return "resource association definition for '" + value().getRef() + "'";
    }
}
