/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
