/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity.shadows;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper.TypedWorkDefinitionWrapper;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowIntegrityAspectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowIntegrityCheckWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ShadowIntegrityCheckWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

    private static final Map<String, ShadowIntegrityAspectType> KNOWN_KEYS = Map.of(
            "intents", ShadowIntegrityAspectType.INTENTS,
            "uniqueness", ShadowIntegrityAspectType.UNIQUENESS,
            "normalization", ShadowIntegrityAspectType.NORMALIZATION,
            "owners", ShadowIntegrityAspectType.OWNERS,
            "fetch", ShadowIntegrityAspectType.EXISTENCE_ON_RESOURCE, // old name
            "existenceOnResource", ShadowIntegrityAspectType.EXISTENCE_ON_RESOURCE, // new name
            "extraData", ShadowIntegrityAspectType.EXTRA_DATA,
            "resourceRef", ShadowIntegrityAspectType.RESOURCE_REF);

    @NotNull private final ObjectSetType shadows;
    @NotNull private final Set<ShadowIntegrityAspectType> aspectsToDiagnose = new HashSet<>();
    @NotNull private final Set<ShadowIntegrityAspectType> aspectsToFix = new HashSet<>();
    @NotNull private final String duplicateShadowsResolver;
    private final boolean checkDuplicatesOnPrimaryIdentifiersOnly;

    ShadowIntegrityCheckWorkDefinition(WorkDefinitionSource source) {
        String duplicateShadowsResolverNullable;
        Boolean checkDuplicatesOnPrimaryIdentifiersOnlyNullable;
        if (source instanceof LegacyWorkDefinitionSource) {
            LegacyWorkDefinitionSource legacySource = (LegacyWorkDefinitionSource) source;
            shadows = ObjectSetUtil.fromLegacySource(legacySource);
            aspectsToDiagnose.addAll(parseAspects(legacySource, SchemaConstants.MODEL_EXTENSION_DIAGNOSE));
            aspectsToFix.addAll(parseAspects(legacySource, SchemaConstants.MODEL_EXTENSION_FIX));
            duplicateShadowsResolverNullable = legacySource.getExtensionItemRealValue(
                    SchemaConstants.MODEL_EXTENSION_DUPLICATE_SHADOWS_RESOLVER, String.class);
            checkDuplicatesOnPrimaryIdentifiersOnlyNullable = legacySource.getExtensionItemRealValue(
                    SchemaConstants.MODEL_EXTENSION_CHECK_DUPLICATES_ON_PRIMARY_IDENTIFIERS_ONLY, Boolean.class);
        } else {
            ShadowIntegrityCheckWorkDefinitionType typedDefinition = (ShadowIntegrityCheckWorkDefinitionType)
                    ((TypedWorkDefinitionWrapper) source).getTypedDefinition();
            shadows = ObjectSetUtil.fromConfiguration(typedDefinition.getShadows());
            aspectsToDiagnose.addAll(typedDefinition.getDiagnose());
            aspectsToFix.addAll(typedDefinition.getFix());
            duplicateShadowsResolverNullable = typedDefinition.getDuplicateShadowsResolver();
            checkDuplicatesOnPrimaryIdentifiersOnlyNullable = typedDefinition.isCheckDuplicatesOnPrimaryIdentifiersOnly();
        }
        ObjectSetUtil.assumeObjectType(shadows, ShadowType.COMPLEX_TYPE);
        duplicateShadowsResolver = firstNonNull(duplicateShadowsResolverNullable, DefaultDuplicateShadowsResolver.class.getName());
        checkDuplicatesOnPrimaryIdentifiersOnly = Boolean.TRUE.equals(checkDuplicatesOnPrimaryIdentifiersOnlyNullable);
    }

    @Override
    public ObjectSetType getObjectSetSpecification() {
        return shadows;
    }

    @NotNull String getDuplicateShadowsResolver() {
        return duplicateShadowsResolver;
    }

    boolean isCheckDuplicatesOnPrimaryIdentifiersOnly() {
        return checkDuplicatesOnPrimaryIdentifiersOnly;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "shadows", shadows, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "aspectsToDiagnose", aspectsToDiagnose, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "aspectsToFix", aspectsToFix, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "duplicateShadowsResolver", duplicateShadowsResolver, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "checkDuplicatesOnPrimaryIdentifiersOnly", checkDuplicatesOnPrimaryIdentifiersOnly, indent+1);
    }

    boolean diagnoses(ShadowIntegrityAspectType aspect) {
        return aspectsToDiagnose.isEmpty() || aspectsToDiagnose.contains(aspect) || aspectsToFix.contains(aspect);
    }

    boolean fixes(ShadowIntegrityAspectType aspect) {
        return aspectsToFix.contains(aspect);
    }

    private Collection<ShadowIntegrityAspectType> parseAspects(LegacyWorkDefinitionSource legacySource, ItemName name) {
        return legacySource.getExtensionItemRealValues(name, String.class).stream()
                .map(this::resolveStringValue)
                .collect(Collectors.toSet());
    }

    private @NotNull ShadowIntegrityAspectType resolveStringValue(String string) {
        ShadowIntegrityAspectType aspect = KNOWN_KEYS.get(string);
        if (aspect != null) {
            return aspect;
        } else {
            throw new IllegalArgumentException("Unknown aspect: " + string + ". Known ones are: " + KNOWN_KEYS.keySet());
        }
    }
}
