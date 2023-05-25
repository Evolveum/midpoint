/*
 * Copyright (c) 2017-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentPathImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Objects;

/**
 * @author semancik
 */
public class PersonaConstruction<AH extends AssignmentHolderType>
        extends AbstractConstruction<AH, PersonaConstructionType, EvaluatedPersonaConstructionImpl<AH>> {

    PersonaConstruction(PersonaConstructionBuilder<AH> builder) {
        super(builder);
    }

    @Override
    public @NotNull PersonaConstructionType getConstructionBean() {
        return Objects.requireNonNull(constructionBean);
    }

    @Override
    public @NotNull AssignmentPathImpl getAssignmentPath() {
        return Objects.requireNonNull(assignmentPath);
    }

    public DeltaSetTriple<EvaluatedPersonaConstructionImpl<AH>> getEvaluatedConstructionTriple() {
        EvaluatedPersonaConstructionImpl<AH> evaluatedConstruction = new EvaluatedPersonaConstructionImpl<>(this);
        return PrismContext.get().deltaFactory().createDeltaSetTriple(
                Collections.singleton(evaluatedConstruction),
                Collections.emptyList(),
                Collections.emptyList());
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, "PersonaConstruction", indent);
        if (constructionBean != null) {
            DebugUtil.debugDumpWithLabelLn(sb, "targetType", constructionBean.getTargetType(), indent + 1);
            DebugUtil.debugDumpWithLabelToStringLn(sb, "archetypeRef", constructionBean.getArchetypeRef(), indent + 1);
            DebugUtil.debugDumpWithLabelToStringLn(sb, "strength", constructionBean.getStrength(), indent + 1);
        }
        DebugUtil.debugDumpWithLabelLn(sb, "valid", isValid(), indent + 1);
        sb.append("\n");
        if (constructionBean != null && constructionBean.getDescription() != null) {
            sb.append("\n");
            DebugUtil.debugDumpLabel(sb, "description", indent + 1);
            sb.append(" ").append(constructionBean.getDescription());
        }
        if (assignmentPath != null) {
            sb.append("\n");
            sb.append(assignmentPath.debugDump(indent + 1));
        }
        return sb.toString();

    }

    @Override
    public boolean isIgnored() {
        return false;
    }
}
