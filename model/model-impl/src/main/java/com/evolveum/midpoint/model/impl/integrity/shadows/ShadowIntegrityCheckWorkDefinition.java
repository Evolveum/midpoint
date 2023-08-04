/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity.shadows;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionBean;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
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
    @NotNull private final Set<ShadowIntegrityAspectType> aspectsToDiagnose;
    @NotNull private final Set<ShadowIntegrityAspectType> aspectsToFix;
    @NotNull private final String duplicateShadowsResolver;
    private final boolean checkDuplicatesOnPrimaryIdentifiersOnly;

    ShadowIntegrityCheckWorkDefinition(@NotNull WorkDefinitionBean source, @NotNull ConfigurationItemOrigin origin) {
        super(origin);
        var typedDefinition = (ShadowIntegrityCheckWorkDefinitionType) source.getBean();
        shadows = ObjectSetUtil.emptyIfNull(typedDefinition.getShadows());
        ObjectSetUtil.assumeObjectType(shadows, ShadowType.COMPLEX_TYPE);
        aspectsToDiagnose = new HashSet<>(typedDefinition.getDiagnose());
        aspectsToFix = new HashSet<>(typedDefinition.getFix());
        duplicateShadowsResolver = firstNonNull(
                typedDefinition.getDuplicateShadowsResolver(),
                DefaultDuplicateShadowsResolver.class.getName());
        checkDuplicatesOnPrimaryIdentifiersOnly = Boolean.TRUE.equals(typedDefinition.isCheckDuplicatesOnPrimaryIdentifiersOnly());
    }

    @Override
    public @NotNull ObjectSetType getObjectSetSpecification() {
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
}
