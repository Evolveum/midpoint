/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization;

import com.evolveum.midpoint.gui.api.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.MappingOverrideTable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationReactionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

/**
 * @author lskublik
 */
public abstract class SynchronizationReactionTableWizardPanel extends AbstractWizardBasicPanel {

    private static final String ID_TABLE = "table";

    private static final String ID_DEPRECATED_CONTAINER_INFO = "deprecatedContainerInfo";

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public SynchronizationReactionTableWizardPanel(
            String id,
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, model);
        this.valueModel = valueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        SynchronizationReactionTable table = new SynchronizationReactionTable(ID_TABLE, valueModel) {
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
        info.add(new VisibleBehaviour(() -> isDeprecatedContainerInfoVisible()));
        add(info);
    }

    private boolean isDeprecatedContainerInfoVisible() {
        PrismContainerValue depSynch = getResourceModel().getObjectType().getSynchronization().clone().asPrismContainerValue();
        depSynch = WebPrismUtil.cleanupEmptyContainerValue(depSynch);
        return depSynch != null && !depSynch.hasNoItems();
    }

    public SynchronizationReactionTable getTablePanel() {
        //noinspection unchecked
        return (SynchronizationReactionTable) get(ID_TABLE);
    }

    @Override
    protected void addCustomButtons(RepeatingView buttons) {
        AjaxIconButton newObjectTypeButton = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fa fa-circle-plus"),
                getPageBase().createStringResource("SynchronizationReactionTableWizardPanel.addNewReaction")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onAddNewObject(target);
            }
        };
        newObjectTypeButton.showTitleAsLabel(true);
        newObjectTypeButton.add(AttributeAppender.append("class", "btn btn-primary"));
        buttons.add(newObjectTypeButton);

        AjaxIconButton saveButton = new AjaxIconButton(
                buttons.newChildId(),
                Model.of(getSubmitIcon()),
                getSubmitLabelModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onSaveResourcePerformed(target);
            }
        };
        saveButton.showTitleAsLabel(true);
        saveButton.add(AttributeAppender.append("class", "btn btn-success"));
        buttons.add(saveButton);
    }

    protected String getSubmitIcon() {
        return "fa fa-floppy-disk";
    }

    protected IModel<String> getSubmitLabelModel() {
        return getPageBase().createStringResource("SynchronizationReactionTableWizardPanel.saveButton");
    }

    private void onAddNewObject(AjaxRequestTarget target) {
        SynchronizationReactionTable table = getTablePanel();
        inEditNewValue(Model.of(table.createNewReaction(target)), target);
    }

    protected abstract void onSaveResourcePerformed(AjaxRequestTarget target);

    protected abstract void inEditNewValue(IModel<PrismContainerValueWrapper<SynchronizationReactionType>> value, AjaxRequestTarget target);

    @Override
    protected IModel<String> getBreadcrumbLabel() {
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
}
