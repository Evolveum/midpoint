/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
