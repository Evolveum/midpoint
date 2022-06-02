/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RequestAccess implements Serializable {

    private List<ObjectReferenceType> personOfInterest;

    public List<ObjectReferenceType> getPersonOfInterest() {
        if (personOfInterest == null) {
            personOfInterest = new ArrayList<>();
        }
        return personOfInterest;
    }

    public void setPersonOfInterest(List<ObjectReferenceType> personOfInterest) {
        this.personOfInterest = personOfInterest;
    }
}
