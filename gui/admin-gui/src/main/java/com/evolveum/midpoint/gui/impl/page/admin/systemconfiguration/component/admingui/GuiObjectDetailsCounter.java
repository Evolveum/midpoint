/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.admingui;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectListViewsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class GuiObjectDetailsCounter extends SimpleCounter<AssignmentHolderDetailsModel<SystemConfigurationType>, SystemConfigurationType> {

    public GuiObjectDetailsCounter() {
        super();
    }

    @Override
    public int count(AssignmentHolderDetailsModel<SystemConfigurationType> model, PageBase pageBase) {
        SystemConfigurationType object = model.getObjectType();
        AdminGuiConfigurationType adminGui = object.getAdminGuiConfiguration();
        if (adminGui == null) {
            return 0;
        }

        GuiObjectDetailsSetType views = adminGui.getObjectDetails();

        return views != null ? views.getObjectDetailsPage().size() : 0;
    }
}

