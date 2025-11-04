/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.cases.CaseWorkItemsTableWithDetailsPanel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import java.io.Serial;

/**
 * Created by honchar
 */
@PanelType(name = "caseWorkItems")
@PanelInstance(identifier = "caseWorkItems",
        display = @PanelDisplay(label = "PageCase.workitemsTab"))
@Counter(provider = CaseWorkitemsCounter.class)
public class CaseWorkitemsPanel extends AbstractObjectMainPanel<CaseType, AssignmentHolderDetailsModel<CaseType>> {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_WORKITEMS_PANEL = "workitemsPanel";

    public CaseWorkitemsPanel(String id, AssignmentHolderDetailsModel<CaseType> objectWrapperModel, ContainerPanelConfigurationType config) {
        super(id, objectWrapperModel, config);
    }

    protected void initLayout() {
        setOutputMarkupId(true);

        PrismContainerWrapperModel<CaseType, CaseWorkItemType> workitemsModel = PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), CaseType.F_WORK_ITEM);

        add(new CaseWorkItemsTableWithDetailsPanel(ID_WORKITEMS_PANEL, workitemsModel){
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected UserProfileStorage.TableId getTableId(){
                return UserProfileStorage.TableId.PAGE_CASE_WORKITEMS_TAB;
            }
        });
    }
}
