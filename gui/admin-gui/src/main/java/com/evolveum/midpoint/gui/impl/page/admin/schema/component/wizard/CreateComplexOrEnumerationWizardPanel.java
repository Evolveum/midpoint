/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.complextype.BasicSettingComplexTypeStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.complextype.PrismItemDefinitionsTableWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.enumerationtype.BasicSettingEnumerationTypeStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.enumerationtype.EnumValueDefinitionsTableWizardPanel;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.EnumerationTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class CreateComplexOrEnumerationWizardPanel extends AbstractWizardPanel<SchemaType, AssignmentHolderDetailsModel<SchemaType>> {

    private static final Trace LOGGER = TraceManager.getTrace(CreateComplexOrEnumerationWizardPanel.class);

    private CreateComplexOrEnumerationChoicePanel.TypeEnum oldChoice;

    private IModel<PrismContainerValueWrapper> typeValueModel;

    public CreateComplexOrEnumerationWizardPanel(String id, WizardPanelHelper<SchemaType, AssignmentHolderDetailsModel<SchemaType>> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createChoicePanel()));
    }

    private void initValueModel(CreateComplexOrEnumerationChoicePanel.TypeEnum currentType, ItemPath path) {
        if (currentType == oldChoice) {
            return;
        }

        if (typeValueModel != null) {
            try {
                typeValueModel.getObject().getParent().remove(typeValueModel.getObject(), getPageBase());
            } catch (SchemaException e) {
                LOGGER.error("Couldn't remove value for definition type container.");
            }
            typeValueModel.detach();
        }

        typeValueModel = new LoadableModel<>(false) {
            @Override
            protected PrismContainerValueWrapper load() {
                IModel<PrismContainerValueWrapper<SchemaType>> schema = getValueModel();
                PrismContainerWrapper typeContainer;
                try {
                    typeContainer = schema.getObject().findContainer(path);

                    PrismContainerValue newValue = ((PrismContainer)typeContainer.getItem()).createNewValue();
                    PrismContainerValueWrapper valueWrapper = WebPrismUtil.createNewValueWrapper(
                            typeContainer, newValue, getPageBase(), getAssignmentHolderModel().createWrapperContext());
                    typeContainer.getValues().add(valueWrapper);
                    return valueWrapper;
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't create value for definition type container.");
                    return null;
                }
            }
        };

        oldChoice = currentType;
    }

    private Component createChoicePanel() {
        return new CreateComplexOrEnumerationChoicePanel(getIdOfChoicePanel(), getAssignmentHolderModel()) {

            @Override
            protected void onTileClickPerformed(TypeEnum value, AjaxRequestTarget target) {
                switch (value) {
                    case COMPLEX_TYPE -> {
                        initValueModel(TypeEnum.COMPLEX_TYPE, PrismSchemaType.F_COMPLEX_TYPE);
                        showWizardFragment(target, new WizardPanel(
                                getIdOfWizardPanel(), new WizardModel(createComplexTypeBasicSteps())));
                    }
                    case ENUMERATION_TYPE -> {
                        initValueModel(TypeEnum.ENUMERATION_TYPE, PrismSchemaType.F_ENUMERATION_TYPE);
                        showWizardFragment(target, new WizardPanel(
                                getIdOfWizardPanel(), new WizardModel(createEnumerationTypeBasicSteps())));
                    }
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                getHelper().onExitPerformed(target);
            }
        };
    }

    protected List<WizardStep> createEnumerationTypeBasicSteps() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new BasicSettingEnumerationTypeStepPanel(getAssignmentHolderModel(), getEnumTypeValueModel()) {

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

        steps.add(new EnumValueDefinitionsTableWizardPanel(getAssignmentHolderModel(), getEnumTypeValueModel()) {
            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                super.onSubmitPerformed(target);
                OperationResult result = CreateComplexOrEnumerationWizardPanel.this.onSavePerformed(target);
                if (result != null && !result.isError()) {
                    onExitPerformed(target);
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                CreateComplexOrEnumerationWizardPanel.this.onExitPerformed(target);
            }
        });

        return steps;
    }

    private IModel<PrismContainerValueWrapper<EnumerationTypeDefinitionType>> getEnumTypeValueModel() {
        return () -> typeValueModel.getObject();
    }

    protected List<WizardStep> createComplexTypeBasicSteps() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new BasicSettingComplexTypeStepPanel(getAssignmentHolderModel(), getComplexTypeValueModel()) {

            @Override
            public boolean onBackPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createChoicePanel());
                return false;
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

        steps.add(new PrismItemDefinitionsTableWizardPanel(getAssignmentHolderModel(), getComplexTypeValueModel()) {
            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                super.onSubmitPerformed(target);
                OperationResult result = CreateComplexOrEnumerationWizardPanel.this.onSavePerformed(target);
                if (result != null && !result.isError()) {
                    onExitPerformed(target);
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                CreateComplexOrEnumerationWizardPanel.this.onExitPerformed(target);
            }
        });

        return steps;
    }

    private IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> getComplexTypeValueModel() {
        return () -> typeValueModel.getObject();
    }
}
