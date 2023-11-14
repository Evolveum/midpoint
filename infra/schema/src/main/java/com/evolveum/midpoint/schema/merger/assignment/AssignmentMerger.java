/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.assignment;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.schema.merger.BaseItemMerger;
import com.evolveum.midpoint.schema.merger.OriginMarker;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.ShadowUtil.resolveDefault;

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

    private boolean matchByAssignmentType(AssignmentType def, AssignmentType source, AssignmentTypeType type) {
        AssignmentTypeType targetType = getAssignmentType(def);
        if (!type.equals(targetType)) {
            return false;
        }
        switch (targetType) {
            case CONSTRUCTION -> {
                boolean allIsNull = true;
                if (def.getConstruction().getResourceRef() != null
                        && source.getConstruction().getResourceRef() != null) {
                    if (!(def.getConstruction().getResourceRef().asReferenceValue().equals(
                            source.getConstruction().getResourceRef().asReferenceValue(),
                            BaseItemMerger.VALUE_COMPARISON_STRATEGY))) {
                        return false;
                    }
                    allIsNull = false;
                } else if (!(def.getConstruction().getResourceRef() == null
                        && source.getConstruction().getResourceRef() == null)) {
                    return false;
                }

                if (def.getConstruction().getKind() != null
                        && source.getConstruction().getKind() != null) {
                    if (!(def.getConstruction().getKind().equals(source.getConstruction().getKind()))) {
                        return false;
                    }
                    allIsNull = false;
                } else if (!(def.getConstruction().getKind() == null
                        && source.getConstruction().getKind() == null)) {
                    return false;
                }

                if (def.getConstruction().getIntent() != null
                        && source.getConstruction().getIntent() != null) {
                    if (!(def.getConstruction().getIntent().equals(source.getConstruction().getIntent()))) {
                        return false;
                    }
                    allIsNull = false;
                } else if (!(def.getConstruction().getIntent() == null
                        && source.getConstruction().getIntent() == null)) {
                    return false;
                }
                return !allIsNull;
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
                if (def.getPersonaConstruction().getObjectMappingRef() != null
                        && source.getPersonaConstruction().getObjectMappingRef() != null
                        && def.getPersonaConstruction().getObjectMappingRef().asReferenceValue().equals(
                        source.getPersonaConstruction().getObjectMappingRef().asReferenceValue(),
                        BaseItemMerger.VALUE_COMPARISON_STRATEGY)) {
                    return true;
                }
                return false;
            }
            case POLICY_RULE -> {
                boolean allIsNull = true;
                if (def.getPolicyRule().getName() != null
                        && source.getPolicyRule().getName() != null) {
                    if (!(def.getPolicyRule().getName().equals(source.getPolicyRule().getName()))) {
                        return false;
                    }
                    allIsNull = false;
                } else if (!(def.getPolicyRule().getName() == null
                        && source.getPolicyRule().getName() == null)) {
                    return false;
                }

                if (def.getPolicyRule().getPolicySituation() != null
                        && source.getPolicyRule().getPolicySituation() != null) {
                    if (!(def.getPolicyRule().getPolicySituation().equals(source.getPolicyRule().getPolicySituation()))) {
                        return false;
                    }
                    allIsNull = false;
                } else if (!(def.getPolicyRule().getPolicySituation() == null
                        && source.getPolicyRule().getPolicySituation() == null)) {
                    return false;
                }
                return !allIsNull;
            }
            case ABSTRACT_ROLE -> {
                if (def.getTargetRef() != null
                        && source.getTargetRef() != null
                        && def.getTargetRef().asReferenceValue().equals(
                        source.getTargetRef().asReferenceValue(), BaseItemMerger.VALUE_COMPARISON_STRATEGY)) {
                    return true;
                }
                return false;
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
