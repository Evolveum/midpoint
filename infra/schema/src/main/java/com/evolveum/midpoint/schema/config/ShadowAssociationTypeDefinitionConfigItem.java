/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayHintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeSubjectDefinitionType;

public class ShadowAssociationTypeDefinitionConfigItem
        extends ConfigurationItem<ShadowAssociationTypeDefinitionType>
        implements AssociationConfigItem {

    @SuppressWarnings("unused") // called dynamically
    public ShadowAssociationTypeDefinitionConfigItem(@NotNull ConfigurationItem<ShadowAssociationTypeDefinitionType> original) {
        super(original);
    }

    public @NotNull QName getAssociationClassName() throws ConfigurationException {
        return nonNull(value().getAssociationClass(), "association class name");
    }

    public @NotNull Collection<? extends ResourceObjectTypeIdentification> getObjectTypeIdentifiers()
            throws ConfigurationException {
        var objectSpec = getObject();
        return objectSpec != null ? objectSpec.getTypeIdentifiers() : List.of();
    }

    public @NotNull Collection<? extends ResourceObjectTypeIdentification> getSubjectTypeIdentifiers()
            throws ConfigurationException {
        var subjectSpec = getSubject();
        return subjectSpec != null ? subjectSpec.getTypeIdentifiers() : List.of();
    }

    public @Nullable ShadowAssociationTypeObjectDefinitionConfigItem getObject()
            throws ConfigurationException {
        return child(
                value().getObject(),
                ShadowAssociationTypeObjectDefinitionConfigItem.class,
                ShadowAssociationTypeDefinitionType.F_OBJECT);
    }

    public @Nullable ShadowAssociationTypeSubjectDefinitionConfigItem getSubject()
            throws ConfigurationException {
        return child(
                value().getSubject(),
                ShadowAssociationTypeSubjectDefinitionConfigItem.class,
                ShadowAssociationTypeDefinitionType.F_SUBJECT);
    }

    @Override
    public boolean isExclusiveStrong() {
        return false; // not part of the configuration (we should deprecate it)
    }

    @Override
    public boolean isDeprecated() {
        var subject = value().getSubject();
        return subject != null && Boolean.TRUE.equals(subject.isDeprecated());
    }

    @Override
    public DisplayHintType getDisplayHint() {
        var subject = value().getSubject();
        return subject != null ? subject.getDisplayHint() : null;
    }

    @Override
    public boolean isTolerant() {
        var subject = value().getSubject();
        return subject == null || BooleanUtils.isNotFalse(subject.isTolerant());
    }

    @Override
    public @NotNull List<String> getTolerantValuePatterns() {
        var subject = value().getSubject();
        return subject != null ? subject.getTolerantValuePattern() : List.of();
    }

    @Override
    public @NotNull List<String> getIntolerantValuePatterns() {
        var subject = value().getSubject();
        return subject != null ? subject.getIntolerantValuePattern() : List.of();
    }

    @Override
    public String getDisplayName() {
        var subject = value().getSubject();
        var subjectDisplayName = subject != null ? subject.getDisplayName() : null;
        if (subjectDisplayName != null) {
            return subjectDisplayName;
        } else {
            return value().getDisplayName();
        }
    }

    @Override
    public Integer getDisplayOrder() {
        var subject = value().getSubject();
        return subject != null ? subject.getDisplayOrder() : null;
    }

    @Override
    public String getHelp() {
        var subject = value().getSubject();
        return subject != null ? subject.getHelp() : null;
    }

    @Override
    public String getDocumentation() {
        var subject = value().getSubject();
        var subjectDocumentation = subject != null ? subject.getDocumentation() : null;
        if (subjectDocumentation != null) {
            return subjectDocumentation;
        } else {
            return value().getDocumentation();
        }
    }

    @Override
    public String getLifecycleState() {
        return value().getLifecycleState();
    }

    public @Nullable MappingConfigItem getOutboundMapping() throws ConfigurationException {
        var subjectSpec = getSubject();
        return subjectSpec != null ? subjectSpec.getOutboundMapping() : null;
    }

    @Override
    public @NotNull List<InboundMappingConfigItem> getInboundMappings() throws ConfigurationException {
        var subjectSpec = getSubject();
        return subjectSpec != null ? subjectSpec.getInboundMappings() : List.of();
    }

    @Override
    public String debugDump(int indent) {
        return value().debugDump(indent);
    }

    @Override
    public @NotNull String localDescription() {
        return "the definition of association type '" + value().getAssociationClass() + "'";
    }
}
