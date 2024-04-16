/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.assignment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.impl.BaseItemMerger;
import com.evolveum.midpoint.prism.OriginMarker;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * A merger specific to assignment: creates inheritance relations between the same assignments
 * (matched by kind and intent).
 */
public class AssignmentMerger extends BaseItemMerger<PrismContainer<AssignmentType>> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentMerger.class);

    public enum AssignmentTypeType {
        CONSTRUCTION, ABSTRACT_ROLE, POLICY_RULE, FOCUS_MAPPING, PERSONA_CONSTRUCTION
    }

    public AssignmentMerger(@Nullable OriginMarker originMarker) {
        super(originMarker);
    }

    protected void mergeInternal(
            @NotNull PrismContainer<AssignmentType> target,
            @NotNull PrismContainer<AssignmentType> source)
            throws ConfigurationException, SchemaException {
        for (AssignmentType sourceDefinition : source.getRealValues()) {
            AssignmentType matching = find(target, sourceDefinition);
            if (matching != null) {
                LOGGER.trace("Adding {}/{} (merged)", sourceDefinition);
                new AssignmentMergeOperation(matching, sourceDefinition, originMarker)
                        .execute();
            } else {
                LOGGER.trace("Adding {}/{} (as is)", sourceDefinition);
                //noinspection unchecked
                target.add(
                        createMarkedClone(sourceDefinition)
                                .asPrismContainerValue());
            }
        }
    }

    /**
     * Finds a matching assignment. Obviously, this must be called before the respective source definition is transferred
     * into the target.
     */
    private AssignmentType find(PrismContainer<AssignmentType> target, AssignmentType source) {
        AssignmentTypeType type = getAssignmentType(source);
        var matching = target.getRealValues().stream()
                .filter(def -> matchesAssignments(def, source, type))
                .collect(Collectors.toList());
        return MiscUtil.extractSingleton(matching,
                () -> new IllegalStateException("Multiple matching definitions for " + source + ": " + matching));
    }

    /** We resolve type of assignment. */
    private boolean matchesAssignments(AssignmentType def, AssignmentType source, AssignmentTypeType type) {
        if (def.getIdentifier() != null && source.getIdentifier() != null) {
            return def.getIdentifier().equals(source.getIdentifier());
        }
        boolean matchByAssignmentType = matchByAssignmentType(def, source, type);
        if (matchByAssignmentType) {
            return true;
        }
        return equalsAssignmentWithoutIdentifier(def, source);
    }

    private <O> Boolean match(List<Boolean> matchResults, O o1, O o2, BiFunction<O, O, Boolean> equals) {
        if (o1 == null && o2 == null) {
            matchResults.add(null);
            return null;
        }

        if (o1 == null || o2 == null) {
            matchResults.add(false);
            return false;
        }

        boolean result = equals.apply(o1, o2);
        matchResults.add(result);

        return result;
    }

    private boolean nonNullMatch(ObjectReferenceType o1, ObjectReferenceType o2) {
        if (o1 == null || o2 == null) {
            return false;
        }

        return o1.asReferenceValue().equals(o2.asReferenceValue(), BaseItemMerger.VALUE_COMPARISON_STRATEGY);
    }

    private boolean matchByAssignmentType(AssignmentType def, AssignmentType source, AssignmentTypeType type) {
        AssignmentTypeType targetType = getAssignmentType(def);
        if (!type.equals(targetType)) {
            return false;
        }
        switch (targetType) {
            case CONSTRUCTION -> {
                ConstructionType defConstr = def.getConstruction();
                ConstructionType sourceConstr = source.getConstruction();

                List<Boolean> results = new ArrayList<>();
                Boolean match = match(
                        results, defConstr.getResourceRef(), sourceConstr.getResourceRef(),
                        (defRes, sourceRes) -> nonNullMatch(defRes, sourceRes));
                if (BooleanUtils.isFalse(match)) {
                    return false;
                }

                match = match(
                        results, defConstr.getKind(), sourceConstr.getKind(),
                        (defKind, sourceKind) -> defKind.equals(sourceKind));
                if (BooleanUtils.isFalse(match)) {
                    return false;
                }

                match = match(
                        results, defConstr.getIntent(), sourceConstr.getIntent(),
                        (defIntent, sourceIntent) -> defIntent.equals(sourceIntent));
                if (BooleanUtils.isFalse(match)) {
                    return false;
                }

                return results.stream().allMatch(b -> b != null);
            }
//            case FOCUS_MAPPING -> {
//                List<String> targetMappingsNames = getMappingsNames(def.getFocusMappings());
//                List<String> sourceMappingsNames = getMappingsNames(source.getFocusMappings());
//                if (targetMappingsNames.isEmpty() || sourceMappingsNames.isEmpty()) {
//                    return false;
//                }
//                return sourceMappingsNames.stream().anyMatch(sourceName -> targetMappingsNames.contains(sourceName));
//            }
            case PERSONA_CONSTRUCTION -> {
                PersonaConstructionType defPersona = def.getPersonaConstruction();
                PersonaConstructionType sourcePersona = source.getPersonaConstruction();

                return nonNullMatch(defPersona.getObjectMappingRef(), sourcePersona.getObjectMappingRef());
            }
            case POLICY_RULE -> {
                PolicyRuleType defRule = def.getPolicyRule();
                PolicyRuleType sourceRule = source.getPolicyRule();

                List<Boolean> results = new ArrayList<>();
                Boolean match = match(
                        results, defRule.getName(), sourceRule.getName(),
                        (defName, sourceName) -> defName.equals(sourceName));
                if (BooleanUtils.isFalse(match)) {
                    return false;
                }

                match = match(
                        results, defRule.getPolicySituation(), sourceRule.getPolicySituation(),
                        (defSituation, sourceSituation) -> defSituation.equals(sourceSituation));
                if (BooleanUtils.isFalse(match)) {
                    return false;
                }

                return results.stream().allMatch(b -> b != null);
            }
            case ABSTRACT_ROLE -> {
                return nonNullMatch(def.getTargetRef(), source.getTargetRef());
            }
        }
        return false;
    }

    private boolean equalsAssignmentWithoutIdentifier(AssignmentType def, AssignmentType source) {
        AssignmentType defWithoutIdentifier = getAssignmentWithoutIdentifier(def);
        AssignmentType sourceWithoutIdentifier = getAssignmentWithoutIdentifier(source);
        return defWithoutIdentifier.equals(sourceWithoutIdentifier);
    }

    private AssignmentType getAssignmentWithoutIdentifier(AssignmentType assignmentType) {
        if (assignmentType.getIdentifier() == null) {
            return assignmentType;
        }
        AssignmentType ret = assignmentType.clone();
        ret.setIdentifier(null);
        return ret;
    }

    private List<String> getMappingsNames(MappingsType focusMappings) {
        return focusMappings.getMapping().stream()
                .filter(mappingType -> mappingType.getName() != null)
                .map(mapping -> mapping.getName())
                .collect(Collectors.toList());
    }

    private AssignmentTypeType getAssignmentType(AssignmentType assignment) {
        if (assignment == null) {
            return AssignmentTypeType.ABSTRACT_ROLE;
        }

        if (assignment.getConstruction() != null) {
            return AssignmentTypeType.CONSTRUCTION;
        }

        if (assignment.getPolicyRule() != null) {
            return AssignmentTypeType.POLICY_RULE;
        }

        if (assignment.getFocusMappings() != null) {
            return AssignmentTypeType.FOCUS_MAPPING;
        }

        if (assignment.getPersonaConstruction() != null) {
            return AssignmentTypeType.PERSONA_CONSTRUCTION;
        }

        return AssignmentTypeType.ABSTRACT_ROLE;
    }
}
