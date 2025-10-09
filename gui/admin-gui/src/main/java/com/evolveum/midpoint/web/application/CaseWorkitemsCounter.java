/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.cases.CaseDetailsModels;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

public class CaseWorkitemsCounter extends SimpleCounter<CaseDetailsModels, CaseType> {

    public CaseWorkitemsCounter() {
        super();
    }

    @Override
    public int count(CaseDetailsModels objectDetailsModels, PageBase pageBase) {
        CaseType parentCase = objectDetailsModels.getObjectType();
        return parentCase.getWorkItem() == null ? 0 : parentCase.getWorkItem().size();
    }
}
