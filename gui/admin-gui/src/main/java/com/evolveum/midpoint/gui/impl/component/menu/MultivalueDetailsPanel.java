/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.menu;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

public interface MultivalueDetailsPanel<C extends Containerable> {

    default void showSubMenu(IModel<PrismContainerValueWrapper<C>> valueModel, ObjectDetailsModels detailsModel,
            ContainerPanelConfigurationType configurationType, PageBase pageBase, AjaxRequestTarget target) {
        if (configurationType == null) {
            errorNotFoundSubmenuPanels(pageBase, target);
            return;
        }

        if (configurationType.getPanel().isEmpty()) {
            errorNotFoundSubmenuPanels(pageBase, target);
            return;
        }

        ContainerPanelConfigurationType firstPanel = null;

        for (ContainerPanelConfigurationType panelConfig : configurationType.getPanel()) {
            if (firstPanel == null || firstPanel.getDisplayOrder() > panelConfig.getDisplayOrder()) {
                firstPanel = panelConfig;
            }
            detailsModel.setSubPanelModel(panelConfig.getIdentifier(), valueModel);
        }

        if (firstPanel != null && pageBase instanceof AbstractPageObjectDetails pageObjectDetails) {
            pageObjectDetails.replacePanel(firstPanel, target);
            target.add(pageObjectDetails.getNavigationPanel());
        }

    }

    private void errorNotFoundSubmenuPanels(PageBase pageBase, AjaxRequestTarget target){
        pageBase.error(pageBase.createStringResource("MultivalueDetailsPanel.notFoundSubmenuPanels").getString());
        target.add(pageBase.getFeedbackPanel());
    }

}
