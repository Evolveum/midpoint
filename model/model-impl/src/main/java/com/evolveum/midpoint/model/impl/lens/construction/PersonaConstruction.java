/*
 * Copyright (c) 2017-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
        debugDumpConstructionDescription(sb, indent);
        debugDumpAssignmentPath(sb, indent);
        return sb.toString();

    }

    @Override
    public boolean isIgnored() {
        return false;
    }
}
