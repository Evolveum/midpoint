/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation;

import com.evolveum.midpoint.gui.api.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsSubCorrelatorType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author lskublik
 */
public abstract class CorrelationItemRefsTableWizardPanel extends AbstractWizardBasicPanel {

    private static final String ID_TABLE = "table";

    private final IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel;

    public CorrelationItemRefsTableWizardPanel(
            String id,
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel) {
        super(id, model);
        this.valueModel = valueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        CorrelationItemRefsTable table = new CorrelationItemRefsTable(ID_TABLE, valueModel);
        table.setOutputMarkupId(true);
        add(table);
    }

    @Override
    protected void addCustomButtons(RepeatingView buttons) {
        AjaxIconButton saveButton = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fa fa-check"),
                getPageBase().createStringResource("CorrelationItemRefTableWizardPanel.confirmSettings")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onExitPerformed(target);
            }
        };
        saveButton.showTitleAsLabel(true);
        saveButton.add(AttributeAppender.append("class", "btn btn-success"));
        buttons.add(saveButton);
    }

    @Override
    protected void onExitPerformed(AjaxRequestTarget target) {
        super.onExitPerformed(target);
    }

    @Override
    protected IModel<String> getBreadcrumbLabel() {
        String name = GuiDisplayNameUtil.getDisplayName(valueModel.getObject().getRealValue());
        if (StringUtils.isNotBlank(name)) {
            return Model.of(name);
        }
        return getPageBase().createStringResource("CorrelationItemRefTableWizardPanel.breadcrumb");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("CorrelationItemRefTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("CorrelationItemRefTableWizardPanel.subText");
    }

    protected CorrelationItemRefsTable getTable() {
        return (CorrelationItemRefsTable) get(ID_TABLE);
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }
}
