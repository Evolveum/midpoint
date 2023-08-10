/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.apache.wicket.markup.repeater.RepeatingView;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.model.IModel;

@PanelType(name = "formSession")
//@PanelInstance(
//        identifier = "formSession",
//        applicableForType = RoleAnalysisClusterType.class,
//        applicableForOperation = OperationTypeType.MODIFY,
//        display = @PanelDisplay(label = "pageClusterOperation.session", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 10))
public class FormSessionPanel extends BasePanel<String> {

    private static final String ID_PANEL_BUTTONS = "panelButtons";

    public FormSessionPanel(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        RepeatingView panelButton = new RepeatingView(ID_PANEL_BUTTONS);
        add(panelButton);

        addPanelButton(panelButton);

    }

    protected void addPanelButton(RepeatingView repeatingView) {

    }

}
