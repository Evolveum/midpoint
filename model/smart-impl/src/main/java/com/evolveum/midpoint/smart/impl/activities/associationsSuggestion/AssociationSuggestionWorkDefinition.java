/*
 * Copyright (c) 2025-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */
package com.evolveum.midpoint.smart.impl.activities.associationsSuggestion;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationSuggestionWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BasicResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.processor.AssociationPair;

/**
 * For suggesting associations (for multiple subject/object types).
 */
public class AssociationSuggestionWorkDefinition extends AbstractWorkDefinition {

    private final String resourceOid;
    private final Set<AssociationPair> configuredAssociations;

    AssociationSuggestionWorkDefinition(@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        var typedDefinition = (AssociationSuggestionWorkDefinitionType) info.getBean();
        configuredAssociations = typedDefinition.getConfiguredAssociationPair().stream().map(AssociationPair::fromBean)
                .collect(Collectors.toUnmodifiableSet());

        resourceOid = configNonNull(
                Referencable.getOid(typedDefinition.getResourceRef()),
                "No resource OID specified");
    }

    public @NotNull Set<AssociationPair> getConfiguredAssociations() {
        return configuredAssociations;
    }

    public String getResourceOid() {
        return resourceOid;
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
        DebugUtil.debugDumpWithLabelLn(sb, "configuredAssociations", configuredAssociations, indent + 1);
    }
}
