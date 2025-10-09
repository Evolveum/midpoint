/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.rolemanagement;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RelationsCounter extends SimpleCounter<AssignmentHolderDetailsModel<SystemConfigurationType>, SystemConfigurationType> {

    public RelationsCounter() {
        super();
    }

    @Override
    public int count(AssignmentHolderDetailsModel<SystemConfigurationType> model, PageBase pageBase) {
        SystemConfigurationType object = model.getObjectType();
        RoleManagementConfigurationType rmc = object.getRoleManagement();
        if (rmc == null) {
            return 0;
        }

        RelationsDefinitionType relations = rmc.getRelations();
        return rmc != null ? relations.getRelation().size() : 0;
    }
}

