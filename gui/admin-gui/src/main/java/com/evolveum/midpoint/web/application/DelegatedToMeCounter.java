/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
