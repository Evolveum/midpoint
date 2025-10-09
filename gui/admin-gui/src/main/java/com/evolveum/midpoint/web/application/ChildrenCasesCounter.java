/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.cases.CaseDetailsModels;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

public class ChildrenCasesCounter extends SimpleCounter<CaseDetailsModels, CaseType> {

    public ChildrenCasesCounter() {
        super();
    }

    @Override
    public int count(CaseDetailsModels objectDetailsModels, PageBase pageBase) {
        CaseType parentCase = objectDetailsModels.getObjectType();
        ObjectQuery childrenCasesQuery = PrismContext.get().queryFor(CaseType.class)
                .item(CaseType.F_PARENT_REF).ref(parentCase.getOid())
                .build();
        return WebModelServiceUtils.countObjects(CaseType.class, childrenCasesQuery, pageBase);
    }
}
