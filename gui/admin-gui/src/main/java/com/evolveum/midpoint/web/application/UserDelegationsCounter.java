/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.user.UserDetailsModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.List;

public class UserDelegationsCounter extends SimpleCounter<UserDetailsModel, UserType> {

    public UserDelegationsCounter() {
        super();
    }

    @Override
    public int count(UserDetailsModel objectDetailsModels, PageBase pageBase) {
        return objectDetailsModels.getDelegationsModel().getObject() == null ? 0 : objectDetailsModels.getDelegationsModel().getObject().size();
    }
}
