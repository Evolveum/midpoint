/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

import org.apache.commons.lang3.StringUtils;

public class FocusCasesCounter<F extends FocusType> extends SimpleCounter<FocusDetailsModels<F>, F> {

    public FocusCasesCounter() {
        super();
    }

    @Override
    public int count(FocusDetailsModels<F> objectDetailsModels, PageBase pageBase) {
        String oid = objectDetailsModels.getObjectType().getOid();
        if (StringUtils.isEmpty(oid)) {
            return 0;
        }

        ObjectQuery casesQuery = QueryUtils.filterForCasesOverObject(PrismContext.get().queryFor(CaseType.class), oid)
                .desc(ItemPath.create(CaseType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP))
                .build();
        return WebModelServiceUtils.countObjects(CaseType.class, casesQuery, pageBase);
    }
}
