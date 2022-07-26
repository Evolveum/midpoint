/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectTypes;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class ResourceObjectTypeWizardPanel extends BasePanel {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectTypeWizardPanel.class);

    private static final String ID_FRAGMENT = "fragment";
    private static final String ID_CHOICE_FRAGMENT = "choiceFragment";
    private static final String ID_CHOICE_PANEL = "choicePanel";
    private static final String ID_WIZARD_FRAGMENT = "wizardFragment";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_WIZARD = "wizard";

    private final ResourceDetailsModel resourceModel;

    public ResourceObjectTypeWizardPanel(String id, ResourceDetailsModel model) {
        super(id);
        this.resourceModel = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(createTableFragment());
    }

    protected Fragment createTableFragment() {
        Fragment fragment = new Fragment(ID_FRAGMENT, ID_CHOICE_FRAGMENT, ResourceObjectTypeWizardPanel.this);
        fragment.setOutputMarkupId(true);
        ResourceObjectTypeTableWizardPanel table = new ResourceObjectTypeTableWizardPanel(ID_CHOICE_PANEL, getResourceModel()) {
            @Override
            protected void onAddNewObject(AjaxRequestTarget target) {
                Fragment fragment = createNewResourceObjectTypeWizardFragment();
                fragment.setOutputMarkupId(true);
                ResourceObjectTypeWizardPanel.this.replace(fragment);
                target.add(fragment);
            }

            @Override
            protected void onEditValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
                Fragment fragment = createObjectTypePreviewFragment(valueModel);
                fragment.setOutputMarkupId(true);
                ResourceObjectTypeWizardPanel.this.replace(fragment);
                target.add(fragment);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceObjectTypeWizardPanel.this.onExitPerformed(target);
            }
        };
        fragment.add(table);
        return fragment;
    }

    protected void onExitPerformed(AjaxRequestTarget target) {
    }

    private Fragment createNewResourceObjectTypeWizardFragment() {
        Fragment fragment = new Fragment(ID_FRAGMENT, ID_WIZARD_FRAGMENT, ResourceObjectTypeWizardPanel.this);
        Form mainForm = new Form(ID_MAIN_FORM);
        fragment.add(mainForm);
        WizardPanel wizard = new WizardPanel(ID_WIZARD, new WizardModel(createNewObjectTypeSteps()));
        wizard.setOutputMarkupId(true);
        mainForm.add(wizard);
        return fragment;
    }

    private List<WizardStep> createNewObjectTypeSteps() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new BasicSettingResourceObjectTypeStepPanel(getResourceModel(), createModelOfResourceObjectType()) {
            @Override
            protected void onFinishWizardPerformed(AjaxRequestTarget target) {
                onSaveResourcePerformed(target);
                Fragment fragment = createObjectTypePreviewFragment(getValueModel());
                ResourceObjectTypeWizardPanel.this.replace(fragment);
                target.add(fragment);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                Fragment fragment = createTableFragment();
                fragment.setOutputMarkupId(true);
                ResourceObjectTypeWizardPanel.this.replace(fragment);
                target.add(fragment);
            }
        });

        return steps;
    }

    private Fragment createObjectTypePreviewFragment(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model) {
        Fragment fragment = new Fragment(ID_FRAGMENT, ID_CHOICE_FRAGMENT, ResourceObjectTypeWizardPanel.this);
        fragment.setOutputMarkupId(true);

        ResourceObjectTypeWizardPreviewPanel preview = new ResourceObjectTypeWizardPreviewPanel(ID_CHOICE_PANEL, getResourceModel(), model) {
            @Override
            protected void onResourceTileClick(ResourceObjectTypePreviewTileType value, AjaxRequestTarget target) {
//                switch (value) {
//                    case CONFIGURE_OBJECT_TYPES:
//                        ResourceObjectTypeWizardPanel objectTypeWizard = createObjectTypeWizard();
//                        ResourceWizardPanel.this.replace(objectTypeWizard);
//                        target.add(objectTypeWizard);
//                        break;
//                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                Fragment fragment = createTableFragment();
                fragment.setOutputMarkupId(true);
                ResourceObjectTypeWizardPanel.this.replace(fragment);
                target.add(fragment);
            }
        };
        preview.setOutputMarkupId(true);
        fragment.add(preview);
        return fragment;
    }

    private IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> createModelOfResourceObjectType() {
        try {
            PrismContainerWrapper<ResourceObjectTypeDefinitionType> container = getResourceModel().getObjectWrapper().findContainer(
                    ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
            PrismContainerValue<ResourceObjectTypeDefinitionType> newItem = container.getItem().createNewValue();
            PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> newItemWrapper = WebPrismUtil.createNewValueWrapper(container, newItem, getPageBase());
            container.getValues().add(newItemWrapper);
            return () -> newItemWrapper;

        } catch (SchemaException e) {
            LOGGER.error("Cannot find assignment wrapper: {}", e.getMessage());
        }
        return null;
    }

    public ResourceDetailsModel getResourceModel() {
        return resourceModel;
    }

    protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
        return null;
    }
}
