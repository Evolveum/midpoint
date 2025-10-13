/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

public class FocusTriggersCounter<F extends FocusType> extends SimpleCounter<FocusDetailsModels<F>, F> {

    public FocusTriggersCounter() {
        super();
    }

    @Override
    public int count(FocusDetailsModels<F> objectDetailsModels, PageBase pageBase) {
        F focusObject = objectDetailsModels.getObjectType();
        return focusObject.getTrigger() != null ? focusObject.getTrigger().size() : 0;
    }
}
