/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.util.AssociationChildWrapperUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author lskublik
 */

@Experimental
public class ResourceAssociationTypeBasicWizardPanel extends AbstractWizardPanel<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceAssociationTypeBasicWizardPanel.class);

    boolean showChoicePanel = true;

    public ResourceAssociationTypeBasicWizardPanel(String id, WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        if (showChoicePanel) {
            add(createChoiceFragment(createAssociationChoicePanel()));
        } else {
            add(createWizardFragment(
                    new WizardPanel(
                            getIdOfWizardPanel(),
                            new WizardModel(
                                    createBasicStepsForModify()))));
        }
    }

    public void setShowChoicePanel(boolean showChoicePanel) {
        this.showChoicePanel = showChoicePanel;
    }

    private AssociationChoicePanel createAssociationChoicePanel() {
        return new AssociationChoicePanel(getIdOfChoicePanel(), getAssignmentHolderModel()) {
            @Override
            protected void onTileClickPerformed(AssociationDefinitionWrapper value, AjaxRequestTarget target) {
                performAssociationDef(value, target);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                ResourceAssociationTypeBasicWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected @NotNull IModel<String> getBreadcrumbLabel() {
                if (showChoicePanel) {
                    return Model.of();
                }
                return super.getBreadcrumbLabel();
            }
        };
    }

    private void performAssociationDef(AssociationDefinitionWrapper value, AjaxRequestTarget target) {
        try {
            PrismContainerWrapper<Containerable> subjectContainer =
                    getValueModel().getObject().findContainer(ShadowAssociationTypeDefinitionType.F_SUBJECT);
            cleanParticipantContainer(subjectContainer);

            PrismContainerWrapper<Containerable> objectContainer =
                    getValueModel().getObject().findContainer(ShadowAssociationTypeDefinitionType.F_OBJECT);
            cleanParticipantContainer(objectContainer);

            if (value.getSubject().getKind() != null) {
                PrismContainerWrapper<ResourceObjectTypeIdentificationType> objectTypeOfSubjectContainer =
                        getValueModel().getObject().findContainer(
                                ItemPath.create(
                                        ShadowAssociationTypeDefinitionType.F_SUBJECT,
                                        ShadowAssociationTypeSubjectDefinitionType.F_OBJECT_TYPE));

                addNewValue(objectTypeOfSubjectContainer, value.getSubject());
            }

            PrismPropertyWrapper<ItemPathType> sourceAttributeRef =
                    getValueModel().getObject().findProperty(
                            ItemPath.create(
                                    ShadowAssociationTypeDefinitionType.F_SUBJECT,
                                    ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                                    ShadowAssociationDefinitionType.F_SOURCE_ATTRIBUTE_REF));
            sourceAttributeRef.getValue().setRealValue(new ItemPathType(ItemPath.create(value.getAssociationAttribute())));

            String origLocalPart = value.getAssociationAttribute().getLocalPart();
            QName refQName = value.getAssociationAttribute();
            int index = 1;
            while (AssociationChildWrapperUtil.existAssociationConfiguration(
                    refQName.getLocalPart(),
                    getValueModel().getObject().getParent())) {

                refQName = new QName(refQName.getNamespaceURI(), origLocalPart + index, refQName.getPrefix());
                index++;

            }

            PrismPropertyWrapper<ItemPathType> refAttribute =
                    getValueModel().getObject().findProperty(
                            ItemPath.create(
                                    ShadowAssociationTypeDefinitionType.F_SUBJECT,
                                    ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                                    ShadowAssociationDefinitionType.F_REF));
            refAttribute.getValue().setRealValue(new ItemPathType(ItemPath.create(refQName)));

            if (value.getObjects().size() == 1 && value.getObjects().get(0).getKind() != null) {
                PrismContainerValueWrapper<Containerable> objectContainerValue = objectContainer.getValues().get(0);
                PrismContainerWrapper<ResourceObjectTypeIdentificationType> objectTypeOfObjectContainer =
                        objectContainerValue.findContainer(ShadowAssociationTypeObjectDefinitionType.F_OBJECT_TYPE);

                addNewValue(objectTypeOfObjectContainer, value.getObjects().get(0));
            }

            showWizardFragment(target,
                    new WizardPanel(
                            getIdOfWizardPanel(),
                            new WizardModel(createBasicStepsForCreate())));

        } catch (SchemaException e) {
            LOGGER.error("Couldn't save association configuration.", e);
        }
    }

    private void cleanParticipantContainer(PrismContainerWrapper<Containerable> container) throws SchemaException {
        container.removeAll(getPageBase());
        if (container.getValues().isEmpty()) {
            PrismContainerValue<Containerable> newValue = container.getItem().createNewValue();
            PrismContainerValueWrapper<Containerable> valueWrapper = WebPrismUtil.createNewValueWrapper(
                    container, newValue, getPageBase(), getAssignmentHolderModel().createWrapperContext());
            container.getValues().add(valueWrapper);
        }
    }

    private void addNewValue(
            PrismContainerWrapper<ResourceObjectTypeIdentificationType> container,
            AssociationDefinitionWrapper.ParticipantWrapper participant) throws SchemaException {
        PrismContainerValue<ResourceObjectTypeIdentificationType> newValue = container.getItem().createNewValue();
        newValue.asContainerable().kind(participant.getKind()).intent(participant.getIntent());
        PrismContainerValueWrapper<ResourceObjectTypeIdentificationType> valueWrapper = WebPrismUtil.createNewValueWrapper(
                container, newValue, getPageBase(), getAssignmentHolderModel().createWrapperContext());
        container.getValues().add(valueWrapper);
    }

    private List<WizardStep> createBasicStepsForCreate() {
        List<WizardStep> steps = new ArrayList<>();

        steps.add(new BasicSettingResourceAssociationTypeStepPanel(getAssignmentHolderModel(), getValueModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceAssociationTypeBasicWizardPanel.this.onExitPerformed(target);
            }

            @Override
            public boolean onBackPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createAssociationChoicePanel());
                return false;
            }

            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return new VisibleBehaviour(() -> showChoicePanel);
            }
        });

        steps.add(new SubjectAssociationStepPanel(getAssignmentHolderModel(), getValueModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceAssociationTypeBasicWizardPanel.this.onExitPerformed(target);
            }
        });

        steps.add(new ObjectAssociationStepPanel(getAssignmentHolderModel(), getValueModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceAssociationTypeBasicWizardPanel.this.onExitPerformed(target);
            }
        });

        steps.add(new AssociationDataAssociationTypeStepPanel(getAssignmentHolderModel(), getValueModel()) {

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                OperationResult result = ResourceAssociationTypeBasicWizardPanel.this.onSavePerformed(target);
                if (result == null || result.isError()) {
                    target.add(getFeedback());
                    refresh(target);
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceAssociationTypeBasicWizardPanel.this.onExitPerformed(target);
            }
        });

        return steps;
    }

    private List<WizardStep> createBasicStepsForModify() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new BasicSettingResourceAssociationTypeStepPanel(getAssignmentHolderModel(), getValueModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceAssociationTypeBasicWizardPanel.this.onExitPerformed(target);
            }

            @Override
            public boolean onBackPerformed(AjaxRequestTarget target) {
                onExitPerformed(target);
                return false;
            }

            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return new VisibleBehaviour(() -> showChoicePanel);
            }
        });

        steps.add(new AssociationDataAssociationTypeStepPanel(getAssignmentHolderModel(), getValueModel()) {

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                OperationResult result = ResourceAssociationTypeBasicWizardPanel.this.onSavePerformed(target);
                if (result == null || result.isError()) {
                    target.add(getFeedback());
                    refresh(target);
                } else {
                    onExitPerformed(target);
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceAssociationTypeBasicWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected ItemVisibilityHandler getVisibilityHandler() {
                return wrapper -> {
                    if (wrapper.getItemName().equals(ShadowAssociationDefinitionType.F_SOURCE_ATTRIBUTE_REF)
                            || wrapper.getItemName().equals(ShadowAssociationDefinitionType.F_REF)) {
                        return ItemVisibility.HIDDEN;
                    }
                    return ItemVisibility.AUTO;
                };
            }
        });
        return steps;
    }
}
