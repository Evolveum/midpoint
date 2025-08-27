/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormCorrelationItemPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsSubCorrelatorType;

/**
 * @author lskublik
 */

@PanelType(name = "rw-correlators")
@PanelInstance(identifier = "rw-correlators",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "CorrelationItemRefsTableWizardPanel.headerLabel", icon = "fa fa-bars-progress"))
public class CorrelationItemRefsTableWizardPanel extends AbstractResourceWizardBasicPanel<ItemsSubCorrelatorType> {

    private static final String PANEL_TYPE = "rw-correlators";

    private static final String ID_PANEL = "panel";
    private static final String ID_TABLE = "table";

    public CorrelationItemRefsTableWizardPanel(
            String id,
            WizardPanelHelper<ItemsSubCorrelatorType, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    public <C extends Containerable> IModel<PrismContainerWrapper<C>> createContainerModel(
            IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> model, ItemPath path) {
        return PrismContainerWrapperModel.fromContainerValueWrapper(model, path);
    }

    private void initLayout() {

        IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel = getValueModel();

        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler((wrapper) -> ItemVisibility.AUTO)
                .isRemoveButtonVisible(false)
                .build();

        valueModel.getObject().setExpanded(true);
        VerticalFormCorrelationItemPanel panel =
                new VerticalFormCorrelationItemPanel(ID_PANEL, valueModel, settings);
        panel.setOutputMarkupId(true);
        add(panel);
        valueModel.getObject().getRealValue().asPrismContainerValue();

        CorrelationItemRefsTable table = new CorrelationItemRefsTable(ID_TABLE, getValueModel(), getConfiguration());
        table.setOutputMarkupId(true);
        add(table);
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        onExitPerformed(target);
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

    protected String getPanelType() {
        return PANEL_TYPE;
    }

}
