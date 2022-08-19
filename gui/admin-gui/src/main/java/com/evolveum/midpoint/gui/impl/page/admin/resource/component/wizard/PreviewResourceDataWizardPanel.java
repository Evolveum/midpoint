/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceContentPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceSchemaHandlingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceUncategorizedPanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

/**
 * @author lskublik
 */
public class PreviewResourceDataWizardPanel extends AbstractWizardBasicPanel {

    private static final String PANEL_TYPE = "resourceUncategorized";
    private static final String ID_TABLE = "table";

    public PreviewResourceDataWizardPanel(String id, ResourceDetailsModel model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ResourceContentPanel table
                = new ResourceContentPanel(ID_TABLE, null, getResourceModel(), getConfiguration(), false) {
            @Override
            protected boolean isTaskButtonsContainerVisible() {
                return false;
            }

            @Override
            protected boolean isTopTableButtonsVisible() {
                return false;
            }

            @Override
            protected boolean isSourceChoiceVisible() {
                return false;
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }

    ContainerPanelConfigurationType getConfiguration(){
        return WebComponentUtil.getContainerConfiguration(
                getResourceModel().getObjectDetailsPageConfiguration().getObject(),
                PANEL_TYPE);
    }

    @Override
    protected IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("PreviewResourceDataWizardPanel.title");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("PreviewResourceDataWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("PreviewResourceDataWizardPanel.subText");
    }
}
