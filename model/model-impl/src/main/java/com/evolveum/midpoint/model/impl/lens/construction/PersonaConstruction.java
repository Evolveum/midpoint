/*
 * Copyright (c) 2017-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;

import java.util.Collections;

/**
 * @author semancik
 *
 */
public class PersonaConstruction<AH extends AssignmentHolderType> extends AbstractConstruction<AH, PersonaConstructionType, EvaluatedPersonaConstructionImpl<AH>> {

    public PersonaConstruction(PersonaConstructionType constructionType, ObjectType source) {
        super(constructionType, source);
    }

    public DeltaSetTriple<EvaluatedPersonaConstructionImpl<AH>> getEvaluatedConstructionTriple() {
        EvaluatedPersonaConstructionImpl<AH> evaluatedConstruction = new EvaluatedPersonaConstructionImpl<>(this);
        return getPrismContext().deltaFactory().createDeltaSetTriple(Collections.singleton(evaluatedConstruction), Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, "PersonaConstruction", indent);
        PersonaConstructionType constructionType = getConstructionType();
        if (constructionType != null) {
            DebugUtil.debugDumpWithLabelLn(sb, "targetType", constructionType.getTargetType(), indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "subtype", constructionType.getTargetSubtype(), indent + 1);
            DebugUtil.debugDumpWithLabelToStringLn(sb, "strength", constructionType.getStrength(), indent + 1);
        }
        DebugUtil.debugDumpWithLabelLn(sb, "isValid", isValid(), indent + 1);
        sb.append("\n");
        if (getConstructionType() != null && getConstructionType().getDescription() != null) {
            sb.append("\n");
            DebugUtil.debugDumpLabel(sb, "description", indent + 1);
            sb.append(" ").append(getConstructionType().getDescription());
        }
        if (getAssignmentPath() != null) {
            sb.append("\n");
            sb.append(getAssignmentPath().debugDump(indent + 1));
        }
        return sb.toString();

    }

    @Override
    public boolean isIgnored() {
        return false;
    }
}
