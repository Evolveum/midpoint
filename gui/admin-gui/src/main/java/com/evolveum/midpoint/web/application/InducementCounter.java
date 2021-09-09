/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

public class InducementCounter<AR extends AbstractRoleType> extends SimpleCounter<ObjectDetailsModels<AR>, AR> {

    public InducementCounter() {
        super();
    }

    @Override
    public int count(ObjectDetailsModels<AR> objectDetailsModels, PageBase pageBase) {
        PrismObjectWrapper<AR> abstractRole = objectDetailsModels.getObjectWrapperModel().getObject();
        AR object = abstractRole.getObject().asObjectable();
        return object.getInducement().size();
    }
}
