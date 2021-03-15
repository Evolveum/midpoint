/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

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
}
