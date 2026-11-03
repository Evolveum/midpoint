/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.create;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Wizard step for verifying the created object when testing the Create capability.
 */
@PanelType(name = "cdw-create-verification")
@PanelInstance(identifier = "cdw-create-verification",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.createVerification", icon = "fa fa-wrench"),
        containerPath = "empty")
public class CreateResultConnectorStepPanel extends AbstractSentValuesVerificationConnectorStepPanel<VerifiedAttributeDto> {

    private static final String PANEL_TYPE = "cdw-create-verification";

    public CreateResultConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel, flowState);
    }

    @Override
    protected List<ConnectorDevelopmentArtifacts.KnownArtifactType> getScriptClassifications() {
        return List.of(ConnectorDevelopmentArtifacts.KnownArtifactType.CREATE);
    }

    @Override
    protected List<IColumn<VerifiedAttributeDto, String>> createValueColumns() {
        return List.of(
                new LambdaColumn<>(createStringResource("CreateResultConnectorStepPanel.expectedValue"),
                        VerifiedAttributeDto.F_EXPECTED_VALUE, VerifiedAttributeDto::getExpectedValue),
                new LambdaColumn<>(createStringResource("AbstractSentValuesVerificationConnectorStepPanel.retrievedValue"),
                        VerifiedAttributeDto.F_VALUE, VerifiedAttributeDto::getValue));
    }

    @Override
    protected List<VerifiedAttributeDto> getAttributeRows() {
        ShadowWrapper testedObject = getFlowState().getTestedObjectModel().getObject();
        ShadowType retrievedObject = getRetrievedObject();
        if (testedObject == null || retrievedObject == null) {
            return List.of();
        }

        ShadowType createdObject;
        try {
            createdObject = testedObject.getObjectApplyDelta().asObjectable();
        } catch (CommonException e) {
            throw new IllegalStateException("Couldn't compute the created test object", e);
        }

        Set<QName> names = new LinkedHashSet<>(getSimpleAttributeNamesWithIdentifiersFirst(retrievedObject));
        ShadowUtil.getAttributesRaw(createdObject).stream()
                .filter(attribute -> attribute instanceof PrismProperty)
                .forEach(attribute -> names.add(attribute.getElementName()));

        return names.stream()
                .map(name -> createRow(name, ShadowUtil.getAttributeValues(createdObject, name),
                        ShadowUtil.getAttributeValues(retrievedObject, name)))
                .filter(row -> row.getStatus() != null)
                .toList();
    }

    private VerifiedAttributeDto createRow(QName name, Collection<?> expectedValues, Collection<?> retrievedValues) {
        AttributeVerificationStatus status = null;
        if (expectedValues.isEmpty() && !retrievedValues.isEmpty()) {
            status = AttributeVerificationStatus.GENERATED;
        } else if (!expectedValues.isEmpty() || !retrievedValues.isEmpty()) {
            status = ConnectorDevelopmentWizardUtil.compareValues(expectedValues, retrievedValues);
        }
        return new VerifiedAttributeDto(name.getLocalPart(),
                ConnectorDevelopmentWizardUtil.formatValues(expectedValues),
                ConnectorDevelopmentWizardUtil.formatValues(retrievedValues),
                status);
    }

    @Override
    protected String getSuccessMessageKey() {
        return "CreateResultConnectorStepPanel.retrievedSuccessfully";
    }

    @Override
    protected String getScriptPanelType() {
        return CreateScriptConnectorStepPanel.PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.createVerification");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.createVerification.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.createVerification.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }
}
