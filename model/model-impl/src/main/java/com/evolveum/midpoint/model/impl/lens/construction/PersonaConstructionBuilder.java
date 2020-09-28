/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;

/**
 *  TEMPORARY
 */
public class PersonaConstructionBuilder<AH extends AssignmentHolderType>
        extends AbstractConstructionBuilder<AH, PersonaConstructionType, EvaluatedPersonaConstructionImpl<AH>, PersonaConstructionBuilder<AH>> {

    public PersonaConstructionBuilder() {
    }

    public PersonaConstruction<AH> build() {
        return new PersonaConstruction<>(this);
    }

}
