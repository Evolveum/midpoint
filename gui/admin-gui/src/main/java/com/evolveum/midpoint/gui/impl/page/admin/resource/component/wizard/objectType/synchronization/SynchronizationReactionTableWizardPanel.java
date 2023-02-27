/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization;

import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationReactionType;

/**
 * @author lskublik
 */
public abstract class SynchronizationReactionTableWizardPanel extends AbstractResourceWizardBasicPanel<ResourceObjectTypeDefinitionType> {

    private static final String ID_TABLE = "table";

    private static final String ID_DEPRECATED_CONTAINER_INFO = "deprecatedContainerInfo";

    public SynchronizationReactionTableWizardPanel(
            String id,
            WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        SynchronizationReactionTable table = new SynchronizationReactionTable(ID_TABLE, getValueModel()) {
            @Override
            protected void editItemPerformed(
                    AjaxRequestTarget target,
                    IModel<PrismContainerValueWrapper<SynchronizationReactionType>> rowModel,
                    List<PrismContainerValueWrapper<SynchronizationReactionType>> listItems) {
                inEditNewValue(rowModel, target);
            }
        };

        table.setOutputMarkupId(true);
        add(table);

        Label info = new Label(
                ID_DEPRECATED_CONTAINER_INFO,
                getPageBase().createStringResource("SynchronizationReactionTableWizardPanel.deprecatedContainer"));
        info.setOutputMarkupId(true);
        info.add(new VisibleBehaviour(this::isDeprecatedContainerInfoVisible));
        add(info);
    }

    private boolean isDeprecatedContainerInfoVisible() {
        PrismContainerValue depSynch = getAssignmentHolderDetailsModel().getObjectType().getSynchronization().clone().asPrismContainerValue();
        depSynch = WebPrismUtil.cleanupEmptyContainerValue(depSynch);
        return depSynch != null && !depSynch.hasNoItems();
    }

    public SynchronizationReactionTable getTablePanel() {
        return (SynchronizationReactionTable) get(ID_TABLE);
    }

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        return getTablePanel().isValidFormComponents(target);
    }

    @Override
    protected String getSaveLabelKey() {
        return "SynchronizationReactionTableWizardPanel.saveButton";
    }

    protected abstract void inEditNewValue(IModel<PrismContainerValueWrapper<SynchronizationReactionType>> value, AjaxRequestTarget target);

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getTextModel();
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("SynchronizationReactionTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("SynchronizationReactionTableWizardPanel.subText");
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-10";
    }
}
