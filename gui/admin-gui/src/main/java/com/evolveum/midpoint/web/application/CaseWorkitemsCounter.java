/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
