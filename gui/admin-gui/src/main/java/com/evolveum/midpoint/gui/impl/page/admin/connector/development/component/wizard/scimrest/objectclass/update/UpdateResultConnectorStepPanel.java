/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.update;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AbstractSentValuesVerificationConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.TestObjectFlowState;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AttributeVerificationStatus;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.VerifiedAttributeDto;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Wizard step for verifying the modified object when testing the Update capability.
 */
@PanelType(name = "cdw-update-verification")
@PanelInstance(identifier = "cdw-update-verification",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.updateVerification", icon = "fa fa-wrench"),
        containerPath = "empty")
public class UpdateResultConnectorStepPanel extends AbstractSentValuesVerificationConnectorStepPanel<UpdatedAttributeDto> {

    private static final Trace LOGGER = TraceManager.getTrace(UpdateResultConnectorStepPanel.class);

    private static final String OP_REBUILD_TEST_OBJECT = UpdateResultConnectorStepPanel.class.getName() + ".rebuildTestObject";

    private static final String PANEL_TYPE = "cdw-update-verification";

    public UpdateResultConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel, flowState);
    }

    @Override
    public boolean onBackPerformed(AjaxRequestTarget target) {
        ShadowType retrievedObject = getRetrievedObject();
        if (retrievedObject != null) {
            Task task = getPageBase().createSimpleTask(OP_REBUILD_TEST_OBJECT);
            OperationResult result = task.getResult();
            try {
                getFlowState().getTestedObjectModel().setObject(ConnectorDevelopmentWizardUtil.createShadowWrapperForModification(
                        getPageBase(), retrievedObject.asPrismObject().clone(), task, result));
            } catch (SchemaException | RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't prepare the test object for another modification", e);
                result.recordFatalError(e);
                getPageBase().showResult(result);
                target.add(getFeedback());
                return false;
            }
        }
        return super.onBackPerformed(target);
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.updateVerification");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected List<ConnectorDevelopmentArtifacts.KnownArtifactType> getScriptClassifications() {
        return List.of(ConnectorDevelopmentArtifacts.KnownArtifactType.UPDATE);
    }

    @Override
    protected List<IColumn<UpdatedAttributeDto, String>> createValueColumns() {
        return List.of(
                new LambdaColumn<>(createStringResource("UpdateResultConnectorStepPanel.previousValue"),
                        UpdatedAttributeDto.F_PREVIOUS_VALUE, UpdatedAttributeDto::getPreviousValue),
                new LambdaColumn<>(createStringResource("UpdateResultConnectorStepPanel.modifiedValue"),
                        VerifiedAttributeDto.F_EXPECTED_VALUE, UpdatedAttributeDto::getExpectedValue),
                new LambdaColumn<>(createStringResource("AbstractSentValuesVerificationConnectorStepPanel.retrievedValue"),
                        VerifiedAttributeDto.F_VALUE, UpdatedAttributeDto::getValue));
    }

    @Override
    protected List<UpdatedAttributeDto> getAttributeRows() {
        ShadowWrapper wrapper = getFlowState().getTestedObjectModel().getObject();
        ShadowType retrievedObject = getRetrievedObject();
        if (wrapper == null || retrievedObject == null) {
            return List.of();
        }

        try {
            ShadowType previousObject = wrapper.getObjectOld().asObjectable();
            ShadowType modifiedObject = wrapper.getObjectApplyDelta().asObjectable();

            Set<QName> names = new LinkedHashSet<>(getSimpleAttributeNamesWithIdentifiersFirst(previousObject));
            names.addAll(getSimpleAttributeNamesWithIdentifiersFirst(modifiedObject));

            return names.stream()
                    .filter(name -> ConnectorDevelopmentWizardUtil.compareValues(ShadowUtil.getAttributeValues(previousObject, name),
                            ShadowUtil.getAttributeValues(modifiedObject, name)) == AttributeVerificationStatus.MISMATCH)
                    .map(name -> createRow(name, previousObject, modifiedObject, retrievedObject))
                    .toList();
        } catch (CommonException e) {
            throw new IllegalStateException("Couldn't compute the modified test object", e);
        }
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        if (getMismatchState().isAnyMarked()) {
            getFlowState().getTestedObjectModel().setObject(null);
        }
        super.onSubmitPerformed(target);
    }

    @Override
    protected String getSuccessMessageKey() {
        return "UpdateResultConnectorStepPanel.retrievedSuccessfully";
    }

    @Override
    protected String getScriptPanelType() {
        return UpdateScriptConnectorStepPanel.PANEL_TYPE;
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.updateVerification.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.updateVerification.subText");
    }

    private UpdatedAttributeDto createRow(QName name, ShadowType previousObject, ShadowType modifiedObject,
            ShadowType retrievedObject) {
        Collection<?> modifiedValues = ShadowUtil.getAttributeValues(modifiedObject, name);
        Collection<?> retrievedValues = ShadowUtil.getAttributeValues(retrievedObject, name);
        return new UpdatedAttributeDto(name.getLocalPart(),
                ConnectorDevelopmentWizardUtil.formatValues(ShadowUtil.getAttributeValues(previousObject, name)),
                ConnectorDevelopmentWizardUtil.formatValues(modifiedValues),
                ConnectorDevelopmentWizardUtil.formatValues(retrievedValues),
                ConnectorDevelopmentWizardUtil.compareValues(modifiedValues, retrievedValues));
    }
}
