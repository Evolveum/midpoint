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
 * Current implementation is very limited as it deals only with locally-defined links in in archetype.
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

    @NotNull
    public <O extends ObjectType> LinkTypeDefinitionType getSourceLinkTypeDefinitionRequired(String linkTypeName,
            PrismObject<O> object, OperationResult result) throws SchemaException, ConfigurationException {
        LinkTypeDefinitionType definition = getSourceLinkTypeDefinition(linkTypeName, object, result);
        if (definition != null) {
            return definition;
        } else {
            throw new IllegalStateException("No source link '" + linkTypeName + "' present in " + object);
        }
    }

    public <O extends ObjectType> LinkTypeDefinitionType getSourceLinkTypeDefinition(String linkTypeName,
            PrismObject<O> object, OperationResult result) throws SchemaException, ConfigurationException {
        ArchetypePolicyType archetypePolicyType = determineArchetypePolicy(object, result);
        if (archetypePolicyType == null || archetypePolicyType.getLinks() == null) {
            return null;
        } else {
            return getLinkDefinition(linkTypeName, archetypePolicyType.getLinks().getSourceLink());
        }
    }

    @NotNull
    public <O extends ObjectType> LinkTypeDefinitionType getTargetLinkTypeDefinitionRequired(String linkTypeName,
            PrismObject<O> object, OperationResult result) throws SchemaException, ConfigurationException {
        LinkTypeDefinitionType definition = getTargetLinkTypeDefinition(linkTypeName, object, result);
        if (definition != null) {
            return definition;
        } else {
            throw new IllegalStateException("No target link '" + linkTypeName + "' present in " + object);
        }
    }

    public <O extends ObjectType> LinkTypeDefinitionType getTargetLinkTypeDefinition(String linkTypeName,
            PrismObject<O> object, OperationResult result) throws SchemaException, ConfigurationException {
        ArchetypePolicyType archetypePolicyType = determineArchetypePolicy(object, result);
        if (archetypePolicyType == null || archetypePolicyType.getLinks() == null) {
            return null;
        } else {
            return getLinkDefinition(linkTypeName, archetypePolicyType.getLinks().getTargetLink());
        }
    }

    private LinkTypeDefinitionType getLinkDefinition(String linkTypeName, List<LinkTypeDefinitionType> definitions) {
        List<LinkTypeDefinitionType> matchingDefinitions = definitions.stream()
                .filter(def -> linkTypeName.equals(def.getName()))
                .collect(Collectors.toList());
        return MiscUtil.extractSingleton(matchingDefinitions, () -> new IllegalStateException("Multiple link definitions named '" + linkTypeName + "'."));
    }

    private <O extends ObjectType> ArchetypePolicyType determineArchetypePolicy(PrismObject<O> object, OperationResult result) throws SchemaException, ConfigurationException {
        return archetypeManager.determineArchetypePolicy(object, result);
    }
}
