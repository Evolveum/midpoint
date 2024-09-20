/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT;

import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * Used for both association definitions and resource object construction with associations.
 *
 * FIXME split these two uses!
 */
public class ResourceObjectAssociationConfigItem
        extends ConfigurationItem<ResourceObjectAssociationType>
        implements AssociationConfigItem {

    @SuppressWarnings({ "unused", "WeakerAccess" }) // called dynamically
    public ResourceObjectAssociationConfigItem(@NotNull ConfigurationItem<ResourceObjectAssociationType> original) {
        super(original);
    }

    public @NotNull ItemName getItemName() throws ConfigurationException {
        return singleNameRequired(value().getRef(), "association name (ref)");
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

    public @NotNull List<InboundMappingConfigItem> getInboundMappings() {
        return children(
                value().getInbound(),
                InboundMappingConfigItem.class,
                ResourceObjectAssociationType.F_INBOUND);
    }

    public boolean isExclusiveStrong() {
        return Boolean.TRUE.equals(value().isExclusiveStrong());
    }

    public boolean isTolerant() {
        return BooleanUtils.isNotFalse(value().isTolerant());
    }

    public @NotNull List<String> getTolerantValuePatterns() {
        return value().getTolerantValuePattern();
    }

    public @NotNull List<String> getIntolerantValuePatterns() {
        return value().getIntolerantValuePattern();
    }

    @Override
    public String debugDump(int indent) {
        return value().debugDump(indent);
    }

    @Override
    public @NotNull String localDescription() {
        return "resource association definition for '" + value().getRef() + "'";
    }

    public Legacy asLegacy() {
        return new Legacy(this);
    }

    /** Access to legacy configuration (i.e. combined association item definition + simulation definition). */
    public static class Legacy extends ResourceObjectAssociationConfigItem {

        Legacy(@NotNull ConfigurationItem<ResourceObjectAssociationType> original) {
            super(original);
        }

        @Override
        public Legacy asLegacy() {
            return this;
        }

        public @NotNull ShadowKindType getKind() {
            return Objects.requireNonNullElse(
                    value().getKind(), ShadowKindType.ENTITLEMENT);
        }

        /** The empty list is legal here. */
        public @NotNull List<String> getIntents() {
            return value().getIntent();
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

        public boolean isExplicitReferentialIntegrity() {
            return BooleanUtils.isNotFalse(value().isExplicitReferentialIntegrity());
        }

        public QName getMatchingRule() {
            return value().getMatchingRule();
        }

        @Override
        public @NotNull String localDescription() {
            return "legacy resource association definition for '" + value().getRef() + "'";
        }
    }
}
