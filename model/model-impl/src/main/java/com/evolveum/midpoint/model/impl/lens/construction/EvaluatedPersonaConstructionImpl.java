/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

/**
 * @author Radovan Semancik
 */
public class EvaluatedPersonaConstructionImpl<AH extends AssignmentHolderType> implements EvaluatedAbstractConstruction<AH> {

    private final PersonaConstruction<AH> construction;

    EvaluatedPersonaConstructionImpl(@NotNull final PersonaConstruction<AH> construction) {
        this.construction = construction;
    }

    @Override
    public PersonaConstruction<AH> getConstruction() {
        return construction;
    }

    @Override
    public String toString() {
        return "EvaluatedPersonaConstructionImpl(" +
                ", construction=" + getConstruction() +
                ')';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "construction", construction, indent + 1);
        return sb.toString();
    }
}
