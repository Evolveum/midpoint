/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.log.CodeBlockPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Base step presenting the object created for a capability test, to be confirmed by the user before the test.
 */
public abstract class AbstractConfirmCreatedObjectConnectorStepPanel extends AbstractCapabilityTestStepPanel {

    private static final String CLASS_DOT = AbstractConfirmCreatedObjectConnectorStepPanel.class.getName() + ".";
    private static final String OP_FORGET_OBJECT = CLASS_DOT + "forgetObject";

    private static final String ID_CREATED_OBJECT_TEXT = "createdObjectText";
    private static final String ID_IDENTIFIER = "identifier";
    private static final String ID_COPY_IDENTIFIER = "copyIdentifier";

    public AbstractConfirmCreatedObjectConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel, flowState);
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> getFlowState().getTestedObjectSource() == TestObjectSource.NEW_OBJECT
                && getFlowState().getTestedObjectModel().getObject() != null;
    }

    @Override
    public boolean isCompleted() {
        return getFlowState().getTestedObjectModel().getObject() != null;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        getTextLabel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        getSubtextLabel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        getButtonContainer().add(AttributeModifier.replace("class",
                "d-flex flex-column-reverse align-items-center mt-4 gap-3 wizard-actions-strip col-12"));
        getCustomButtonsContainer().add(AttributeModifier.append("class", "d-flex justify-content-center col-sm-5"));
        getSubmit().add(AttributeModifier.replace("class", "btn btn-primary flex-fill"));

        add(createCreatedObjectTextLabel());
        add(createIdentifierLabel());
        add(createCopyIdentifierLink());
    }

    @Override
    protected void initCustomButtons(RepeatingView customButtons) {
        customButtons.add(createObjectNotCreatedButton(customButtons.newChildId()));
    }

    @Override
    protected String getSubmitIcon() {
        return "";
    }

    /**
     * Returns the object created for the test.
     *
     * @return the created object as it was read after its creation
     */
    protected final ShadowType getTestedObject() {
        return getFlowState().getTestedObjectModel().getObject().getObjectOld().asObjectable();
    }

    /**
     * Returns the type of the script step opened by the fix action.
     *
     * @return the panel type of the script step
     */
    protected abstract String getScriptPanelType();

    private Label createCreatedObjectTextLabel() {
        return new Label(ID_CREATED_OBJECT_TEXT, () -> getString("AbstractConfirmCreatedObjectConnectorStepPanel.text", getTestedOperationName()));
    }

    private Label createIdentifierLabel() {
        return new Label(ID_IDENTIFIER,
                () -> ConnectorDevelopmentWizardUtil.formatPrimaryIdentifiers(getTestedObject()));
    }

    private AjaxLink<Void> createCopyIdentifierLink() {
        return new AjaxLink<>(ID_COPY_IDENTIFIER) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                target.appendJavaScript(CodeBlockPanel.buildCopyScript(
                        ConnectorDevelopmentWizardUtil.formatPrimaryIdentifiers(getTestedObject())));
            }
        };
    }

    private AjaxButton createObjectNotCreatedButton(String id) {
        AjaxButton button = new AjaxButton(id, createStringResource("AbstractConfirmCreatedObjectConnectorStepPanel.objectNotCreated")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                objectNotCreatedPerformed(target);
            }
        };
        button.add(AttributeModifier.replace("class", "btn btn-outline-primary flex-fill"));
        return button;
    }

    private void objectNotCreatedPerformed(AjaxRequestTarget target) {
        changeExternalSystem(OP_FORGET_OBJECT, getScriptPanelType(),
                "AbstractConfirmCreatedObjectConnectorStepPanel.forgetObjectFailed", target,
                task -> {
                    getDetailsModel().getConnectorDevelopmentOperation()
                            .forgetTestObjects(Set.of(getTestedObject().getOid()), task, task.getResult());
                    getDetailsModel().reloadPrismObjectByOid();
                    return null;
                });
        if (isOperationFailed()) {
            return;
        }
        getFlowState().objectDiscarded();
        onBackPerformed(target);
    }
}
