/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.basic;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.complextype.BasicSettingComplexTypeStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.complextype.PrismItemDefinitionsTableWizardPanel;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.prism.PrismObject;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public class SchemaWizardPanel extends AbstractWizardPanel<SchemaType, AssignmentHolderDetailsModel<SchemaType>> {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaWizardPanel.class);

    private LoadableModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> complexTypeValueModel;

    public SchemaWizardPanel(String id, WizardPanelHelper<SchemaType, AssignmentHolderDetailsModel<SchemaType>> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        initComplexTypeValueModel();
        add(createChoiceFragment(createChoicePanel()));
    }

    private void initComplexTypeValueModel() {
        if (complexTypeValueModel == null) {
            complexTypeValueModel = new LoadableModel<>(false) {
                @Override
                protected PrismContainerValueWrapper<ComplexTypeDefinitionType> load() {
                    IModel<PrismContainerValueWrapper<SchemaType>> schema = getValueModel();
                    PrismContainerWrapper<ComplexTypeDefinitionType> complexTypeContainer;
                    try {
                        complexTypeContainer = schema.getObject().findContainer(
                                ItemPath.create(WebPrismUtil.PRISM_SCHEMA, PrismSchemaType.F_COMPLEX_TYPE));

                        PrismContainerValue<ComplexTypeDefinitionType> newValue = complexTypeContainer.getItem().createNewValue();
                        PrismContainerValueWrapper<ComplexTypeDefinitionType> valueWrapper = WebPrismUtil.createNewValueWrapper(
                                complexTypeContainer, newValue, getPageBase(), getAssignmentHolderModel().createWrapperContext());
                        complexTypeContainer.getValues().add(valueWrapper);
                        return valueWrapper;
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't create value for complex type container.");
                        return null;
                    }
                }
            };
        }
    }

    private Component createChoicePanel() {
        return new CreateSchemaChoicePanel(getIdOfChoicePanel(), getAssignmentHolderModel()) {
            @Override
            protected void onTileClickPerformed(SchemaEnumType value, AjaxRequestTarget target) {
                switch (value) {
                    case ADD_TO_EXISTING_SCHEMA -> showChoiceFragment(target, createTemplatePanel());
                    case NEW_SCHEMA_TYPE -> showWizardFragment(target, new WizardPanel(
                            getIdOfWizardPanel(), new WizardModel(createBasicSteps(false))));
                }
            }
        };
    }

    protected ChoseSchemaPanel createTemplatePanel() {
        return new ChoseSchemaPanel(getIdOfChoicePanel()) {

            @Override
            protected void onTemplateSelectionPerformed(PrismObject<SchemaType> existObject, AjaxRequestTarget target) {
                reloadObjectDetailsModel(existObject);
                showWizardFragment(target, new WizardPanel(
                        getIdOfWizardPanel(), new WizardModel(createBasicSteps(true))));
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createChoicePanel());
            }
        };
    }

    private void reloadObjectDetailsModel(PrismObject<SchemaType> newObject) {
        getAssignmentHolderModel().reset();
        getAssignmentHolderModel().reloadPrismObjectModel(newObject);
    }

    protected List<WizardStep> createBasicSteps(boolean existingSchema) {
        List<WizardStep> steps = new ArrayList<>();
        if (!existingSchema) {
            steps.add(new BasicInformationStepPanel(getAssignmentHolderModel()) {

                @Override
                public boolean onBackPerformed(AjaxRequestTarget target) {
                    showChoiceFragment(target, createChoicePanel());

                    return false;
                }

                @Override
                protected void onExitPerformed(AjaxRequestTarget target) {
                    getHelper().onExitPerformed(target);
                }
            });
        }

        steps.add(new BasicSettingComplexTypeStepPanel(getAssignmentHolderModel(), complexTypeValueModel) {

            @Override
            public boolean onBackPerformed(AjaxRequestTarget target) {
                if (existingSchema){
                    showChoiceFragment(target, createTemplatePanel());
                    return false;
                }

                return super.onBackPerformed(target);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                getHelper().onExitPerformed(target);
            }

            @Override
            protected ItemMandatoryHandler getMandatoryHandler() {
                return this::checkMandatory;
            }

            protected boolean checkMandatory(ItemWrapper itemWrapper) {
                if (itemWrapper.getItemName().equals(ComplexTypeDefinitionType.F_EXTENSION)) {
                    return true;
                }
                return itemWrapper.isMandatory();
            }
        });

        steps.add(new PrismItemDefinitionsTableWizardPanel(getAssignmentHolderModel(), complexTypeValueModel) {
            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                super.onSubmitPerformed(target);
                OperationResult result = SchemaWizardPanel.this.onSavePerformed(target);
                if (result != null && !result.isError()) {
                    onExitPerformed(target);
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                SchemaWizardPanel.this.onExitPerformed(target);
            }
        });

        return steps;
    }
}
