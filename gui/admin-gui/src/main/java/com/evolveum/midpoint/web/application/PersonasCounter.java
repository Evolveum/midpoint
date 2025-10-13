/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.user.UserDetailsModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.List;

public class PersonasCounter extends SimpleCounter<UserDetailsModel, UserType> {

    public PersonasCounter() {
        super();
    }

    @Override
    public int count(UserDetailsModel objectDetailsModels, PageBase pageBase) {
        List<ObjectReferenceType> personasRefList = objectDetailsModels.getObjectType().getPersonaRef();
        int count = 0;
        for (ObjectReferenceType object : personasRefList) {
            if (object != null && !object.asReferenceValue().isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
