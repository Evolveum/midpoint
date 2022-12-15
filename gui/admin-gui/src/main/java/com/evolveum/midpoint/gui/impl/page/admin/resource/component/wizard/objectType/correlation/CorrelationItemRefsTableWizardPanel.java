/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardPanelHelper;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsSubCorrelatorType;

/**
 * @author lskublik
 */
public class CorrelationItemRefsTableWizardPanel extends AbstractResourceWizardBasicPanel<ItemsSubCorrelatorType> {

    private static final String ID_TABLE = "table";

    public CorrelationItemRefsTableWizardPanel(
            String id,
            ResourceWizardPanelHelper<ItemsSubCorrelatorType> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        CorrelationItemRefsTable table = new CorrelationItemRefsTable(ID_TABLE, getValueModel());
        table.setOutputMarkupId(true);
        add(table);
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        onExitPerformed(target);
    }

    @Override
    protected String getSubmitIcon() {
        return "fa fa-check";
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.confirmSettings");
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        String name = GuiDisplayNameUtil.getDisplayName(getValueModel().getObject().getRealValue());
        if (StringUtils.isNotBlank(name)) {
            return Model.of(name);
        }
        return getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.breadcrumb");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.subText");
    }

    protected CorrelationItemRefsTable getTable() {
        return (CorrelationItemRefsTable) get(ID_TABLE);
    }

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        return getTable().isValidFormComponents(target);
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }
}
