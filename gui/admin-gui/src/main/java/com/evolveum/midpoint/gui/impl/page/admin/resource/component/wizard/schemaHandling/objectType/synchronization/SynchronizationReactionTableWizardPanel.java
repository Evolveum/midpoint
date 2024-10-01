/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.synchronization;

import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

/**
 * @author skublik
 */
@PanelType(name = "rw-synchronization-reactions")
@PanelInstance(identifier = "rw-synchronization-reactions",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "SynchronizationReactionTableWizardPanel.headerLabel", icon = "fa fa-arrows-rotate"))
@PanelInstance(identifier = "rw-association-synchronization-reactions",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "SynchronizationReactionTableWizardPanel.headerLabel", icon = "fa fa-arrows-rotate"))
public abstract class SynchronizationReactionTableWizardPanel<C extends AbstractSynchronizationReactionType, P extends Containerable> extends AbstractResourceWizardBasicPanel<P> {

    private static final String OBJECT_TYPE_PANEL_TYPE = "rw-synchronization-reactions";
    private static final String ASSOCIATION_TYPE_PANEL_TYPE = "rw-association-synchronization-reactions";

    private static final String ID_TABLE = "table";

    private static final String ID_DEPRECATED_CONTAINER_INFO = "deprecatedContainerInfo";

    public SynchronizationReactionTableWizardPanel(String id, WizardPanelHelper<P, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        SynchronizationReactionTable<C, P> table = new SynchronizationReactionTable<>(
                ID_TABLE, getValueModel(), getConfiguration()) {
            @Override
            public void editItemPerformed(
                    AjaxRequestTarget target,
                    IModel<PrismContainerValueWrapper<C>> rowModel,
                    List<PrismContainerValueWrapper<C>> listItems) {
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

    protected abstract void inEditNewValue(IModel<PrismContainerValueWrapper<C>> value, AjaxRequestTarget target);

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

    protected String getPanelType() {
        if (getValueModel().getObject().getParentContainerValue(ResourceObjectTypeDefinitionType.class) != null) {
            return OBJECT_TYPE_PANEL_TYPE;
        }
        return ASSOCIATION_TYPE_PANEL_TYPE;
    }
}
