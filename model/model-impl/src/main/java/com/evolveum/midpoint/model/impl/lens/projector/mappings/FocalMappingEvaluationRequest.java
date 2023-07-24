/*
 * Copyright (c) 2019-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;

import com.evolveum.midpoint.schema.constants.ExpressionConstants;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.mapping.MappingPreExpression;
import com.evolveum.midpoint.model.impl.lens.AssignmentPathVariables;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

/**
 * Contains some of the information necessary to evaluate a mapping. It is used when mappings are collected e.g. from
 * template and its referenced sub-templates, auto-assigned roles, or (in the future) assignments. Each of these mappings need
 * to go along some minimal context (e.g. the holding template, role, or assignment path) that is to be used when mapping
 * is evaluated.
 *
 * @author semancik
 */
public abstract class FocalMappingEvaluationRequest<MT extends MappingType, OO extends ObjectType>
        implements ShortDumpable, Serializable {

    @NotNull protected final MT mapping;
    @NotNull final ConfigurationItemOrigin mappingOrigin;
    @NotNull protected final MappingKindType mappingKind;

    /**
     * This field is used e.g. to fill {@link ExpressionConstants#VAR_SOURCE} variable, and for other uses.
     * It should no longer be used to provide diagnostic info about mapping definition location.
     * The {@link #mappingOrigin} should be used instead.
     */
    @NotNull protected final OO originObject;

    private String mappingInfo; // lazily computed

    FocalMappingEvaluationRequest(
            @NotNull MT mapping,
            @NotNull ConfigurationItemOrigin mappingOrigin,
            @NotNull MappingKindType mappingKind,
            @NotNull OO originObject) {
        this.mapping = mapping;
        this.mappingOrigin = mappingOrigin;
        this.mappingKind = mappingKind;
        this.originObject = originObject;
    }

    @NotNull
    public MT getMapping() {
        return mapping;
    }

    public @NotNull ConfigurationItemOrigin getMappingOrigin() {
        return mappingOrigin;
    }

    public @NotNull List<VariableBindingDefinitionType> getSources() {
        return mapping.getSource();
    }

    public @Nullable VariableBindingDefinitionType getTarget() {
        return mapping.getTarget();
    }

    public <V extends PrismValue,
            D extends ItemDefinition<?>,
            AH extends AssignmentHolderType> Source<V,D>
    constructDefaultSource(ObjectDeltaObject<AH> focusOdo) throws SchemaException {
        return null;
    }

    public MappingPreExpression getMappingPreExpression() {
        return null;
    }

    @NotNull
    public OO getOriginObject() {
        return originObject;
    }

    @NotNull
    public MappingKindType getMappingKind() {
        return mappingKind;
    }

    /**
     * @return The phase this mapping should be evaluated in. If null, the mapping will be skipped if explicit
     *         evaluation phase is requested.
     */
    public abstract ObjectTemplateMappingEvaluationPhaseType getEvaluationPhase();

    public AssignmentPathVariables getAssignmentPathVariables() {
        return null;
    }

    String getMappingInfo() {
        if (mappingInfo == null) {
            StringBuilder sb = new StringBuilder();
            String name = mapping.getName();
            if (name != null) {
                sb.append(name).append(" (");
            }
            String sources = mapping.getSource().stream()
                    .filter(source -> source != null && source.getPath() != null)
                    .map(source -> source.getPath().toString())
                    .collect(Collectors.joining(", "));
            if (!sources.isEmpty()) {
                sb.append(sources).append(" ");
            }
            sb.append("->");
            var target = mapping.getTarget();
            ItemPathType targetPath = target != null ? target.getPath() : null;
            if (targetPath != null) {
                sb.append(" ").append(targetPath);
            }
            if (name != null) {
                sb.append(")");
            }
            mappingInfo = sb.toString();
        }
        return mappingInfo;
    }
}
