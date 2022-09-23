/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common;

import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages link definitions.
 *
 * Current implementation is very limited as it deals only with locally-defined links in an archetype.
 * Future extensions:
 * - consider links defined in object policy configuration (in system configuration)
 * - consider links globally e.g. target link A->B defined in archetype A is visible as source link in archetype B
 * - define global links also in system configuration
 * - allow restriction also for defining object (i.e. not only 'the other side') using object selectors
 */
@Experimental
@Component
public class LinkManager {

    @Autowired private ArchetypeManager archetypeManager;

    public @NotNull LinkTypeDefinitionType getSourceLinkTypeDefinitionRequired(
            String linkTypeName,
            List<PrismObject<? extends ObjectType>> objectVariants,
            OperationResult result) throws SchemaException, ConfigurationException {
        return MiscUtil.requireNonNull(
                getSourceLinkTypeDefinition(linkTypeName, objectVariants, result),
                () -> new ConfigurationException("No source link '" + linkTypeName + "' present in " +
                        MiscUtil.getFirstNonNullFromList(objectVariants)));
    }

    public LinkTypeDefinitionType getSourceLinkTypeDefinition(
            String linkTypeName,
            List<PrismObject<? extends ObjectType>> objectVariants,
            OperationResult result) throws SchemaException, ConfigurationException {
        for (PrismObject<? extends ObjectType> objectVariant : objectVariants) {
            if (objectVariant != null) {
                ArchetypePolicyType archetypePolicy = archetypeManager.determineArchetypePolicy(objectVariant, result);
                LinkTypeDefinitionsType links = archetypePolicy != null ? archetypePolicy.getLinks() : null;
                if (links != null) {
                    LinkTypeDefinitionType definition = getLinkDefinition(linkTypeName, links.getSourceLink());
                    if (definition != null) {
                        return definition;
                    }
                }
            }
        }
        return null;
    }

    public @NotNull LinkTypeDefinitionType getTargetLinkTypeDefinitionRequired(
            String linkTypeName,
            List<PrismObject<? extends ObjectType>> objectVariants,
            OperationResult result) throws SchemaException, ConfigurationException {
        return MiscUtil.requireNonNull(
                getTargetLinkTypeDefinition(linkTypeName, objectVariants, result),
                () -> new ConfigurationException("No target link '" + linkTypeName + "' present in " +
                        MiscUtil.getFirstNonNullFromList(objectVariants)));
    }

    public LinkTypeDefinitionType getTargetLinkTypeDefinition(
            String linkTypeName,
            List<PrismObject<? extends ObjectType>> objectVariants,
            OperationResult result) throws SchemaException, ConfigurationException {
        for (PrismObject<? extends ObjectType> objectVariant : objectVariants) {
            if (objectVariant != null) {
                ArchetypePolicyType archetypePolicy = archetypeManager.determineArchetypePolicy(objectVariant, result);
                LinkTypeDefinitionsType links = archetypePolicy != null ? archetypePolicy.getLinks() : null;
                if (links != null) {
                    LinkTypeDefinitionType definition = getLinkDefinition(linkTypeName, links.getTargetLink());
                    if (definition != null) {
                        return definition;
                    }
                }
            }
        }
        return null;
    }

    private LinkTypeDefinitionType getLinkDefinition(String linkTypeName, List<LinkTypeDefinitionType> definitions) {
        List<LinkTypeDefinitionType> matchingDefinitions = definitions.stream()
                .filter(def -> linkTypeName.equals(def.getName()))
                .collect(Collectors.toList());
        return MiscUtil.extractSingleton(
                matchingDefinitions,
                () -> new IllegalStateException("Multiple link definitions named '" + linkTypeName + "'."));
    }
}
