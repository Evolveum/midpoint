/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.Collection;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_CREATE_TIMESTAMP;

@PanelType(name = "myWorkItems")
public class MyCaseWorkItemsPanel extends CaseWorkItemsPanel {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = MyCaseWorkItemsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_POWER_DONOR_OBJECT = DOT_CLASS + "loadPowerDonorObject";
    private static final String OPERATION_COMPLETE_WORK_ITEM = DOT_CLASS + "completeWorkItem";

    public MyCaseWorkItemsPanel(String id) {
        super(id);
    }

    public MyCaseWorkItemsPanel(String id, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(id, options);
    }

    public MyCaseWorkItemsPanel(String id, Collection<SelectorOptions<GetOperationOptions>> options, ContainerPanelConfigurationType configurationType) {
        super(id, options, configurationType);
    }

    //TODO wucik hack. cleanup needed. also, what about my cases? all cases? how to differentiate
    public MyCaseWorkItemsPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, (AssignmentHolderDetailsModel) null, configurationType);
    }

    protected ObjectFilter getCaseWorkItemsFilter() {
        return QueryUtils.filterForNotClosedStateAndAssignees(
                        getPrismContext().queryFor(CaseWorkItemType.class),
                        AuthUtil.getPrincipalUser(),
                        OtherPrivilegesLimitationType.F_CASE_MANAGEMENT_WORK_ITEMS)
                .desc(F_CREATE_TIMESTAMP)
                .buildFilter();
    }

}
