/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.cases;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import org.apache.wicket.model.LoadableDetachableModel;

public class CaseDetailsModels extends AssignmentHolderDetailsModel<CaseType> {

    private static final Trace LOGGER = TraceManager.getTrace(CaseDetailsModels.class);
    private static final String DOT_CLASS = CaseDetailsModels.class.getName() + ".";

    public CaseDetailsModels(LoadableDetachableModel<PrismObject<CaseType>> prismObjectModel, PageBase serviceLocator) {
        super(prismObjectModel, serviceLocator);
    }

    @Override
    protected boolean isReadonly() {
        return true;
    }
}
