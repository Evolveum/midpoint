/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RequestAccessItem implements Serializable, Comparable<RequestAccessItem> {

    private ObjectReferenceType person;

    private List<AssignmentType> assignments;

    public RequestAccessItem(ObjectReferenceType person, List<AssignmentType> assignments) {
        this.person = person;
        this.assignments = assignments;
    }

    public ObjectReferenceType getPerson() {
        return person;
    }

    public List<AssignmentType> getAssignments() {
        return assignments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        RequestAccessItem that = (RequestAccessItem) o;

        if (person != null ? !person.equals(that.person) : that.person != null) {return false;}
        return assignments != null ? assignments.equals(that.assignments) : that.assignments == null;
    }

    @Override
    public int hashCode() {
        int result = person != null ? person.hashCode() : 0;
        result = 31 * result + (assignments != null ? assignments.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(@NotNull RequestAccessItem o) {
        if (o == null) {
            return 1;
        }

        ObjectReferenceType ref = o.getPerson();
        if (ref == null) {
            return person == null ? 0 : 1;
        }

        if (person == null) {
            return -1;
        }

        PolyStringType name = person.getTargetName();
        PolyStringType otherName = ref.getTargetName();



        return 0;
    }
}
