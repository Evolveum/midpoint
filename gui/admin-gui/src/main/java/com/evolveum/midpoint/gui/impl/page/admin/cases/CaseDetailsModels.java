package com.evolveum.midpoint.gui.impl.page.admin.cases;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;


public class CaseDetailsModels extends AssignmentHolderDetailsModel<CaseType> {

    private static final Trace LOGGER = TraceManager.getTrace(FocusDetailsModels.class);
    private static final String DOT_CLASS = FocusDetailsModels.class.getName() + ".";

    public CaseDetailsModels(LoadableModel<PrismObject<CaseType>> prismObjectModel, PageBase serviceLocator) {
        super(prismObjectModel, serviceLocator);
    }

    @Override
    protected boolean isReadonly() {
        return true;
    }
}
