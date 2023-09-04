/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.page.admin.server.RefreshableTabPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@PanelType(name = "miningBasic", defaultContainerPath = "empty")
@PanelInstance(identifier = "sessionBasic",
        applicableForType = RoleAnalysisSessionType.class,
        defaultPanel = true,
        display = @PanelDisplay(label = "pageAdminFocus.basic", order = 10))

@PanelInstance(identifier = "clusterBasic",
        applicableForType = RoleAnalysisClusterType.class,
        defaultPanel = true,
        display = @PanelDisplay(label = "pageAdminFocus.basic", order = 10))
public class RoleAnalysisBasicPanel<AH extends AssignmentHolderType> extends AbstractObjectMainPanel<AH, ObjectDetailsModels<AH>> {

    private static final String ID_MAIN_PANEL = "main";

    public RoleAnalysisBasicPanel(String id, AssignmentHolderDetailsModel<AH> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected void initLayout() {
        SingleContainerPanel mainPanel = new SingleContainerPanel(ID_MAIN_PANEL, getObjectWrapperModel(), getPanelConfiguration()) {

            @Override
            protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                return getBasicTabVisibility(itemWrapper.getPath());
            }

            @Override
            protected ItemEditabilityHandler getEditabilityHandler() {
                return wrapper -> true;
            }

        };
        add(mainPanel);
    }

    private ItemVisibility getBasicTabVisibility(ItemPath path) {
        if (RoleAnalysisSessionType.F_NAME.equivalent(path)) {
            return ItemVisibility.AUTO;
        }

        if (RoleAnalysisSessionType.F_DOCUMENTATION.equivalent(path)) {
            return ItemVisibility.AUTO;
        }

        if (RoleAnalysisSessionType.F_DESCRIPTION.equivalent(path)) {
            return ItemVisibility.AUTO;
        }

        return ItemVisibility.HIDDEN;
    }

}
