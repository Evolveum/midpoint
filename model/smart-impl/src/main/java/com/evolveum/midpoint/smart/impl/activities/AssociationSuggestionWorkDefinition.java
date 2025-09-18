/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.activities;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationSuggestionWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BasicResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * For suggesting associations (for multiple subject/object types).
 */
public class AssociationSuggestionWorkDefinition extends AbstractWorkDefinition {

    private final String resourceOid;
    private final List<ResourceObjectTypeIdentification> subjectTypeIdentifications;
    private final List<ResourceObjectTypeIdentification> objectTypeIdentifications;

    AssociationSuggestionWorkDefinition(@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        var typedDefinition = (AssociationSuggestionWorkDefinitionType) info.getBean();

        resourceOid = configNonNull(
                Referencable.getOid(typedDefinition.getResourceRef()),
                "No resource OID specified");

        subjectTypeIdentifications = typedDefinition.getSubjectObjectTypes().stream()
                .map(ResourceObjectTypeIdentification::of)
                .collect(Collectors.toList());

        objectTypeIdentifications = typedDefinition.getObjectObjectTypes().stream()
                .map(ResourceObjectTypeIdentification::of)
                .collect(Collectors.toList());
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public List<ResourceObjectTypeIdentification> getSubjectTypeIdentifications() {
        return subjectTypeIdentifications;
    }

    public List<ResourceObjectTypeIdentification> getObjectTypeIdentifications() {
        return objectTypeIdentifications;
    }

    @Override
    public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(
            @Nullable AbstractActivityWorkStateType state) {

        return AffectedObjectsInformation.ObjectSet.resource(
                new BasicResourceObjectSetType()
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE));
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "resourceOid", resourceOid, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "subjectTypeIdentifications", subjectTypeIdentifications.toString(), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "objectTypeIdentifications", objectTypeIdentifications.toString(), indent + 1);
    }
}
