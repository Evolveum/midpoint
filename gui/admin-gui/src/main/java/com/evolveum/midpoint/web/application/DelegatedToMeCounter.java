/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.user.UserDetailsModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class DelegatedToMeCounter extends SimpleCounter<UserDetailsModel, UserType> {

    public DelegatedToMeCounter() {
        super();
    }

    @Override
    public int count(UserDetailsModel objectDetailsModels, PageBase pageBase) {
        return objectDetailsModels.getDelegatedToMeModel().getObject() == null ?
                0 : objectDetailsModels.getDelegatedToMeModel().getObject().size();
    }
}
