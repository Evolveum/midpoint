/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.util.HumanReadableDescribable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PersonaKey implements HumanReadableDescribable {

    private final PersonaProcessor personaProcessor;
    private final QName type;
    @NotNull private final List<ObjectReferenceType> archetypeRefs;
    @NotNull private final Set<String> archetypeOids;

    PersonaKey(PersonaProcessor personaProcessor, PersonaConstructionType constructionType) {
        super();
        this.personaProcessor = personaProcessor;
        this.type = constructionType.getTargetType();
        this.archetypeRefs = constructionType.getArchetypeRef();
        // We currently do not support dynamic filter-based archetype references.
        this.archetypeOids = archetypeRefs.stream()
                .map(AbstractReferencable::getOid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public QName getType() {
        return type;
    }

    @NotNull public Set<String> getArchetypeOids() {
        return archetypeOids;
    }

    @Override
    public String toHumanReadableDescription() {
        return "persona " + type.getLocalPart() + "/" + archetypeRefs;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getOuterType().hashCode();
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + archetypeOids.hashCode();
        return result;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {return true;}
        if (obj == null) {return false;}
        if (getClass() != obj.getClass()) {return false;}
        PersonaKey other = (PersonaKey) obj;
        if (!getOuterType().equals(other.getOuterType())) {return false;}
        if (type == null) {
            if (other.type != null) {return false;}
        } else if (!type.equals(other.type)) {return false;}
        if (!archetypeOids.equals(other.archetypeOids)) {return false;}
        return true;
    }

    @Override
    public String toString() {
        return "PersonaKey(" + type + "/" + archetypeRefs + ")";
    }

    private PersonaProcessor getOuterType() {
        return personaProcessor;
    }

}
