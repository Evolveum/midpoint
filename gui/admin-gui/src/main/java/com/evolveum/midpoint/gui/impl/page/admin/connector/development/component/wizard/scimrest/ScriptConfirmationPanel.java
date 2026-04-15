/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevArtifactType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.List;

public abstract class ScriptConfirmationPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private final IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel;

    public ScriptConfirmationPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel) {
        super(helper);
        this.valueModel = valueModel;
    }

    protected final IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> getValueModel() {
        return valueModel;
    }

    @Override
    public boolean isCompleted() {
        return getScriptClassifications().stream()
                .allMatch(
                        classification -> ConnectorDevelopmentWizardUtil.isScriptConfirmed(
                                getDetailsModel(), classification, valueModel.getObject().getRealValue().getName()));
    }

    protected abstract List<ConnectorDevelopmentArtifacts.KnownArtifactType> getScriptClassifications();

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        getScriptClassifications().forEach(
                classification -> {
                    try {
                        PrismPropertyWrapper<Boolean> confirmProperty = valueModel.getObject().findProperty(
                                ItemPath.create(classification.itemName, ConnDevArtifactType.F_CONFIRM));

                        PrismPropertyValueWrapper<Boolean> confirmValue = confirmProperty.getValue();

                        confirmValue.setRealValue(Boolean.TRUE);
                    } catch (SchemaException e) {
                        throw new RuntimeException(e);
                    }
                });

        OperationResult result = getHelper().onSaveObjectPerformed(target);
        getDetailsModel().getConnectorDevelopmentOperation();
        if (result != null && !result.isError()) {
            super.onNextPerformed(target);
        } else {
            target.add(getFeedback());
        }
        return false;
    }
}
